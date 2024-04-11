package com.example.mypdr;

import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import android.location.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;

public class PDR extends AppCompatActivity implements SensorEventListener {
    double[] accData = new double[3];
    double[] gyrData = new double[3];
    double[] magData = new double[3];
    boolean haveaccdata = false, havegyrdata = false, havemagdata = false, havestartheading = false;
    long startTime;
    double GYROtimeGap, lastGYROtime = 0;
    TextView textStep, textDist;
    TextView t1, t2, t3, textTime;
    TextView nowAddress, lat, lon;
    PDRView p;
    int stepNumber = 0;
    private Handler timehandler;
    private SimpleDateFormat dateFormat;
    Button startPDR, showMap, showData;
    LocationManager locationManager;
    LocationListener locationListener;
    private double roll, pitch, heading, startHeading;
    private double mx, my;
    private double PsiD;
    double totalDistance = 0;
    int iter = 0;
    boolean isRecording = false; // 用于记录数据采集状态
    SensorManager sensorManager;
    Sensor acc, gyr, mag;
    FileOutputStream outputStream;
    double[] Quat = new double[4];
    double q0, q1, q2, q3;
    double[] eInt = new double[3];
    double height = Setting.H;

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

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
        }

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
                startPDR.setText("Stop");
                startPDR.setEnabled(false);
                new Handler().postDelayed(this::startRecording, 2000); // 延迟2秒
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

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            lat.setText("纬度: " + String.format("%.4f", latitude) + "°");
            lon.setText("经度: " + String.format("%.4f", longitude) + "°");

            try {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses.size() > 0) {
                    String address = addresses.get(0).getAddressLine(0);
                    nowAddress.setText("位置: " + address);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private void startRecording() {
        isRecording = true;
        startPDR.setEnabled(true);

        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyr, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
        startTime = System.currentTimeMillis();
        try {
            File file = new File(getExternalFilesDir(null), "pdrData.txt");
            outputStream = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {
        isRecording = false;
        startPDR.setText("Start");
        sensorManager.unregisterListener(this);
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording) {
            try {
                if (event.sensor == acc) {
                    haveaccdata = true;
                    accData[0] = event.values[0];
                    accData[1] = event.values[1];
                    accData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    String s = "acc," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", accData[0]);
                    s += String.format("%.6f,", accData[1]);
                    s += String.format("%.6f,", accData[2]) + "\n";
                    outputStream.write(s.getBytes());
                } else if (event.sensor == gyr) {
                    havegyrdata = true;
                    gyrData[0] = event.values[0];
                    gyrData[1] = event.values[1];
                    gyrData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    GYROtimeGap = timeElapsed - lastGYROtime;
                    lastGYROtime = timeElapsed;
                    String s = "gyo," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", gyrData[0]);
                    s += String.format("%.6f,", gyrData[1]);
                    s += String.format("%.6f,", gyrData[2]) + "\n";
                    outputStream.write(s.getBytes());
                } else if (event.sensor == mag) {
                    havemagdata = true;
                    magData[0] = event.values[0];
                    magData[1] = event.values[1];
                    magData[2] = event.values[2];
                    double timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                    String s = "mag," + String.format("%.3f,", timeElapsed);
                    s += String.format("%.6f,", magData[0]);
                    s += String.format("%.6f,", magData[1]);
                    s += String.format("%.6f,", magData[2]) + "\n";
                    outputStream.write(s.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //PDR算法

            if (havemagdata && haveaccdata && iter == 0) {
                getStartHeading();
                iter++;
            } else if (havegyrdata && havestartheading) {
                updateHeading6(GYROtimeGap * 27 / 80.0);
                UpdatePosition();
            }
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

    public void getStartHeading() {
        havestartheading = true;
        pitch = Math.atan2(accData[1], Math.sqrt(accData[0] * accData[0] + accData[2] * accData[2]));
        roll = Math.atan2(-accData[0], accData[2]);
        //初始航向角
        mx = magData[1] * Math.cos(pitch) + magData[0] * Math.sin(roll) * Math.sin(pitch) + (-magData[2]) * Math.cos(roll) * Math.sin(pitch);
        my = magData[0] * Math.cos(roll) - (-magData[2]) * Math.sin(roll);
        PsiD = -Math.atan2(my, mx);
        startHeading = PsiD + 9.9 * Math.PI / 180.0;
        heading = startHeading;

        Quat[0] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        Quat[1] = Math.cos(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2)
                - Math.sin(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2);
        Quat[2] = Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2);
        Quat[3] = Math.sin(startHeading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                - Math.cos(startHeading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        q0 = Quat[0];
        q1 = Quat[1];
        q2 = Quat[2];
        q3 = Quat[3];
        DecimalFormat df = new DecimalFormat("#0.0000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI) + "°");
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI) + "°");
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");
    }

    public void updateHeading6(double timeGap) {
        double Ki = 0.001, Kp = 1;
        double ax = accData[0], ay = accData[1], az = accData[2];
        double gx = gyrData[0], gy = gyrData[1], gz = gyrData[2];
        double norm;
        double vx, vy, vz;
        double ex, ey, ez;
        double q0q0 = q0 * q0;
        double q0q1 = q0 * q1;
        double q0q2 = q0 * q2;
        double q1q3 = q1 * q3;
        double q2q3 = q2 * q3;
        double q3q3 = q3 * q3;

        // 加速度计归一化
        norm = Math.sqrt(ax * ax + ay * ay + az * az);
        //if (norm == 0) return;
        ax /= norm;
        ay /= norm;
        az /= norm;

        vx = q1q3 - q0q2;
        vy = q2q3 + q0q1;
        vz = q0q0 - 0.5 + q3q3;

        ex = (ay * vz - az * vy);
        ey = (az * vx - ax * vz);
        ez = (ax * vy - ay * vx);
        eInt[0] += Ki * ex * timeGap;
        eInt[1] += Ki * ey * timeGap;
        eInt[2] += Ki * ez * timeGap;

        gx += Kp * ex + eInt[0];
        gy += Kp * ey + eInt[1];
        gz += Kp * ez + eInt[2];

        double pa = q1;
        double pb = q2;
        double pc = q3;
        q0 = q0 + (-q1 * gx - q2 * gy - q3 * gz) * (0.5 * timeGap);
        q1 = pa + (q0 * gx + pb * gz - pc * gy) * (0.5 * timeGap);
        q2 = pb + (q0 * gy - pa * gz + pc * gx) * (0.5 * timeGap);
        q3 = pc + (q0 * gz + pa * gy - pb * gx) * (0.5 * timeGap);

        norm = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 /= norm;
        q1 /= norm;
        q2 /= norm;
        q3 /= norm;

        pitch = Math.atan2(2 * (q2 * q3 + q0 * q1), 1 - 2 * (q1 * q1 + q2 * q2));
        roll = Math.asin(2 * (q0 * q2 - q1 * q3));
        heading = -Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3)) + Math.PI * 2;
        if (heading > Math.PI)
            heading -= Math.PI * 2;
        else if (heading < -Math.PI)
            heading += Math.PI * 2;

        DecimalFormat df = new DecimalFormat("#0.000000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI) + "°");
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI) + "°");
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");
    }

    public void updateHeading9(double timeGap) {
        double Ki = 0.001, Kp = 1;
        double ax = accData[0], ay = accData[1], az = accData[2];
        double mx = magData[0], my = magData[1], mz = magData[2];
        double gx = gyrData[0], gy = gyrData[1], gz = gyrData[2];
        double norm;
        double vx, vy, vz;
        double wx, wy, wz;
        double ex, ey, ez;
        double hx, hy, hz;
        double bx, bz;
        double q0q0 = q0 * q0;
        double q0q1 = q0 * q1;
        double q0q2 = q0 * q2;
        double q0q3 = q0 * q3;
        double q1q1 = q1 * q1;
        double q1q2 = q1 * q2;
        double q1q3 = q1 * q3;
        double q2q2 = q2 * q2;
        double q2q3 = q2 * q3;
        double q3q3 = q3 * q3;

        // 加速度计归一化
        norm = Math.sqrt(ax * ax + ay * ay + az * az);
        ax /= norm;
        ay /= norm;
        az /= norm;
        // 磁力计归一化
        norm = Math.sqrt(mx * mx + my * my + mz * mz);
        mx /= norm;
        my /= norm;
        mz /= norm;

        vx = q1q3 - q0q2;
        vy = q2q3 + q0q1;
        vz = q0q0 - 0.5 + q3q3;

        hx = 2 * mx * (0.5 - q2q2 - q3q3) + 2 * my * (q1q2 - q0q3) + 2 * mz * (q1q3 + q0q2);
        hy = 2 * mx * (q1q2 + q0q3) + 2 * my * (0.5 - q1q1 - q3q3) + 2 * mz * (q2q3 - q0q1);
        hz = 2 * mx * (q1q3 - q0q2) + 2 * my * (q2q3 + q0q1) + 2f * mz * (0.5 - q1q1 - q2q2);
        bx = Math.sqrt((hx * hx) + (hy * hy));
        bz = hz;
        wx = bx * (0.5 - q2q2 - q3q3) + bz * (q1q3 - q0q2);
        wy = bx * (q1q2 - q0q3) + bz * (q0q1 + q2q3);
        wz = bx * (q0q2 + q1q3) + bz * (0.5 - q1q1 - q2q2);

        ex = (ay * vz - az * vy) + (my * wz - mz * wy);
        ey = (az * vx - ax * vz) + (mz * wx - mx * wz);
        ez = (ax * vy - ay * vx) + (mx * wy - my * wx);
        eInt[0] += Ki * ex * timeGap;      // accumulate integral error
        eInt[1] += Ki * ey * timeGap;
        eInt[2] += Ki * ez * timeGap;

        gx += Kp * ex + Ki * eInt[0];
        gy += Kp * ey + Ki * eInt[1];
        gz += Kp * ez + Ki * eInt[2];

        double pa = q1;
        double pb = q2;
        double pc = q3;
        q0 = q0 + (-q1 * gx - q2 * gy - q3 * gz) * (0.5 * timeGap);
        q1 = pa + (q0 * gx + pb * gz - pc * gy) * (0.5 * timeGap);
        q2 = pb + (q0 * gy - pa * gz + pc * gx) * (0.5 * timeGap);
        q3 = pc + (q0 * gz + pa * gy - pb * gx) * (0.5 * timeGap);

        norm = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 /= norm;
        q1 /= norm;
        q2 /= norm;
        q3 /= norm;

        pitch = Math.atan2(2 * (q2 * q3 + q0 * q1), 1 - 2 * (q1 * q1 + q2 * q2));
        roll = Math.asin(2 * (q0 * q2 - q1 * q3));
        heading = -Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3));


        DecimalFormat df = new DecimalFormat("#0.000000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI));
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI));
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI));
    }

    public void UpdatePosition() {
        if (DetectStep(Math.sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2]), GYROtimeGap, lastGYROtime)) {
            stepNumber += 1;

            double Sf = 0;
            float meter = (float) (0.7 * 0.371 * (height - 1.6) + 0.227 * (Sf - 1.79) * height / 1.6);
            p.draw((float) heading, meter);

            totalDistance += meter;
            DecimalFormat df = new DecimalFormat("#0.00000");
            textStep.setText("步数: " + stepNumber);
            textDist.setText("移动距离: " + df.format(totalDistance) + "m");
        }
    }

    public double lastAcc = 99999f;
    public double disOfTopAndBottom = 4.5;
    public double minTimeGap = 0.4;
    public boolean inMin = true;
    private boolean inMax = false;
    private double maxAcc2 = -99999;
    private double minAcc2 = 99999;

    public boolean DetectStep(double acc, double time, double lasttime) {
        boolean flag = false;
        if ((acc - lastAcc >= disOfTopAndBottom) && inMin) {
            inMin = false;
            inMax = true;
            maxAcc2 = -99999;
        }
        if ((acc - lastAcc <= -disOfTopAndBottom) && inMax) {
            inMin = true;
            inMax = false;
            minAcc2 = 99999;
            //探测到脚步
            if (time - lasttime >= minTimeGap * 1000) {
                flag = true;
            }
        }
        if (inMax) {
            if (acc > maxAcc2) {
                maxAcc2 = acc;
                lastAcc = maxAcc2;
            }
        }
        if (inMin) {
            if (acc < minAcc2) {
                minAcc2 = acc;
                lastAcc = minAcc2;
            }
        }
        return flag;
    }
}