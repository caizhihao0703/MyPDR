package com.example.mypdr;

import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.*;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.location.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;

import java.io.*;
import java.text.*;
import java.util.*;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;

public class PDR extends AppCompatActivity implements SensorEventListener {
    private float[] accData = new float[3];
    private ArrayList<Double> accNormList = new ArrayList<>();
    private float[] gyrData = new float[3];
    private float[] lastgyrData = new float[3];
    private float[] magData = new float[3];
    private ArrayList<Double> stepTime = new ArrayList<>();
    private boolean haveaccdata = false;
    private boolean havemagdata = false;
    private boolean haveinithead = false;
    private long startTime;
    private double GYROtimeGap, lastGYROtime = 0, timeElapsed = 0;
    private TextView textStep, textDist, warning, t1, t2, t3, textTime, nowAddress, lat, lon;
    //    private PDRView p;
    private int stepNumber = 0;
    private Handler timehandler;
    private Button startPDR;
    private Button postprocess;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double roll;
    private double pitch;
    private double heading;
    private double totalDistance = 0;
    private SensorManager sensorManager;
    private Sensor acc, gyr, mag;
    private FileOutputStream outputStream;
    private double[] Quat = new double[4];
    private double q0, q1, q2, q3;
    private double[] eInt = new double[3];
    private double height = 1.8, dis = 0.7;
    private double latitude = 0, longitude = 0;
    private boolean isFirstPos = true;

    boolean isInitinghead = false, isRecording = false; //用于校准航向角// 用于记录数据采集状态

    private LinearLayout settingsPanel;
    boolean useAHRS6 = true, useAHRS9 = false, useKutta = false;
    SeekBar seekBar1;
    SeekBar seekBar2;
    TextView textView1;
    TextView textView2;
    boolean useLinearModel = true, useDynamicModel = false;
    boolean useDetectStop = false;
    private Handler handler;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);
        textStep = findViewById(R.id.stepNumber);
        textStep.setText("步数: ");
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
        lon.setText("经度: ");
        nowAddress.setText("位置: ");
        textDist = findViewById(R.id.distance);
        textDist.setText("移动距离: ");
        textTime = findViewById(R.id.pdrTime);
