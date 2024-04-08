package com.example.mypdr;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import android.location.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import java.io.*;
import java.text.*;
import java.util.*;


public class PDR extends AppCompatActivity {
    boolean usingYAcc = false, usingGyro = false, usingMag = false;
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

    TextView textStep, textDist;
    TextView t1, t2, t3, textTime;
    TextView nowAddress, lat, lon;
    PDRView p;
    int stepNumber = 0;
    double stepLength = 0.5;
    private Handler timehandler;
    private SimpleDateFormat dateFormat;
    Button startPDR, endPDR, showMap;
    static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    LocationManager locationManager;
    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);
        textStep = findViewById(R.id.stepNumber);
        textStep.setText("步数: ");
        startPDR = findViewById(R.id.startPDR);
        endPDR = findViewById(R.id.endPDR);
        endPDR.setVisibility(View.INVISIBLE);
        showMap = findViewById(R.id.showMap);
        showMap.setVisibility(View.INVISIBLE);
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
        // 检查位置权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有位置权限，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 如果已经有位置权限，开始获取位置信息
            startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新设置经度信息
        if (locationListener != null && locationManager != null &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                String slat = String.format("纬度: %.4f" , latitude);
                String slon = String.format("经度: %.4f" , longitude);
                lat.setText(slat);
                lon.setText(slon);
                // 定义位置解析
                Geocoder geocoder = new Geocoder(PDR.this, Locale.getDefault());
                try {
                    // 获取经纬度对于的位置
                    // getFromLocation(纬度, 经度, 最多获取的位置数量)
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    // 得到第一个经纬度位置解析信息
                    Address address = addresses.get(0);
                    // 获取到详细的当前位置
                    // Address里面还有很多方法你们可以自行实现去尝试。比如具体省的名称、市的名称...
                    String info = address.getAddressLine(0);
                    // 赋值
                    nowAddress.setText("位置: "+info);
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startLocationUpdates() {
        // 创建位置监听器
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 处理位置变化事件，location包含了实时位置信息
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String slat = String.format("纬度: %.4f" , latitude);
                String slon = String.format("经度: %.4f" , longitude);
                lat.setText(slat);
                lon.setText(slon);
                // 定义位置解析
                Geocoder geocoder = new Geocoder(PDR.this, Locale.getDefault());
                try {
                    // 获取经纬度对于的位置
                    // getFromLocation(纬度, 经度, 最多获取的位置数量)
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    // 得到第一个经纬度位置解析信息
                    Address address = addresses.get(0);
                    // 获取到详细的当前位置
                    // Address里面还有很多方法你们可以自行实现去尝试。比如具体省的名称、市的名称...
                    String info = address.getAddressLine(0);
                    // 赋值
                    nowAddress.setText("位置: "+info);
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        // 请求位置更新前再次检查权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // 已经有位置权限，请求位置更新
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 0, locationListener); // 修改这里的时间间隔为1000毫秒（1秒），距离间隔为0米
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了位置权限，开始获取位置信息
                startLocationUpdates();
            } else {
                // 用户拒绝了位置权限，可以给出相应提示或处理逻辑
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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

    float totalDistance = 0;
    Handler handler = new Handler();
    Timer timer = new Timer(true);
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    secondToNow += timeGap;
                    updateHeanding(secondToNow);
                    try {
                        writeData();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (realTimeGetStep2((float) Math.sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2]), secondToNow)) {
                        stepNumber += 1;
                        textStep.setText("步数: " + stepNumber);
                        float meter = (float) (0.4 * Math.pow((float) (maxAcc2 - minAcc2), 1f / 4f));
                        p.draw((float) heading, meter);
                        totalDistance += meter;
                        DecimalFormat df = new DecimalFormat("#0.00000");
                        textDist.setText("移动距离: " + df.format(totalDistance) + "m");
                    }
                }
            });
        }
    };

    protected void writeData() throws IOException {
        String s = String.valueOf(secondToNow);
        DecimalFormat df = new DecimalFormat("#0.0000");
        s += " " + df.format(accData[0]) + " " + df.format(accData[1]) + " " + df.format(accData[2]);
        s += " " + df.format(gyrData[0]) + " " + df.format(gyrData[1]) + " " + df.format(gyrData[2]);
        s += " " + df.format(magData[0]) + " " + df.format(magData[1]) + " " + df.format(magData[2]) + "\n";
        writeData(s);
        //String yAcc= String.valueOf(accData[1]) + "\n";
    }

    public void writeData(String data) throws IOException {
        FileOutputStream fos = openFileOutput("PDRdata.txt", MODE_APPEND);
        fos.write(data.getBytes());
        fos.close();
    }


    yAccListener accSensor;
    gyroListener gyroL;
    magListener magSensor;
    SensorManager sensorManager;
    Sensor acc;
    Sensor gyr;
    Sensor mag;

    public void getSensor(View view) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        //TextView t = (TextView) findViewById(R.id.text1);
        //t.setText("");
        accSensor = new yAccListener();
        gyroL = new gyroListener();
        magSensor = new magListener();
        for (Sensor s : sensors) {
            if (s.getType() == Sensor.TYPE_ACCELEROMETER && !usingYAcc) {
                sensorManager.registerListener(accSensor, s, SensorManager.SENSOR_DELAY_UI);
                usingYAcc = true;
                acc = s;
            }
            if (s.getType() == Sensor.TYPE_GYROSCOPE && !usingGyro) {
                sensorManager.registerListener(gyroL, s, SensorManager.SENSOR_DELAY_UI);
                usingGyro = true;
                gyr = s;
            }
            if (s.getType() == Sensor.TYPE_MAGNETIC_FIELD && !usingMag) {
                sensorManager.registerListener(magSensor, s, SensorManager.SENSOR_DELAY_UI);
                usingMag = true;
                mag = s;
            }
        }
        timer.schedule(timerTask, 3000, timeGap);

        startPDR.setVisibility(View.GONE);
        endPDR.setVisibility(View.VISIBLE);
        showMap.setVisibility(View.VISIBLE);
        Toast.makeText(this, "请待上方出现数据后开始行走", Toast.LENGTH_SHORT).show();
        //t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    }

    public void endSensor(View view) {
        sensorManager.unregisterListener(accSensor, acc);
        sensorManager.unregisterListener(gyroL, gyr);
        sensorManager.unregisterListener(magSensor, mag);
        timer.cancel();
        showData.mod = 1;
        Intent intent = new Intent();
        intent.setClass(PDR.this, showData.class);
        startActivity(intent);
    }

    public void showMap(View view) {
        Intent intent = new Intent();
        intent.setClass(PDR.this, showMap.class);
        startActivity(intent);
    }

    private double roll, pitch;
    private double mx, my;
    private double omegaD, PsiD;
    private double heading;
    private double lastTime = 0;
    private double startHeading = 0;
    private double lastOD = 0;

    @SuppressLint("SetTextI18n")
    public void updateHeanding(float time) {
        //航向角
        pitch = Math.atan2(accData[1], Math.sqrt(accData[0] * accData[0] + accData[2] * accData[2]));
        roll = Math.atan2(-accData[0], -accData[2]);
        mx = magData[0] * Math.cos(pitch) * +magData[1] * Math.sin(roll) * Math.sin(pitch) + magData[2] * Math.cos(roll) * Math.sin(pitch);
        my = magData[1] * Math.cos(roll) - magData[2] * Math.sin(roll);
        PsiD = -Math.atan2(my, mx) * 180 / Math.PI;


        omegaD = -Math.sin(pitch) * gyrData[0] / Math.PI * 180 + Math.sin(roll) * Math.cos(pitch) * gyrData[1] / Math.PI * 180 + Math.cos(roll) * Math.cos(pitch) * gyrData[2] / Math.PI * 180;
        if (Math.abs(omegaD) < 0.5f && Math.abs(lastOD) < 0.5f)
            omegaD *= 0.0005;
        double od = startHeading / 180 * Math.PI + omegaD / 180 * Math.PI * (time - lastTime) * 0.001;
        if (od > Math.PI)
            od -= Math.PI * 2;
        if (od < -Math.PI)
            od += Math.PI * 2;
        heading = od;//弧度
        startHeading = heading / Math.PI * 180;
        lastTime = time;
        lastOD = omegaD;
        DecimalFormat df = new DecimalFormat("#0.00000");
        t1.setText("横滚角: " + df.format(roll * 180 / Math.PI));
        t2.setText("俯仰角: " + df.format(pitch * 180 / Math.PI));
        t3.setText("航向角: " + df.format(startHeading));
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

    private class yAccListener implements SensorEventListener {
        @SuppressLint("SetTextI18n")
        @Override
        public void onSensorChanged(SensorEvent event) {
            //TextView t1=(TextView)findViewById(R.id.accText);
            accData[0] = event.values[0];
            accData[1] = event.values[1];
            accData[2] = event.values[2];
            //String s = "ACC : [x: "+event.values[0] + "][y: "+event.values[1] + "][z: "+event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class gyroListener implements SensorEventListener {
        @SuppressLint("SetTextI18n")
        @Override
        public void onSensorChanged(SensorEvent event) {
            gyrData[0] = event.values[0];
            gyrData[1] = event.values[1];
            gyrData[2] = event.values[2];
            String s = "GYRO : [x: " + event.values[0] + "][y: " + event.values[1] + "][z: " + event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class magListener implements SensorEventListener {
        @SuppressLint("SetTextI18n")
        @Override
        public void onSensorChanged(SensorEvent event) {
            //TextView t1=(TextView)findViewById(R.id.mag);

            String s = "MAG : [x: " + event.values[0] + "][y: " + event.values[1] + "][z: " + event.values[2];
            magData[0] = event.values[0];
            magData[1] = event.values[1];
            magData[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}