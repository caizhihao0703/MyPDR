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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class Data extends AppCompatActivity {

    TextView t;
    public static int mod = 0;
    int maxLine = 200;
    int totalNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);
        t = findViewById(R.id.dataView);
        t.setMovementMethod(ScrollingMovementMethod.getInstance());
        try {
            loadAndShow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        totalNumber = 0;
    }

    @SuppressLint("SetTextI18n")
    private void loadAndShow() throws IOException {
        FileInputStream fis = null;
        if(mod == 0)
            fis = openFileInput("data.txt");
        else if (mod == 1) {
            fis = openFileInput("PDRdata.txt");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            StringBuilder builder = new StringBuilder();
            while((line = reader.readLine()) != null) {
                if(totalNumber < maxLine){
                    builder.append(line);
                    builder.append("\n");
                }
                totalNumber += 1;
            }
            t.setText(builder.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
        if(fis != null){
            try {
                fis.close();
            } catch (IOException e){
                e.printStackTrace();
            }

        }
        TextView t = findViewById(R.id.totalNumberText);
        t.setText("line number : " + totalNumber + "        time duration : " + 25*totalNumber/1000);
    }
    }

    TextView t1;
    public void clearData(View view) throws IOException {
        t1=(TextView)findViewById(R.id.clearData);
        FileOutputStream fos = null;
        if(mod == 0) {
            fos = openFileOutput("data.txt", MODE_PRIVATE);
            fos = openFileOutput("yAcc.txt", MODE_PRIVATE);
        }
        else if (mod == 1) {
            fos = openFileOutput("PDRdata.txt", MODE_PRIVATE);
        }
        assert fos != null;
        fos.close();

        fos.close();
    }



    public void saveFile(View view) {
        FileOutputStream fos = null;
        //获取SD卡状态
        String state = Environment.getExternalStorageState();
        //判断SD卡是否就绪
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "请检查SD卡", Toast.LENGTH_SHORT).show();
            return;
        }
        //取得SD卡根目录
        File file = Environment.getExternalStorageDirectory();
        try {
            Log.d("======SD卡根目录：", "" + file.getCanonicalPath().toString());
            //File myFile=new File(file.getCanonicalPath()+"/sd.txt");
            /*
            输出流的构造参数1：可以是File对象  也可以是文件路径
            输出流的构造参数2：默认为False=>覆盖内容； true=>追加内容
             */
            String timeString = getDate();
            fos = new FileOutputStream(file.getCanonicalPath() + "/" + timeString + "Data.txt");
            // fos = new FileOutputStream(file.getCanonicalPath() + "/sd.txt",true);
            //fos=new FileOutputStream(myFile);
            String str = getData();
            fos.write(str.getBytes());
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getData() throws FileNotFoundException {
        FileInputStream fis = null;
        if(mod == 0)
            fis = openFileInput("data.txt");
        else if (mod == 1) {
            fis = openFileInput("PDRdata.txt");
        }
        String returnString = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            StringBuilder builder = new StringBuilder();
            while((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            returnString = builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e){
                    e.printStackTrace();
                }

            }
        }
        return returnString;
    }

    public String getDate(){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }


}