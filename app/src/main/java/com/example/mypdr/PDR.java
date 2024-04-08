package com.example.mypdr;

import android.content.*;
import android.hardware.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import android.location.*;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.text.*;
import java.util.*;

public class PDR extends AppCompatActivity implements SensorEventListener {
    float yAccLimit = 9.8f, yAccBias = 2f;
    int minNumberOfStep = 1;
    private ArrayList<Float> RTyAcc = new ArrayList<Float>();
    private ArrayList<Float> RTyAccPart = new ArrayList<Float>();
    private ArrayList<Float> RTyAccTime = new ArrayList<Float>();
    private ArrayList<Float> RTyAccForMax = new ArrayList<Float>();
    private ArrayList<Float> RTyAccForMin = new ArrayList<Float>();
    private float maxAcc, minAcc;
    private boolean hasMax = false;
    long secondToNow = 0;
    long timeGap = 25;
    double[] accData = new double[3];
    double[] gyrData = new double[3];
    double[] magData = new double[3];
    long startTime;
    TextView textStep, textDist;
    TextView t1, t2, t3, textTime;
    TextView nowAddress, lat, lon;
    PDRView p;
    int stepNumber = 0;
    double stepLength = 0.5;
    private Handler timehandler;
    private SimpleDateFormat dateFormat;
    Button startPDR, showMap, showData;
    static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    LocationManager locationManager;
    LocationListener locationListener;
    private double roll, pitch, heading, startHeading;
    private double mx, my;
    private double omegaD, PsiD;
    private double lastTime = 0;
    private double lastOD = 0;
    double totalDistance = 0;
    int iter = 0;
    boolean isRecording = false; // 用于记录数据采集状态
    SensorManager sensorManager;
    Sensor acc, gyr, mag;
    private File file;
    private FileWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);
        textStep = findViewById(R.id.stepNumber);
        textStep.setText("步数: ");
        showMap = findViewById(R.id.showMap);
        t1 = findViewById(R.id.attText1);
        t2 = findViewById(R.id.attText2);
        t3 = findViewById(R.id.attText3);
        t1.setText("横滚角: ");
        t2.setText("俯仰角: ");
        t3.setText("航向角: ");
        lat = findViewById(R.id.lat);
        lon = findViewById(R.id.lon);
        nowAddress = findViewById(R.id.location);
        lat.setText("纬度: ");
        lon.setText("纬度: ");
        nowAddress.setText("位置: ");
        textDist = findViewById(R.id.distance);
        textDist.setText("移动距离: ");
        textTime = findViewById(R.id.pdrTime);
        p = findViewById(R.id.PDRView);

        timehandler = new Handler();
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss EEEE", Locale.getDefault());
        // 定时更新系统时间
        timehandler.postDelayed(updateTimeTask, 1000);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        startPDR = findViewById(R.id.startPDR);
        showMap = findViewById(R.id.showMap);
        showData = findViewById(R.id.showData);
        startPDR.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                showData.setEnabled(true);
            } else {
                Toast.makeText(this, "请在界面上出现角度后开始走动", Toast.LENGTH_SHORT).show();
                showData.setEnabled(false);
                startPDR.setText("STOP");
                new Handler().postDelayed(this::startRecording, 2000); // 延迟3秒
            }
        });


        showMap.setOnClickListener(v -> {
            Intent intent = new Intent(PDR.this, Map.class);
            startActivity(intent);
        });
        showData.setOnClickListener(v -> {
            Intent intent = new Intent(PDR.this, PDRData.class);
            startActivity(intent);
        });
    }

    private void startRecording() {
        isRecording = true;
        startTime = System.currentTimeMillis();
        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyr, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);

        try {
            file = new File(Environment.getExternalStorageDirectory(), "PDRdata.txt");
            writer = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        isRecording = false;
        startPDR.setText("Start");
        sensorManager.unregisterListener(this);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (isRecording) {
            try {
                if (event.sensor == acc) {
                    accData[0] = event.values[0];
                    accData[1] = event.values[1];
                    accData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    String s = "acc," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", accData[0]);
                    s += String.format("%.6f,", accData[1]);
                    s += String.format("%.6f,", accData[2]);
                    writer.write(s + "\n");
                } else if (event.sensor == gyr) {
                    gyrData[0] = event.values[0];
                    gyrData[1] = event.values[1];
                    gyrData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    String s = "gyo," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", gyrData[0]);
                    s += String.format("%.6f,", gyrData[1]);
                    s += String.format("%.6f,", gyrData[2]);
                    writer.write(s + "\n");
                } else if (event.sensor == mag) {
                    magData[0] = event.values[0];
                    magData[1] = event.values[1];
                    magData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    String s = "mag," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", magData[0]);
                    s += String.format("%.6f,", magData[1]);
                    s += String.format("%.6f,", magData[2]);
                    writer.write(s + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //PDR算法
//            secondToNow += timeGap;
//            updateHeading(secondToNow);
//            if (realTimeGetStep2((float) Math.sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2]), secondToNow)) {
//                stepNumber += 1;
//                textStep.setText("步数: " + stepNumber);
//                float meter = (float) (0.4 * Math.pow((float) (maxAcc2 - minAcc2), 1f / 4f));
//                p.draw((float) heading, meter);
//                totalDistance += meter;
//                DecimalFormat df = new DecimalFormat("#0.00000");
//                textDist.setText("移动距离: " + df.format(totalDistance) + "m");
//            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    // 更新系统时间的任务
    private Runnable updateTimeTask = new Runnable() {
        @Override
        public void run() {
            updateTime();
            timehandler.postDelayed(this, 1000);
        }
    };

    // 更新 TextView 显示的系统时间
    private void updateTime() {
        Date currentTime = new Date();
        String timeString = dateFormat.format(currentTime);
        textTime.setText(timeString);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除更新时间的任务
        timehandler.removeCallbacks(updateTimeTask);
        // 在Activity销毁时停止位置监听器，释放资源
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    public void updateHeading(float time) {
        //航向角
        //方向右前上
        pitch = Math.atan2(accData[1], Math.sqrt(accData[0] * accData[0] + accData[2] * accData[2]));
        roll = Math.atan2(-accData[0], accData[2]);
        //初始航向角
        if (iter == 0) {
            mx = magData[1] * Math.cos(pitch) + magData[0] * Math.sin(roll) * Math.sin(pitch) + (-magData[2]) * Math.cos(roll) * Math.sin(pitch);
            my = magData[0] * Math.cos(roll) - (-magData[2]) * Math.sin(roll);
            PsiD = -Math.atan2(my, mx);
            startHeading = PsiD + 9.9 * Math.PI / 180;
            heading = startHeading;
            iter++;
        }

        omegaD = -Math.sin(pitch) * gyrData[1] + Math.sin(roll) * Math.cos(pitch) * gyrData[0] + Math.cos(roll) * Math.cos(pitch) * (-gyrData[2]);
        heading += omegaD * timeGap * 0.001;
        if (heading > Math.PI)
            heading -= Math.PI * 2;
        if (heading < -Math.PI)
            heading += Math.PI * 2;
        DecimalFormat df = new DecimalFormat("#0.0000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI));
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI));
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI));
    }

    public static float lastAcc = 99999f;
    public static float disOfTopAndBottom = 4.5f;
    private float lastTime2 = -9999;
    public static float minTimeDis = 0.4f;
    public boolean inMin = true;
    private boolean inMax = false;
    private float maxAcc2 = -99999;
    private float minAcc2 = 99999;

    public boolean realTimeGetStep2(float yAcc, float time) {
        boolean boolToReturn = false;
        if ((yAcc - lastAcc >= disOfTopAndBottom) && inMin) {
            inMin = false;
            inMax = true;
            maxAcc2 = -99999;
        }
        if ((yAcc - lastAcc <= -disOfTopAndBottom) && inMax) {
            inMin = true;
            inMax = false;
            minAcc2 = 99999;
            if (time - lastTime2 >= minTimeDis * 1000) {
                lastTime2 = time;
                boolToReturn = true;
            }
        }
        if (inMax) {
            if (yAcc > maxAcc2) {
                maxAcc2 = yAcc;
                lastAcc = maxAcc2;
            }
        }
        if (inMin) {
            if (yAcc < minAcc2) {
                minAcc2 = yAcc;
                lastAcc = minAcc2;
            }
        }
        return boolToReturn;
    }

    private float lastMax = 0;

    public boolean realTimeGetStep(float yAcc, float time) {
        RTyAcc.add(yAcc);
        if (yAcc > yAccLimit + yAccBias) {
            RTyAccForMax.add(yAcc);
            if (RTyAccForMin.size() <= minNumberOfStep) {
                RTyAccForMin.clear();
                return false;
            } else if (hasMax) {
                int index = 0;
                float minAcc1 = 99999;
                for (int i2 = 0; i2 < RTyAccForMin.size(); i2++) {
                    if (RTyAccForMin.get(i2) > minAcc1) {
                        minAcc1 = RTyAccForMin.get(i2);
                        index = i2;
                    }
                }
                minAcc = minAcc1;
                hasMax = false;
                RTyAccTime.add(time);
                stepLength = 0.4 * Math.pow(maxAcc - minAcc, 1f / 4f);
                return true;
            }
        } else if (yAcc < yAccLimit - yAccBias) {
            RTyAccForMin.add(yAcc);
            if (RTyAccForMax.size() <= minNumberOfStep) {
                RTyAccForMax.clear();
                return false;
            }
            int index = 0;
            float maxAcc1 = -99999;
            for (int i2 = 0; i2 < RTyAccForMax.size(); i2++) {
                if (RTyAccForMax.get(i2) > maxAcc) {
                    maxAcc1 = RTyAccForMax.get(i2);
                    index = i2;
                }
            }
            if (lastMax == 0)
                lastMax = maxAcc1;
            else yAccLimit += lastMax - maxAcc1;
            maxAcc = maxAcc1;
            hasMax = true;
        }
        return false;
    }

    public ArrayList<Integer> getStep(ArrayList<Float> yAcc) {
        ArrayList<Integer> step = new ArrayList<Integer>();
        ArrayList<Float> accArray = new ArrayList<Float>();
        for (int i = 0; i < yAcc.size(); i++) {
            if (yAcc.get(i) > yAccLimit) {
                accArray.add(yAcc.get(i));
                continue;
            }
            if (yAcc.get(i) < yAccLimit) {
                if (accArray.size() <= 3) {
                    accArray.clear();
                    continue;
                }

                int index = 0;
                float maxAcc = -99999;
                for (int i2 = 0; i2 < accArray.size(); i2++) {
                    if (accArray.get(i2) > maxAcc) {
                        maxAcc = accArray.get(i2);
                        index = i2;
                    }
                }
                step.add(index + i);
                accArray.clear();
            }
        }
        return step;
    }
}