package com.example.mypdr;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import java.util.ArrayList;
import java.util.List;

public class SensorView extends AppCompatActivity implements SensorEventListener {
    private LineChart accChart,gyochart,magchart;
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
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accxEntries.add(new Entry(accxEntries.size(), x));
            accyEntries.add(new Entry(accyEntries.size(), y));
            acczEntries.add(new Entry(accxEntries.size(), z));
            updateChart(accChart, accxEntries, accyEntries, acczEntries, "加速度计");
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            gyrxEntries.add(new Entry(gyrxEntries.size(), x));
            gyryEntries.add(new Entry(gyryEntries.size(), y));
            gyrzEntries.add(new Entry(gyrzEntries.size(), z));
            updateChart(gyochart, gyrxEntries, gyryEntries, gyrzEntries, "陀螺仪");
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            magxEntries.add(new Entry(magxEntries.size(), x));
            magyEntries.add(new Entry(magyEntries.size(), y));
            magzEntries.add(new Entry(magzEntries.size(), z));
            updateChart(magchart, magxEntries, magyEntries, magzEntries, "磁传感器");
        }
    }

    private void updateChart(LineChart chart, List<Entry> entriesX, List<Entry> entriesY, List<Entry> entriesZ, String label) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                LineDataSet xData = new LineDataSet(entriesX, label +"x");
                xData.setColor(Color.RED);
                xData.setCircleColor(Color.RED);
                LineDataSet yData = new LineDataSet(entriesY, label +"y");
                yData.setColor(Color.GREEN);
                yData.setCircleColor(Color.GREEN);
                LineDataSet zData = new LineDataSet(entriesZ, label +"z");
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
