package com.example.mypdr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class Ori extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private TextView headingTextView;

    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        headingTextView = findViewById(R.id.headingTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerData, 0, event.values.length);
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerData, 0, event.values.length);
        }

        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float heading = orientationAngles[0];
        if (heading > Math.PI)
            heading -= Math.PI * 2;
        else if (heading < -Math.PI)
            heading += Math.PI * 2;
        DecimalFormat df = new DecimalFormat("#0.0000");
        headingTextView.setText("航向角: " + df.format(heading * 180 / Math.PI) + "°");
    }
}
