package com.example.mypdr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

public class Menu extends AppCompatActivity {
    static final int REQUEST_EXTERNAL_STORAGE = 1;
    static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        //android 6.0以上
        //申请读写权限
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }

        Button toPDR = findViewById(R.id.toPDR);
        toPDR.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, PDR.class);
            startActivity(intent);
        });

        Button collectdata = findViewById(R.id.collectData);
        collectdata.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, SensorView.class);
            startActivity(intent);
        });

        Button setting = findViewById(R.id.setting);
        setting.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, Ori.class);
            startActivity(intent);
        });
    }
}