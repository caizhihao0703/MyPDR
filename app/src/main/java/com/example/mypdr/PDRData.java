package com.example.mypdr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.*;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;


public class PDRData extends AppCompatActivity {

    private TextView dataView;
    private Button selectFileButton;
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private Button checkContent, checkTrack;
    private boolean isshowContent = false, isshowTrack = true;
    private double[] latlon = new double[2];
    private List<LatLng> points = new ArrayList<>();
    private double lat = PDR.startlat, lon = PDR.startlon;
    ArrayList<double[]> accData= new ArrayList<>();
    ArrayList<double[]> gyoData= new ArrayList<>();
    ArrayList<double[]> magData= new ArrayList<>();
    double[] lastReadTime = new double[3];
    ArrayList<double[]> newAccData=new ArrayList<>();
    ArrayList<double[]> newMagData=new ArrayList<>();
    ArrayList<double[]> Heading=new ArrayList<>();
    ArrayList<double[]> accMagnitude=new ArrayList<>();
    ArrayList<Integer> realStepIndices=new ArrayList<>();
    ArrayList<double[]> foot=new ArrayList<>();
    ArrayList<double[]> newHeading=new ArrayList<>();
    double[] q = new double[4];
    ArrayList<Double> x=new ArrayList<>();
    ArrayList<Double> y=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postprocess);

        dataView = findViewById(R.id.dataView);
        dataView.setEnabled(false);
        selectFileButton = findViewById(R.id.selectFileButton);
        selectFileButton.setOnClickListener(view -> openFilePicker());
        checkContent = findViewById(R.id.checkContent);
        checkTrack = findViewById(R.id.checkTrack);

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        // 设置地图中心点为30°N，114°E的经纬度位置
        LatLng sourceLatLng = new LatLng(lat, lon);
        CoordinateConverter converter = new CoordinateConverter()
                .from(CoordinateConverter.CoordType.GPS)
                .coord(sourceLatLng);
        LatLng convertedpoint = converter.convert();
        points.add(convertedpoint);

        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(convertedpoint));

        checkTrack.setOnClickListener(v -> {
            if (isshowContent) {
                dataView.setEnabled(false);
                isshowTrack = true;
            } else if (isshowTrack) {
                mMapView.setEnabled(false);
                isshowContent = true;
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                displayFileContent(selectedFileUri);
            }
        }
    }

    private void displayFileContent(Uri fileUri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                    //后处理
                    String[] parts = line.split(",");
                    double time = Double.parseDouble(parts[1]);
                    double x = Double.parseDouble(parts[2]);
                    double y = Double.parseDouble(parts[3]);
                    double z = Double.parseDouble(parts[4]);
                    String sensorType = parts[0];

                    if ("acc".equals(sensorType)) {
                        if (lastReadTime[0] <= time) {
                            accData.add(new double[]{time, x, y, z});
                            lastReadTime[0] = time;
                        }
                    } else if ("gyo".equals(sensorType)) {
                        if (lastReadTime[1] <= time) {
                            gyoData.add(new double[]{time, x, y, z});
                            lastReadTime[1] = time;
                        }
                    } else if ("mag".equals(sensorType)) {
                        if (lastReadTime[2] <= time) {
                            magData.add(new double[]{time, x * miu2G, y * miu2G, z * miu2G});
                            lastReadTime[2] = time;
                        }
                    }
                }
                inputStream.close();
                reader.close();
            }
            PostPDR(accData,gyoData,magData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataView.setText(stringBuilder.toString());
    }
    //配置初始参数
    double height = 1.8;
    int timepause = 200;
    private static double miu2G = 0.01;
    private double[] gyroffset = new double[3];
    private double threshold = 13.2; // 设置阈值

    private double Psi=-5;//设置磁偏角


    private void PostPDR(ArrayList<double[]> accData,ArrayList<double[]> gyoData,ArrayList<double[]> magData) {
        gyroffset[0] = -7.647624262847514e-04;
        gyroffset[1] = 7.754136478517271e-04;
        gyroffset[2] = -4.748694187026118e-04;

        adjustGyoData(gyoData);//零偏补偿

        interpolateData(accData, gyoData, magData);//数据插值到陀螺时间

        getStartHeading(newAccData,newMagData, Heading, q);//航向初始对准
        updateHeading6(newAccData, gyoData, q, Heading);//AHRS计算航向角变化

        calculateAccMagnitude(accData);// 计算加速度幅值

        footDetectAddFoot(accMagnitude, threshold);//有效步数索引并获取脚步

        finalPDR(Heading, foot);

        calculateCoordinates(newHeading, foot);
    }
    private void adjustGyoData(ArrayList<double[]> gyoData) {
        for (int i = 1; i < gyoData.get(0).length; i++) {
            for (double[] data : gyoData) {
                data[i] -= gyroffset[i - 1];
            }
        }
    }
    private void interpolateData(ArrayList<double[]> accData, ArrayList<double[]> gyoData, ArrayList<double[]> magData) {
        // Copy time data from gyoData
        int[] index={0,0};
        for (int i = 0; i < gyoData.size(); i++) {
            double[] accRow = new double[4];
            double[] magRow = new double[4];
            accRow[0] = gyoData.get(i)[0];
            magRow[0] = gyoData.get(i)[0];
            for (int column = 1; column < 4; column++) {
                accRow[column]=interpolate(accData, gyoData.get(i)[0], index,column,0);
                magRow[column]=interpolate(magData, gyoData.get(i)[0], index,column,1);
            }
            newAccData.add(accRow);
            newMagData.add(magRow);
        }
    }
    private static double interpolate(ArrayList<double[]> data, double timestamp, int[] index,int column,int flag) {
        int i = 0;
        int n = data.size();

        // 从上次找到的位置开始查找
        if (index[flag] >= 0 && index[flag] < n) {
            i = index[flag];
        }

        // 寻找大于等于timestamp的数据点
        while (i < n && data.get(i)[0] < timestamp) {
            i++;
        }

        if (i == n) {
            i = n - 1; // 如果超出数据范围，将位置索引设置为最后一个数据点的索引
        }

        // 更新位置索引
        index[flag] = i;

        if (i == 0) {
            return data.get(0)[column]; // 没有找到大于等于timestamp的数据，返回第一个数据点
        }
        double x0 = data.get(i - 1)[0];
        double x1 = data.get(i)[0];
        double y0 = data.get(i - 1)[column];
        double y1 = data.get(i)[column];

        return y0 + (y1 - y0) * ((timestamp - x0) / (x1 - x0));
    }
    private void getStartHeading(ArrayList<double[]> accData, ArrayList<double[]> magData, ArrayList<double[]> heading, double[] q) {
        double[] accMean = new double[3];
        double[] magMean = new double[3];

        for (int i = 1; i <= 3; i++) {
            accMean[i - 1] = getColumnMean(accData, i, 0, timepause - 1);
            magMean[i - 1] = getColumnMean(magData, i, 0, timepause - 1);
        }

        double pitch = Math.atan2(accMean[1], Math.sqrt(accMean[0] * accMean[0] + accMean[2] * accMean[2]));
        double roll = Math.atan2(-accMean[1], accMean[2]);

        double mx = magMean[1] * Math.cos(pitch) + magMean[0] * Math.sin(roll) * Math.sin(pitch) - magMean[2] * Math.cos(roll) * Math.sin(pitch);
        double my = magMean[0] * Math.cos(roll) - magMean[1] * Math.sin(roll);
        double psiD = -Math.atan2(my, mx);

        double startHeading = psiD +Psi * Math.PI / 180.0;

        q[0] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        q[1] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2)
                - Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2);
        q[2] = Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2);
        q[3] = Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                - Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);

        for (int i = 0; i < timepause; i++) {
            double[] head=new double[2];
            head[0]=gyoData.get(i)[0];
            head[1]=startHeading;
            Heading.add(head);
        }
    }
    private double getColumnMean(ArrayList<double[]> data, int column, int start, int end) {
        double sum=0;double num=end-start+1;
        for (int i = start; i <= end; i++) {
            sum += data.get(i)[column];
        }
        return sum /num;
    }
    private void updateHeading6(ArrayList<double[]> accData, ArrayList<double[]> gyoData, double[] q, ArrayList<double[]> heading) {
        double Ki = 0.01;
        double Kp = 1;
        double[] eInt = new double[3];

        for (int j = timepause; j < gyoData.size(); j++) {
            double ax = accData.get(j)[2];
            double ay = accData.get(j)[1];
            double az = -accData.get(j)[3];
            double gx = gyoData.get(j)[2];
            double gy = gyoData.get(j)[1];
            double gz = -gyoData.get(j)[3];

            double q0 = q[0];
            double q1 = q[1];
            double q2 = q[2];
            double q3 = q[3];

            double q0q0 = q0 * q0;
            double q0q1 = q0 * q1;
            double q0q2 = q0 * q2;
            double q1q3 = q1 * q3;
            double q2q3 = q2 * q3;
            double q3q3 = q3 * q3;

            double timeGap = gyoData.get(j)[0] - gyoData.get(j-1)[0];

            double norm = Math.sqrt(ax * ax + ay * ay + az * az);
            if (norm == 0) {
                continue;
            } else {
                ax = ax / norm;
                ay = ay / norm;
                az = az / norm;
            }

            double vx = -(q1q3 - q0q2);
            double vy = -(q2q3 + q0q1);
            double vz = -(q0q0 - 0.5 + q3q3);

            double ex = ay * vz - az * vy;
            double ey = az * vx - ax * vz;
            double ez = ax * vy - ay * vx;

            eInt[0] += ex * timeGap;
            eInt[1] += ey * timeGap;
            eInt[2] += ez * timeGap;

            gx = gx + Kp * ex + Ki * eInt[0];
            gy = gy + Kp * ey + Ki * eInt[1];
            gz = gz + Kp * ez + Ki * eInt[2];

            double pa = q1;
            double pb = q2;
            double pc = q3;
            q0 = q0 + (-q1 * gx - q2 * gy - q3 * gz) * (0.5 * timeGap);
            q1 = pa + (q0 * gx + pb * gz - pc * gy) * (0.5 * timeGap);
            q2 = pb + (q0 * gy - pa * gz + pc * gx) * (0.5 * timeGap);
            q3 = pc + (q0 * gz + pa * gy - pb * gx) * (0.5 * timeGap);

            double normQ = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
            q[0] = q0 / normQ;
            q[1] = q1 / normQ;
            q[2] = q2 / normQ;
            q[3] = q3 / normQ;

            double[] Head=new double[2];
            Head[1] = Math.atan2(2 * (q[1] * q[2] + q[0] * q[3]), 1 - 2 * (q[2] * q[2] + q[3] * q[3]));
            if (Head[1] > Math.PI) {
                Head[1] -= 2 * Math.PI;
            } else if (Head[1] < -Math.PI) {
                Head[1] += 2 * Math.PI;
            }
            Head[0]=gyoData.get(j)[0];
            Heading.add(Head);
        }
    }
    private void calculateAccMagnitude(ArrayList<double[]> accData) {
        for (int i = 0; i < accData.size(); i++) {
            double ax = accData.get(i)[1];
            double ay = accData.get(i)[2];
            double az = accData.get(i)[3];


            double[] magnitude=new double[2];
            magnitude[1]= Math.sqrt(ax * ax + ay * ay + az * az);
            magnitude[0] = accData.get(i)[0];
            accMagnitude.add(magnitude);
        }
    }
    private void footDetectAddFoot(ArrayList<double[]> accMagnitude, double threshold) {
        List<Double> peakTimes = new ArrayList<>();
        List<Integer> peakIndices = new ArrayList<>();

        // 峰值探测
        for (int i = 2; i < accMagnitude.size(); i++) {
            if (accMagnitude.get(i-1)[1] > accMagnitude.get(i)[1] && accMagnitude.get(i-1)[1] > accMagnitude.get(i-2)[1] && accMagnitude.get(i-1)[1] > threshold) {
                peakTimes.add(accMagnitude.get(i-1)[0]);
                peakIndices.add(i - 1);
            }
        }
        // 脚步判断
        for (int i = 2; i < peakTimes.size(); i++) {
            if (peakTimes.get(i) - peakTimes.get(i - 2) > 1&&peakTimes.get(i) - peakTimes.get(i - 1) > 0.4&&peakTimes.get(i-1) - peakTimes.get(i - 2) > 0.4) {
                realStepIndices.add(peakIndices.get(i));
            }
        }
        //添加脚步
        for (int j = 0; j < realStepIndices.size(); j++) {
            double[] footRow=new double[2];
            double sf=1;
            footRow[0]=accMagnitude.get(realStepIndices.get(j))[0];
            if(j>2)
            {
                double timeDiff1 = accMagnitude.get(realStepIndices.get(j))[0];
                double timeDiff2 = accMagnitude.get(realStepIndices.get(j-1))[0];
                double timeDiff3 = accMagnitude.get(realStepIndices.get(j-2))[0];
                sf=1 / (0.8 * (timeDiff1 - timeDiff2) + 0.2 * (timeDiff2 - timeDiff3));
            }
            footRow[1]= 0.7 + 0.371 * (height - 1.6) + 0.227 * (sf - 1.79) * height / 1.6;
            foot.add(footRow);
        }
    }
    public void finalPDR(ArrayList<double[]> heading, ArrayList<double[]> foot) {
        int index[]={0};

        // Interpolate Heading data
        for (int i = 0; i < foot.size(); i++) {
            double[] headingRow=new double[2];
            headingRow[0]=foot.get(i)[0];
            headingRow[1] = interpolate(heading, foot.get(i)[0],index,1,0);
            newHeading.add(headingRow);
        }
    }
    public void calculateCoordinates(ArrayList<double[]> newHeading, ArrayList<double[]> foot) {
        double[] xy = CordTrans.BL2xy(lat, lon);
        x.add(xy[0]);y.add(xy[1]);
        for (int i = 1; i <= foot.size(); i++) {
            double x0 = x.get(i-1)+ foot.get(i-1)[1] * Math.cos(newHeading.get(i-1)[1]);
            double y0 = y.get(i-1) + foot.get(i-1)[1] * Math.sin(newHeading.get(i-1)[1]);
            x.add(x0);y.add(y0);

            latlon = CordTrans.xytoBL(x0, y0, 114);
            LatLng sourceLatLng = new LatLng(latlon[0], latlon[1]);
            CoordinateConverter converter = new CoordinateConverter()
                    .from(CoordinateConverter.CoordType.GPS)
                    .coord(sourceLatLng);
            LatLng convertedpoint = converter.convert();
            points.add(convertedpoint);

            OverlayOptions mOverlayOptions = new PolylineOptions()
                    .width(10)
                    .color(0xAAFF0000)
                    .points(points);
            //在地图上绘制折线
            mBaiduMap.clear();
            mBaiduMap.addOverlay(mOverlayOptions);
        }

    }
}

