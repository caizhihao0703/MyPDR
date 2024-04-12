package com.example.mypdr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.SeekBar;
import android.widget.TextView;

public class Setting extends AppCompatActivity {
    SeekBar seekBar1;
    SeekBar seekBar2;
    TextView textView1;
    TextView textView2;

    public static double H = 1.8;
    public static double dis = 0.7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        seekBar1 = findViewById(R.id.seekBar1);
        textView1 = findViewById(R.id.attText1);
        seekBar2 = findViewById(R.id.seekBar2);
        textView2 = findViewById(R.id.attText2);
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新TextView的数值
                progress += 140;
                H = progress / 100.0;
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
                progress -= 50;
                dis = progress / 100;
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

    public void turnToMenu(View view) {
        Intent intent = new Intent();
        intent.setClass(Setting.this, Menu.class);
        startActivity(intent);
    }
}