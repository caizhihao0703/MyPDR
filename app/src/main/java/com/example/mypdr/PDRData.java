package com.example.mypdr;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import java.util.Date;
import java.util.Scanner;

public class PDRData extends AppCompatActivity {

    private TextView dataView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        File file = new File(getExternalFilesDir(null), "pdrData.txt");
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);

            // 使用StringBuilder将文件中的所有行组合到一起
            StringBuilder data = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                data.append(line).append("\n");
            }

            // 找到你的TextView，并将数据设置到TextView
            dataView = findViewById(R.id.dataView);
            dataView.setText(data.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

    }



}