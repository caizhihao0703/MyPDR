package com.example.mypdr;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SensorView extends AppCompatActivity implements SensorEventListener {
    private LineChart accChart, gyochart, magchart;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor, gyroscopeSensor, magneticSensor;
    private List<Entry> accxEntries = new ArrayList<>();
    private List<Entry> accyEntries = new ArrayList<>();
    private List<Entry> acczEntries = new ArrayList<>();
    private List<Entry> gyrxEntries = new ArrayList<>();
    private List<Entry> gyryEntries = new ArrayList<>();
    private List<Entry> gyrzEntries = new ArrayList<>();
    private List<Entry> magxEntries = new ArrayList<>();
    private List<Entry> magyEntries = new ArrayList<>();
    private List<Entry> magzEntries = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private long startTime;
    private static final long MAX_DISPLAY_TIME_MS = 5000; // 10秒



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensorview);

        accChart = findViewById(R.id.accChart);
        gyochart = findViewById(R.id.gyoChart);
        magchart = findViewById(R.id.magChart);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Description description = new Description();
        description.setText("Real-time Sensor Data");
        accChart.setDescription(description);
        gyochart.setDescription(description);
        magchart.setDescription(description);

        startSensorUpdates();
    }

    private void startSensorUpdates() {
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float timeElapsed = (float) ((System.currentTimeMillis() - startTime) / 1000.0);
            // 添加数据时，判断时间范围，只保留最近一段时间内的数据
            if (timeElapsed <= MAX_DISPLAY_TIME_MS / 1000) {
                accxEntries.add(new Entry(timeElapsed, x));
                accyEntries.add(new Entry(timeElapsed, y));
                acczEntries.add(new Entry(timeElapsed, z));
            } else {
                // 移除过期的数据
                accxEntries.remove(0);
                accyEntries.remove(0);
                acczEntries.remove(0);
                accxEntries.add(new Entry(timeElapsed, x));
                accyEntries.add(new Entry(timeElapsed, y));
                acczEntries.add(new Entry(timeElapsed, z));
            }
            updateChart(accChart, accxEntries, accyEntries, acczEntries, "加速度计");
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float timeElapsed = (float) ((System.currentTimeMillis() - startTime) / 1000.0);

            if (timeElapsed <= MAX_DISPLAY_TIME_MS / 1000) {
                gyrxEntries.add(new Entry(timeElapsed, x));
                gyryEntries.add(new Entry(timeElapsed, y));
                gyrzEntries.add(new Entry(timeElapsed, z));
            } else {
                // 移除过期的数据
                gyrxEntries.remove(0);
                gyryEntries.remove(0);
                gyrzEntries.remove(0);
                gyrxEntries.add(new Entry(timeElapsed, x));
                gyryEntries.add(new Entry(timeElapsed, y));
                gyrzEntries.add(new Entry(timeElapsed, z));
            }

            updateChart(gyochart, gyrxEntries, gyryEntries, gyrzEntries, "陀螺仪");
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float timeElapsed = (float) ((System.currentTimeMillis() - startTime) / 1000.0);

            if (timeElapsed <= MAX_DISPLAY_TIME_MS / 1000) {
                magxEntries.add(new Entry(timeElapsed, x));
                magyEntries.add(new Entry(timeElapsed, y));
                magzEntries.add(new Entry(timeElapsed, z));
            } else {
                // 移除过期的数据
                magxEntries.remove(0);
                magyEntries.remove(0);
                magzEntries.remove(0);
                magxEntries.add(new Entry(timeElapsed, x));
                magyEntries.add(new Entry(timeElapsed, y));
                magzEntries.add(new Entry(timeElapsed, z));
            }

            updateChart(magchart, magxEntries, magyEntries, magzEntries, "磁传感器");
        }
    }

    private void updateChart(LineChart chart, List<Entry> entriesX, List<Entry> entriesY, List<Entry> entriesZ, String label) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                LineDataSet xData = new LineDataSet(entriesX, label + "x");
                xData.setColor(Color.RED);
                xData.setCircleColor(Color.RED);
                LineDataSet yData = new LineDataSet(entriesY, label + "y");
                yData.setColor(Color.GREEN);
                yData.setCircleColor(Color.GREEN);
                LineDataSet zData = new LineDataSet(entriesZ, label + "z");
                zData.setColor(Color.BLUE);
                zData.setCircleColor(Color.BLUE);

                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(xData);
                dataSets.add(yData);
                dataSets.add(zData);

                LineData Data = new LineData(dataSets);

                chart.setData(Data);
                chart.invalidate();
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSensorUpdates();
    }
}