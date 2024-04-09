package com.example.mypdr;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PDRData extends AppCompatActivity {

    private TextView dataView;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        dataView = findViewById(R.id.dataView);

        loadData();
    }

    private void loadData() {
        Future<StringBuilder> future = executorService.submit(this::loadFileContent);
        executorService.shutdown();

        try {
            StringBuilder data = future.get();
            dataView.setText(data.toString());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder loadFileContent() {
        File file = new File(getExternalFilesDir(null), "pdrData.txt");
        Scanner scanner = null;
        StringBuilder data = new StringBuilder();
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                data.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return data;
    }
}

