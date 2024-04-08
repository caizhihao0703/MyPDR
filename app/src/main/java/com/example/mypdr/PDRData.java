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

public class PDRData extends AppCompatActivity {

    private TextView dataView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        dataView = findViewById(R.id.dataView);
        String data = loadDataFromFile();
        dataView.setText(data);
    }

    private String loadDataFromFile() {
        // 定义一个StringBuilder来存储文件内容
        StringBuilder contentBuilder = new StringBuilder();

        try {
            // 定位到你的数据文件
            File file = new File(Environment.getExternalStorageDirectory(), "PDRdata.txt");

            // 用BufferedReader包装FileReader来提高效率
            // try-with-resources语句确保BufferedReader在读取完文件后能正确关闭
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;

                // 逐行读取文件内容
                while ((line = br.readLine()) != null) {
                    contentBuilder.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            // 如果遇到错误，打印错误信息
            e.printStackTrace();
        }

        // 返回文件内容
        return contentBuilder.toString();
    }


}