//        p = findViewById(R.id.PDRView);
        warning = findViewById(R.id.warning);
        settingsPanel = findViewById(R.id.settingsPanel);
        settingsPanel.setVisibility(View.GONE);
        RadioGroup radioGroup1 = findViewById(R.id.radioGroup1);
        RadioGroup radioGroup2 = findViewById(R.id.radioGroup2);
        RadioGroup radioGroup3 = findViewById(R.id.radioGroup3);
        radioGroup1.check(R.id.radioOption1_1);
        radioGroup2.check(R.id.radioOption2_1);
        radioGroup3.check(R.id.radioOption3_2);
        Button closeSetting = findViewById(R.id.closeSettings);
        Button openSetting = findViewById(R.id.openSetting);
        seekBar1 = findViewById(R.id.seekBar1);
        textView1 = findViewById(R.id.height);
        seekBar2 = findViewById(R.id.seekBar2);
        textView2 = findViewById(R.id.steplen);


        //时间位置
        timehandler = new Handler();
        // 定时更新系统时间
        timehandler.postDelayed(updateTimeTask, 1000);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
        }
        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        startPDR = findViewById(R.id.startPDR);
        Button showMap = findViewById(R.id.showMap);
        postprocess = findViewById(R.id.postprocess);
        startPDR.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                postprocess.setEnabled(true);
            } else {
                Toast.makeText(this, "请在界面上出现角度后开始走动", Toast.LENGTH_SHORT).show();
                startPDR.setText("结束");
                startRecording();
            }
        });
        showMap.setOnClickListener(v -> {
            Intent intent = new Intent(PDR.this, Map.class);
            startActivity(intent);
        });
        postprocess.setOnClickListener(v -> {
            Intent intent = new Intent(PDR.this, PDRData.class);
            startActivity(intent);
        });
        openSetting.setOnClickListener(v -> {
            Animation fadeIn = AnimationUtils.loadAnimation(PDR.this, R.anim.fade_in);
            settingsPanel.startAnimation(fadeIn);
            settingsPanel.setVisibility(View.VISIBLE);
        });
        closeSetting.setOnClickListener(v -> {
            Animation fadeOut = AnimationUtils.loadAnimation(PDR.this, R.anim.fade_out);
            settingsPanel.startAnimation(fadeOut);
            settingsPanel.setVisibility(View.GONE);

            RadioButton selectedRadioButton1_1 = findViewById(R.id.radioOption1_1);
            RadioButton selectedRadioButton1_2 = findViewById(R.id.radioOption1_2);
            RadioButton selectedRadioButton1_3 = findViewById(R.id.radioOption1_3);
            RadioButton selectedRadioButton2_1 = findViewById(R.id.radioOption2_1);
            RadioButton selectedRadioButton2_2 = findViewById(R.id.radioOption2_2);
            RadioButton selectedRadioButton3_1 = findViewById(R.id.radioOption3_1);
            RadioButton selectedRadioButton3_2 = findViewById(R.id.radioOption3_2);

            if (selectedRadioButton1_1.isChecked()) {
                useAHRS6 = true;
                useAHRS9 = false;
                useKutta = false;
            } else if (selectedRadioButton1_2.isChecked()) {
                useAHRS6 = false;
                useAHRS9 = true;
                useKutta = false;
            } else if (selectedRadioButton1_3.isChecked()) {
                useAHRS6 = false;
                useAHRS9 = false;
                useKutta = true;
            }
            if (selectedRadioButton2_1.isChecked()) {
                useLinearModel = true;
                useDynamicModel = false;
            } else if (selectedRadioButton2_2.isChecked()) {
                useLinearModel = false;
                useDynamicModel = true;
            }
            if (selectedRadioButton3_1.isChecked()) {
                useDetectStop = true;
            } else if (selectedRadioButton3_2.isChecked()) {
                useDetectStop = false;
            }
        });

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新TextView的数值
                progress += 140;
                height = progress / 100.0;
                textView1.setText("身高：" + progress + "cm");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Your code here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Your code here
            }
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新TextView的数值
                progress += 50;
                dis = progress / 100.0;
                textView2.setText("步长：" + progress + "cm");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Your code here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Your code here
            }
        });
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (isFirstPos) {
                isFirstPos = false;

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                lat.setText("纬度: " + String.format("%.8f", latitude) + "°");
                lon.setText("经度: " + String.format("%.8f", longitude) + "°");
                // 设置地图中心点为30°N，114°E的经纬度位置
                LatLng centerPoint = new LatLng(latitude, longitude);
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(centerPoint));
            }

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

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
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
        mMapView.onDestroy();
    }


    private void adjustHeading() {
        DecimalFormat df = new DecimalFormat("#0.0000");
        if (!haveinithead) {
            if (useDetectStop) {
                getOrientation();
                warning.setText("正在获得初始航向！");
            } else {
                pitch = Math.atan2(accData[1], Math.sqrt(accData[0] * accData[0] + accData[2] * accData[2]));
                roll = Math.atan2(-accData[0], accData[2]);
                //初始航向角
                double mx = magData[1] * Math.cos(pitch) + magData[0] * Math.sin(roll) * Math.sin(pitch) + (-magData[2]) * Math.cos(roll) * Math.sin(pitch);
                double my = magData[0] * Math.cos(roll) - (-magData[2]) * Math.sin(roll);
                double psiD = -Math.atan2(my, mx);
                heading = psiD + 9.9 * Math.PI / 180.0;
                warning.setText("正在获得初始航向！");
            }
        } else {
            if (useDetectStop) {
                getOrientation();
                warning.setText("检测到脚步中断！");
                t1.setText("横滚角: " + df.format(roll * 180 / Math.PI) + "°");
                t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI) + "°");
            } else {
                warning.setText("检测到脚步中断！");
            }
        }
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");

        Euler2Quat(heading, pitch, roll);
    }

    private void Euler2Quat(double heading, double pitch, double roll) {
        Quat[0] = Math.cos(heading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(heading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        Quat[1] = Math.cos(heading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2)
                - Math.sin(heading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2);
        Quat[2] = Math.cos(heading / 2) * Math.sin(pitch / 2) * Math.cos(roll / 2)
                + Math.sin(heading / 2) * Math.cos(pitch / 2) * Math.sin(roll / 2);
        Quat[3] = Math.sin(heading / 2) * Math.cos(pitch / 2) * Math.cos(roll / 2)
                - Math.cos(heading / 2) * Math.sin(pitch / 2) * Math.sin(roll / 2);
        q0 = Quat[0];
        q1 = Quat[1];
        q2 = Quat[2];
        q3 = Quat[3];
    }

    private void getOrientation() {
        pitch = Math.atan2(accData[1], Math.sqrt(accData[0] * accData[0] + accData[2] * accData[2]));
        roll = Math.atan2(-accData[0], accData[2]);
        float[] rotationMatrix = new float[9];
        float[] orientationAngles = new float[3];
        SensorManager.getRotationMatrix(rotationMatrix, null, accData, magData);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        heading = orientationAngles[0];
        if (heading > Math.PI)
            heading -= Math.PI * 2;
        else if (heading < -Math.PI)
            heading += Math.PI * 2;
    }

    private void startRecording() {
        isRecording = true;
        isInitinghead = true;
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    // 在这里执行需要延迟执行的命令
                    isInitinghead = false;
                    haveinithead = true;
                    warning.setText("");
                }
            }
        };

        // 开始计时，延迟两秒执行命令
        handler.sendEmptyMessageDelayed(1, 2000);

        //打开文件，文件名格式pdrData_年月日时分秒
        Date currentTime = new Date();
        SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String textName = filenameFormat.format(currentTime);
        //开始记录时间
        startTime = System.currentTimeMillis();
        try {
            String fileName = "pdrData_" + textName + ".txt";
            File file = new File(getExternalFilesDir(null), fileName);
            outputStream = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyr, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopRecording() {
        isRecording = false;
        startPDR.setText("开始");
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
            if (event.sensor == acc) {
                processAccSensorData(event);

                if (accNormList.size() == 3) accNormList.remove(0);//储存三个历元的加速度计数据
                accNormList.add(Math.sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2]));
                if (accNormList.size() == 3) {
                    if (DetectStep(accNormList)) {
                        warning.setText("");
                        isInitinghead = false;
                        UpdatePosition(stepTime);
                    }
                }
                if (!isInitinghead) {
                    //脚步中断
                    if (stepTime.size() > 0 && (timeElapsed - stepTime.get(stepTime.size() - 1)) > 2) {
                        isInitinghead = true;
                    }
                }
            } else if (event.sensor == gyr) {
                processGyrSensorData(event);
                if (!isInitinghead) {
                    if (useAHRS6) updateHeading6(GYROtimeGap);
                    else if (useAHRS9) updateHeading9(GYROtimeGap);
                    else if (useKutta) updateKutta(GYROtimeGap);
                } else if (!useDetectStop && haveinithead) {
                    updateHeading6(GYROtimeGap);
                }

            } else if (event.sensor == mag) {
                processMagSensorData(event);
            }
            //如果正在校准航向角
            if (haveaccdata && havemagdata && isInitinghead) {
                adjustHeading();//获得起始航向角
            }
        }
    }

    private void processAccSensorData(SensorEvent event) {
        haveaccdata = true;
        System.arraycopy(event.values, 0, accData, 0, event.values.length);
        try {
            writeSensorData("acc", accData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processGyrSensorData(SensorEvent event) {
        lastgyrData = gyrData;
        System.arraycopy(event.values, 0, gyrData, 0, event.values.length);
        try {
            writeSensorData("gyo", gyrData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        GYROtimeGap = timeElapsed - lastGYROtime;
        lastGYROtime = timeElapsed;
    }

    private void processMagSensorData(SensorEvent event) {
        havemagdata = true;
        System.arraycopy(event.values, 0, magData, 0, event.values.length);
        try {
            writeSensorData("mag", magData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSensorData(String sensorType, float[] data) throws IOException {
        timeElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        String s = sensorType + "," + String.format("%.3f,", timeElapsed);
        for (float value : data) {
            s += String.format("%.6f,", value);
        }
        s += "\n";
        outputStream.write(s.getBytes());
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss EEEE", Locale.getDefault());

        String timeString = dateFormat.format(currentTime);
        textTime.setText(timeString);
    }

    public void updateHeading6(double timeGap) {
        double Ki = 0.001, Kp = 1;
        double ax = accData[1], ay = accData[0], az = -accData[2];
        double gx = gyrData[1], gy = gyrData[0], gz = -gyrData[2];
        double norm;
        double vx, vy, vz;
        double ex, ey, ez;
        double q0q0 = q0 * q0;
        double q0q1 = q0 * q1;
        double q0q2 = q0 * q2;
        double q1q1 = q1 * q1;
        double q1q3 = q1 * q3;
        double q2q2 = q2 * q2;
        double q2q3 = q2 * q3;
        double q3q3 = q3 * q3;

        // 加速度计归一化
        norm = Math.sqrt(ax * ax + ay * ay + az * az);
        //if (norm == 0) return;
        ax /= norm;
        ay /= norm;
        az /= norm;

        vx = -2 * (q1q3 - q0q2);
        vy = -2 * (q2q3 + q0q1);
        vz = -(q0q0 - q1q1 - q2q2 + q3q3);

        ex = (ay * vz - az * vy);
        ey = (az * vx - ax * vz);
        ez = (ax * vy - ay * vx);
        eInt[0] += Ki * ex * timeGap;
        eInt[1] += Ki * ey * timeGap;
        eInt[2] += Ki * ez * timeGap;

        gx += (Kp * ex + eInt[0]);
        gy += (Kp * ey + eInt[1]);
        gz += (Kp * ez + eInt[2]);

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

        roll = Math.atan2(2 * (q2 * q3 + q0 * q1), 1 - 2 * (q1 * q1 + q2 * q2));
        pitch = Math.asin(2 * (q0 * q2 - q1 * q3));
        heading = Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3));

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
        double ax = accData[1], ay = accData[0], az = -accData[2];
        double mx = magData[1], my = magData[0], mz = -magData[2];
        double gx = gyrData[1], gy = gyrData[0], gz = -gyrData[2];
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

        vx = -2 * (q1q3 - q0q2);
        vy = -2 * (q2q3 + q0q1);
        vz = -(q0q0 - q1q1 - q2q2 + q3q3);

        hx = 2 * mx * (0.5 - q2q2 - q3q3) + 2 * my * (q1q2 - q0q3) + 2 * mz * (q1q3 + q0q2);
        hy = 2 * mx * (q1q2 + q0q3) + 2 * my * (0.5 - q1q1 - q3q3) + 2 * mz * (q2q3 - q0q1);
        hz = 2 * mx * (q1q3 - q0q2) + 2 * my * (q2q3 + q0q1) + 2 * mz * (0.5 - q1q1 - q2q2);
        bx = Math.sqrt((hx * hx) + (hy * hy));
        bz = hz;
        wx = 2 * bx * (0.5 - q2q2 - q3q3) + bz * (q1q3 - q0q2);
        wy = 2 * bx * (q1q2 - q0q3) + bz * (q0q1 + q2q3);
        wz = 2 * bx * (q0q2 + q1q3) + bz * (0.5 - q1q1 - q2q2);

        ex = (ay * vz - az * vy) + (my * wz - mz * wy);
        ey = (az * vx - ax * vz) + (mz * wx - mx * wz);
        ez = (ax * vy - ay * vx) + (mx * wy - my * wx);
        eInt[0] += Ki * ex * timeGap;
        eInt[1] += Ki * ey * timeGap;
        eInt[2] += Ki * ez * timeGap;

        gx += (Kp * ex + eInt[0]);
        gy += (Kp * ey + eInt[1]);
        gz += (Kp * ez + eInt[2]);

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

        roll = Math.atan2(2 * (q2 * q3 + q0 * q1), 1 - 2 * (q1 * q1 + q2 * q2));
        pitch = Math.asin(2 * (q0 * q2 - q1 * q3));
        heading = Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3));

        if (heading > Math.PI)
            heading -= Math.PI * 2;
        else if (heading < -Math.PI)
            heading += Math.PI * 2;

        DecimalFormat df = new DecimalFormat("#0.0000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI) + "°");
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI) + "°");
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");
    }

    public void updateKutta(double timeGap) {
        double wx = lastgyrData[1], wy = lastgyrData[0], wz = -lastgyrData[2];
        double dx = gyrData[1] - wx, dy = gyrData[0] - wy, dz = -gyrData[2] - wz;
        double[] k1 = new double[4];
        double[] k2 = new double[4];
        double[] k3 = new double[4];
        double[] k4 = new double[4];
        double[] qm = new double[4];
        k1[0] = (-wx * q1 - wy * q2 - wz * q3) / 2;
        k1[1] = (wx * q0 - wz * q2 - wy * q3) / 2;
        k1[2] = (wy * q0 - wz * q1 + wx * q3) / 2;
        k1[3] = (wz * q0 + wy * q1 - wx * q2) / 2;
        qm[0] = q0 + 0.5 * timeGap * k1[0];
        qm[1] = q1 + 0.5 * timeGap * k1[1];
        qm[2] = q2 + 0.5 * timeGap * k1[2];
        qm[3] = q3 + 0.5 * timeGap * k1[3];

        k2[0] = (-(wx + dx) * qm[1] - (wy + dy) * qm[2] - (wz + dz) * qm[3]) / 4;
        k2[1] = ((wx + dx) * qm[0] - (wz + dz) * qm[2] - (wy + dy) * qm[3]) / 4;
        k2[2] = ((wy + dy) * qm[0] - (wz + dz) * qm[1] + (wx + dx) * qm[3]) / 4;
        k2[3] = ((wz + dz) * qm[0] + (wy + dy) * qm[1] - (wx + dx) * qm[2]) / 4;
        qm[0] = q0 + 0.5 * timeGap * k2[0];
        qm[1] = q1 + 0.5 * timeGap * k2[1];
        qm[2] = q2 + 0.5 * timeGap * k2[2];
        qm[3] = q3 + 0.5 * timeGap * k2[3];

        k3[0] = (-(wx + dx) * qm[1] - (wy + dy) * qm[2] - (wz + dz) * qm[3]) / 4;
        k3[1] = ((wx + dx) * qm[0] - (wz + dz) * qm[2] - (wy + dy) * qm[3]) / 4;
        k3[2] = ((wy + dy) * qm[0] - (wz + dz) * qm[1] + (wx + dx) * qm[3]) / 4;
        k3[3] = ((wz + dz) * qm[0] + (wy + dy) * qm[1] - (wx + dx) * qm[2]) / 4;
        qm[0] = q0 + k3[0] * timeGap;
        qm[1] = q1 + k3[1] * timeGap;
        qm[2] = q2 + k3[2] * timeGap;
        qm[3] = q3 + k3[3] * timeGap;

        k4[0] = (-(wx + dx) * qm[1] - (wy + dy) * qm[2] - (wz + dz) * qm[3]) / 2;
        k4[1] = ((wx + dx) * qm[0] - (wz + dz) * qm[2] - (wy + dy) * qm[3]) / 2;
        k4[2] = ((wy + dy) * qm[0] - (wz + dz) * qm[1] + (wx + dx) * qm[3]) / 2;
        k4[3] = ((wz + dz) * qm[0] + (wy + dy) * qm[1] - (wx + dx) * qm[2]) / 2;


        q0 += (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0]) / 6;
        q1 += (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1]) / 6;
        q2 += (k1[2] + 2 * k2[2] + 2 * k3[2] + k4[2]) / 6;
        q3 += (k1[3] + 2 * k2[3] + 2 * k3[3] + k4[3]) / 6;

        double norm = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 /= norm;
        q1 /= norm;
        q2 /= norm;
        q3 /= norm;

        roll = Math.atan2(2 * (q2 * q3 + q0 * q1), 1 - 2 * (q1 * q1 + q2 * q2));
        pitch = Math.asin(2 * (q0 * q2 - q1 * q3));
        heading = Math.atan2(2 * (q1 * q2 + q0 * q3), 1 - 2 * (q2 * q2 + q3 * q3));

        if (heading > Math.PI)
            heading -= Math.PI * 2;
        else if (heading < -Math.PI)
            heading += Math.PI * 2;

        DecimalFormat df = new DecimalFormat("#0.0000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI) + "°");
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI) + "°");
        t3.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");
    }

    public void UpdatePosition(ArrayList<Double> stepTime) {
        stepNumber += 1;

        double Sf;
        if (stepTime.size() < 3) {
            Sf = 1;
        } else {
            Sf = 1 / (0.8 * (stepTime.get(2) - stepTime.get(1)) + 0.2 * (stepTime.get(1) - stepTime.get(0)));
        }
        float meter = (float) (dis + 0.371 * (height - 1.6) + 0.227 * (Sf - 1.79) * height / 1.6);
//        p.draw((float) heading, meter);

        totalDistance += meter;
        DecimalFormat df = new DecimalFormat("#0.00000");
        textStep.setText("步数: " + stepNumber);
        textDist.setText("移动距离: " + df.format(totalDistance) + "m");

        double[] xy = CordTrans.BL2xy(latitude, longitude);
        xy[0] += meter * Math.cos(heading);
        xy[1] += meter * Math.sin(heading);
        double[] BL = CordTrans.xytoBL(xy[0], xy[1], 114);
        lat.setText("纬度: " + String.format("%.8f", BL[0]) + "°");
        lon.setText("经度: " + String.format("%.8f", BL[1]) + "°");
    }

    public boolean DetectStep(ArrayList<Double> Acc3Epoch) {
        boolean flag = false;
        double accNorm1, accNorm2, accNorm3;

        accNorm1 = Acc3Epoch.get(0);
        accNorm2 = Acc3Epoch.get(1);
        accNorm3 = Acc3Epoch.get(2);
        double lasttime = lastGYROtime;

        double thread = 12;
        if (accNorm2 > accNorm1 && accNorm2 > accNorm3 && accNorm2 >= thread) {
            if (stepTime.size() == 3) {
                stepTime.remove(0);
            }
            if (stepTime.size() == 0) {
                stepTime.add(lasttime);
                flag = true;
            } else if (stepTime.size() == 1 && (lasttime - stepTime.get(0) > 0.4)) {
                stepTime.add(lasttime);
                flag = true;
            } else if ((lasttime - stepTime.get(stepTime.size() - 1)) > 0.4) {
                stepTime.add(lasttime);
            }

            if (stepTime.size() == 3) {
                double minTimeGap = 1;
                if ((stepTime.get(2) - stepTime.get(0)) > minTimeGap) {
                    flag = true;
                }
            }

        }

        return flag;
    }
}
