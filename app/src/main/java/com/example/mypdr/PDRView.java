package com.example.mypdr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

public class PDRView extends View {

    private float startXNow = 200;
    private float startYNow = 200;

    private ArrayList<Float> X = new ArrayList<Float>();
    private ArrayList<Float> Y = new ArrayList<Float>();

    private ArrayList<Float> XForDraw = new ArrayList<Float>();
    private ArrayList<Float> YForDraw = new ArrayList<Float>();

    private float yaw;
    private float stepLength;

    private  boolean startPDR = false;

    public PDRView(Context context) {
        super(context);

    }

    public PDRView(Context context, AttributeSet attrs) {
        super(context, attrs);
        X.add(startXNow);
        Y.add(startYNow);
    }

    private int width;
    private int height;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!startPDR){
            return;
        }
        super.onDraw(canvas);
        canvas.rotate(-90);
        canvas.translate(-height, 0);

        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);

        // 计算网格大小，这里设定为每个格子20像素
        int gridSize = 20;

        // 在画布上绘制横向和纵向的线，形成网格
        for (int i = 0; i < getWidth(); i += gridSize) {
            canvas.drawLine(i, 0, i, getHeight(), paint);
        }
        for (int i = 0; i < getHeight(); i += gridSize) {
            canvas.drawLine(0, i, getWidth(), i, paint);
        }

        @SuppressLint("DrawAllocation") Paint p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(10);

        double endX = startXNow + stepLength * Math.cos(yaw)*0.25;
        double endY = startYNow + stepLength * Math.sin(yaw)*0.25;

        X.add((float)endX);
        Y.add((float)endY);

        float maxX = Collections.max(X);
        float maxY = Collections.max(Y);
        float minX = Collections.min(X);
        float minY = Collections.min(Y);

        float max, min;
        max = Math.max(maxX, maxY);
        min = Math.min(minX, minY);

        for (int i = 0; i < X.size(); i++) {
            float XD = 200+(1000-100)/(max-min)*(X.get(i)-min);
            float YD = 200+(1500-600)/(max-min)*(Y.get(i)-min);
            XForDraw.add(XD);
            YForDraw.add(YD);
        }

        maxX = Collections.max(XForDraw);
        maxY = Collections.max(YForDraw);
        minX = Collections.min(XForDraw);
        minY = Collections.min(YForDraw);
        float average;
        average = (maxY + minY) / 2;
        for (int i = 0; i < X.size(); i++) {
            YForDraw.set(i, YForDraw.get(i)+700-average);
        }

        average = (maxX + minX) / 2;
        for (int i = 0; i < X.size(); i++) {
            XForDraw.set(i, XForDraw.get(i)+500-average);
        }

        for (int i = 0; i < XForDraw.size() - 1; i++) {
            canvas.drawLine(XForDraw.get(i), YForDraw.get(i), XForDraw.get(i+1), YForDraw.get(i+1), p);
        }

        startXNow = (float)endX;
        startYNow = (float)endY;

        XForDraw.clear();
        YForDraw.clear();

        canvas.save();
    }

    public void draw(float heading, float length){
        startPDR = true;
        yaw = heading;
        stepLength = length;
        invalidate();
    }


}
