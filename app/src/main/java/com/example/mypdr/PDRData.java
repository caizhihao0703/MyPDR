package com.example.mypdr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postprocess);

        dataView = findViewById(R.id.dataView);
        selectFileButton = findViewById(R.id.selectFileButton);
        selectFileButton.setOnClickListener(view -> openFilePicker());
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


                }
                PostPDR(stringBuilder);
                inputStream.close();
                reader.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataView.setText(stringBuilder.toString());
    }
    //配置初始参数
    double height=1.8;
    int timepause=200;
    private static double miu2G = 0.01;
    private double[] gyroffset=new double[3];
    private int windowSize=3;
    private double threshold = 13; // 设置阈值

    private void PostPDR(StringBuilder stringBuilder)
    {
        gyroffset[0]=-7.647624262847514e-04;
        gyroffset[1]=7.754136478517271e-04;
        gyroffset[2]=-4.748694187026118e-04;

        Map<String, double[][]> sensorData = parseSensorData(stringBuilder);//读取数据

        double[][] accData = sensorData.get("acc");
        double[][] gyoData = sensorData.get("gyo");
        double[][] magData = sensorData.get("mag");

        adjustGyoData(gyoData);//零偏补偿

        double[][][] interpolatedData = interpolateData(accData, gyoData, magData);//数据插值到陀螺时间

        double[][] newAccData = interpolatedData[0];
        double[][] newMagData = interpolatedData[1];

        int[] columns = {2, 3, 4};
        double[][] filteredGyoData = meanFilter(gyoData, windowSize,columns);//数据平滑

        double[][] heading = new double[accData.length][2];
        double[][] q = new double[accData.length][4];
        getStartHeading(newAccData, newMagData, heading, q, timepause);//航向初始对准
        updateHeading6(newAccData, filteredGyoData, q, heading, timepause);//AHRS计算航向角变化

        double[] aaccMagnitude = calculateAccMagnitude(accData);// 计算加速度幅值
        double[] accMagnitude = meanFilter(aaccMagnitude, windowSize);//数据平滑

        double[] timeSequence = extractTimeSequence(accData);
        int[] realStepIndices = footDetect(timeSequence, accMagnitude, threshold);//有效步数索引

        double[] Sf = new double[realStepIndices.length];
        double[][] footmeter = new double[realStepIndices.length][2];//利用步频及行人身高计算步长
        Sf[0] = 1;Sf[1] = 1;
        for (int j = 2; j < realStepIndices.length; j++) {
            double timeDiff1 = timeSequence[realStepIndices[j]];
            double timeDiff2 = timeSequence[realStepIndices[j - 1]];
            double timeDiff3 = timeSequence[realStepIndices[j - 2]];

            Sf[j] = 1 / (0.8 * (timeDiff1 - timeDiff2) + 0.2 * (timeDiff2 - timeDiff3));
        }
        for (int j = 0; j < realStepIndices.length; j++) {
            footmeter[j][0] = timeSequence[realStepIndices[j]];
            footmeter[j][1] = 0.7 + 0.371 * (height - 1.6) + 0.227 * (Sf[j] - 1.79) * height / 1.6;
        }
        double[][] newHeading = finalPDR(heading, footmeter);
        double[] x = new double[footmeter.length];
        double[] y = new double[footmeter.length];
        calculateCoordinates(newHeading, footmeter, x, y);

    }
    private static Map<String, double[][]> parseSensorData(StringBuilder data) {
        String[] lines = data.toString().split("\n");

        List<double[]> accDataList = new ArrayList<>();
        List<double[]> gyoDataList = new ArrayList<>();
        List<double[]> magDataList = new ArrayList<>();
        List<Double> existingTimes = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(",");
            double time = Double.parseDouble(parts[1]);
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double z = Double.parseDouble(parts[4]);
            String sensorType = parts[0];

            if (!existingTimes.contains(time)) {
                existingTimes.add(time);

                if ("acc".equals(sensorType)) {
                    accDataList.add(new double[]{time, x, y, z});
                } else if ("gyo".equals(sensorType)) {
                    gyoDataList.add(new double[]{time, x, y, z});
                } else if ("mag".equals(sensorType)) {
                    magDataList.add(new double[]{time, x * miu2G, y * miu2G, z * miu2G});
                }
            }
        }

        Map<String, double[][]> dataMap = new HashMap<>();
        dataMap.put("acc", accDataList.toArray(new double[0][]));
        dataMap.put("gyo", gyoDataList.toArray(new double[0][]));
        dataMap.put("mag", magDataList.toArray(new double[0][]));

        return dataMap;
    }
    private void adjustGyoData(double[][] gyoData) {
        for (int i = 1; i < gyoData[0].length; i++) {
            for (int j = 0; j < gyoData.length; j++) {
                gyoData[j][i] -= gyroffset[i-1];
            }
        }
    }
    private static double[][][] interpolateData(double[][] accData, double[][] gyoData, double[][] magData) {
        double[][] newAccData = new double[gyoData.length][4];
        double[][] newMagData = new double[gyoData.length][4];

        // Copy time data from gyoData
        for (int i = 0; i < gyoData.length; i++) {
            newAccData[i][0] = gyoData[i][0];
            newMagData[i][0] = gyoData[i][0];
        }

        // Interpolate accData and magData if timestamps do not match
        for (int i = 1; i < 4; i++) {
            if (!Arrays.equals(accData[0], gyoData[0])) {
                newAccData[i] = interpolate(accData, gyoData, i);
            } else {
                newAccData[i] = Arrays.copyOfRange(accData[i], 1, accData[i].length);
            }

            if (!Arrays.equals(magData[0], gyoData[0])) {
                newMagData[i] = interpolate(magData, gyoData, i);
            } else {
                newMagData[i] = Arrays.copyOfRange(magData[i], 1, magData[i].length);
            }
        }

        return new double[][][]{newAccData, newMagData};
    }
    private static double[] interpolate(double[][] sourceData, double[][] targetData, int index) {
        double[] interpolatedValues = new double[targetData.length];

        LinearInterpolator interpolator = new LinearInterpolator();
        PolynomialSplineFunction splineFunction = interpolator.interpolate(sourceData[0], sourceData[index]);

        for (int i = 0; i < targetData.length; i++) {
            double targetTime = targetData[i][0];
            if (targetTime < sourceData[0][0]) {
                // Target time is earlier than source data time
                interpolatedValues[i] = sourceData[index][1]; // Use the first value in source data
            } else if (targetTime > sourceData[sourceData.length - 1][0]) {
                // Target time is later than the last source data time
                interpolatedValues[i] = sourceData[index][sourceData.length - 1];
            } else {
                interpolatedValues[i] = splineFunction.value(targetTime);
            }
        }

        return interpolatedValues;
    }
    private double[][] meanFilter(double[][] accData, int windowSize, int[] columns) {
        double[][] filteredData = new double[accData.length][accData[0].length];

        for (int i = 0; i < accData.length; i++) {
            for (int j = 0; j < accData[0].length; j++) {
                filteredData[i][j] = accData[i][j]; // 复制原始数据到滤波数据
            }
        }

        for (int columnIndex : columns) {
            for (int i = 0; i < accData.length; i++) {
                int start = Math.max(0, i - windowSize);
                int end = Math.min(accData.length - 1, i + windowSize);

                double sum = 0;
                int count = 0;
                for (int k = start; k <= end; k++) {
                    sum += accData[k][columnIndex];
                    count++;
                }

                filteredData[i][columnIndex] = sum / count; // 计算均值并赋值给滤波数据
            }
        }

        return filteredData;
    }
    private void getStartHeading(double[][] accData, double[][] magData, double[][] heading, double[][] q, int timepause) {
        double[] accMean = new double[3];
        double[] magMean = new double[3];

        for (int i = 1; i <= 3; i++) {
            accMean[i - 1] = mean(getColumnMean(accData, i, 0, timepause - 1));
            magMean[i - 1] = mean(getColumnMean(magData, i, 0, timepause - 1));
        }

        double pitch = Math.atan2(accMean[1], Math.sqrt(accMean[0] * accMean[0] + accMean[2] * accMean[2]));
        double roll = Math.atan2(-accMean[1], accMean[2]);

        double mx = magMean[1] * Math.cos(pitch) + magMean[0] * Math.sin(roll) * Math.sin(pitch) - magMean[2] * Math.cos(roll) * Math.sin(pitch);
        double my = magMean[0] * Math.cos(roll) - magMean[1] * Math.sin(roll);
        double psiD = -Math.atan2(my, mx);

        double startHeading = psiD - 5 * Math.PI / 180.0;

        double[] q0 = new double[4];
        q0[0] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        q0[1] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2)
                - Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2);
        q0[2] = Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2);
        q0[3] = Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                - Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);

        for (int i = 0; i < timepause; i++) {
            heading[i][1] = startHeading;
            System.arraycopy(q0, 0, q[i], 0, 4);
        }
    }
    private double mean(double[] arr) {
        double sum = 0;
        for (double num : arr) {
            sum += num;
        }
        return sum / arr.length;
    }
    private double[] getColumnMean(double[][] data, int column, int start, int end) {
        double[] columnData = new double[end - start + 1];
        for (int i = start; i <= end; i++) {
            columnData[i - start] = data[i][column];
        }
        return columnData;
    }
    private void updateHeading6(double[][] accData, double[][] gyoData, double[][] q, double[][] heading, int timepause) {
        double Ki = 0.01;
        double Kp = 1;
        double[] eInt = new double[3];

        for (int j = timepause; j < gyoData.length; j++) {
            double ax = accData[j][1];
            double ay = accData[j][0];
            double az = -accData[j][2];
            double gx = gyoData[j][1];
            double gy = gyoData[j][0];
            double gz = -gyoData[j][2];

            double q0 = q[j - 1][0];
            double q1 = q[j - 1][1];
            double q2 = q[j - 1][2];
            double q3 = q[j - 1][3];

            double q0q0 = q0 * q0;
            double q0q1 = q0 * q1;
            double q0q2 = q0 * q2;
            double q1q3 = q1 * q3;
            double q2q3 = q2 * q3;
            double q3q3 = q3 * q3;

            double timegap = gyoData[j][0] - gyoData[j - 1][0];

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

            eInt[0] += ex * timegap;
            eInt[1] += ey * timegap;
            eInt[2] += ez * timegap;

            gx = gx + Kp * ex + Ki * eInt[0];
            gy = gy + Kp * ey + Ki * eInt[1];
            gz = gz + Kp * ez + Ki * eInt[2];

            double pa = q1;
            double pb = q2;
            double pc = q3;
            q0 = q0 + (-q1 * gx - q2 * gy - q3 * gz) * (0.5 * timegap);
            q1 = pa + (q0 * gx + pb * gz - pc * gy) * (0.5 * timegap);
            q2 = pb + (q0 * gy - pa * gz + pc * gx) * (0.5 * timegap);
            q3 = pc + (q0 * gz + pa * gy - pb * gx) * (0.5 * timegap);

            double normQ = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
            q[j][0] = q0 / normQ;
            q[j][1] = q1 / normQ;
            q[j][2] = q2 / normQ;
            q[j][3] = q3 / normQ;

            double newHeading = Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3));
            if (newHeading > Math.PI) {
                newHeading -= 2 * Math.PI;
            } else if (newHeading < -Math.PI) {
                newHeading += 2 * Math.PI;
            }

            heading[j][1] = newHeading;
        }
    }
    private double[] calculateAccMagnitude(double[][] accData) {
        double[] accMagnitude = new double[accData.length];

        for (int i = 0; i < accData.length; i++) {
            double ax = accData[i][1];
            double ay = accData[i][2];
            double az = accData[i][3];

            double magnitude = Math.sqrt(ax * ax + ay * ay + az * az);
            accMagnitude[i] = magnitude;
        }

        return accMagnitude;
    }
    private double[] meanFilter(double[] data, int windowSize) {
        double[] filteredData = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            double sum = 0.0;
            int count = 0;

            for (int j = Math.max(0, i - windowSize + 1); j <= i; j++) {
                sum += data[j];
                count++;
            }

            filteredData[i] = sum / count;
        }

        return filteredData;
    }
    private static double[] extractTimeSequence(double[][] accData) {
        double[] timeSequence = new double[accData.length];

        for (int i = 0; i < accData.length; i++) {
            timeSequence[i] = accData[i][0]; // 提取acc_data的第一列作为时间序列
        }

        return timeSequence;
    }
    private static int[] footDetect(double[] timeSequence, double[] accMagnitude, double threshold) {
        List<Double> peakTimes = new ArrayList<>();
        List<Integer> peakIndices = new ArrayList<>();

        // 峰值探测
        for (int i = 2; i < accMagnitude.length; i++) {
            if (accMagnitude[i - 1] > accMagnitude[i] && accMagnitude[i - 1] > accMagnitude[i - 2] && accMagnitude[i - 1] > threshold) {
                peakTimes.add(timeSequence[i - 1]);
                peakIndices.add(i - 1);
            }
        }

        // 脚步判断
        List<Integer> realStepIndices = new ArrayList<>();
        for (int i = 2; i < peakTimes.size(); i++) {
            if (peakTimes.get(i) - peakTimes.get(i - 2) > 1) {
                realStepIndices.add(peakIndices.get(i));
            }
        }

        // 将List转换为int数组
        int[] realStepIndicesArray = realStepIndices.stream().mapToInt(Integer::intValue).toArray();

        return realStepIndicesArray;
    }
    public static double[][] finalPDR(double[][] heading, double[][] footmeter) {
        double[] uniqueIndices = new double[heading.length];
        for (int i = 0; i < heading.length; i++) {
            uniqueIndices[i] = heading[i][0];
        }

        heading = removeDuplicateRows(heading, uniqueIndices);

        double[][] newHeading = new double[footmeter.length][2];

        for (int i = 0; i < footmeter.length; i++) {
            newHeading[i][0] = footmeter[i][0];
        }

        LinearInterpolator interpolator = new LinearInterpolator();
        PolynomialSplineFunction splineFunction = interpolator.interpolate(heading[0], heading[1]);

        for (int i = 0; i < footmeter.length; i++) {
            if (footmeter[i][0] < heading[0][0]) {
                newHeading[i][1] = heading[0][1];
            } else if (footmeter[i][0] > heading[heading.length - 1][0]) {
                newHeading[i][1] = heading[heading.length - 1][1];
            } else {
                newHeading[i][1] = splineFunction.value(footmeter[i][0]);
            }
        }

        return newHeading;
    }
    public static void calculateCoordinates(double[][] newHeading, double[][] footmeter, double[] x, double[] y) {
        for (int i = 1; i < footmeter.length; i++) {
            x[i] = x[i - 1] + footmeter[i][1] * Math.cos(newHeading[i][1]);
            y[i] = y[i - 1] + footmeter[i][1] * Math.sin(newHeading[i][1]);
        }
    }
    public static double[][] removeDuplicateRows(double[][] array, double[] indices) {
        int uniqueCount = 1;
        for (int i = 1; i < array.length; i++) {
            if (array[i][0] != array[i - 1][0]) {
                uniqueCount++;
            }
        }

        double[][] result = new double[uniqueCount][2];
        result[0] = array[0];

        int currentIndex = 1;
        for (int i = 1; i < array.length; i++) {
            if (array[i][0] != array[i - 1][0]) {
                result[currentIndex] = array[i];
                currentIndex++;
            }
        }

        return result;
    }
}

