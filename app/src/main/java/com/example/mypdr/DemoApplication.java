package com.example.mypdr;

import android.app.Application;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 设置是否同意隐私协议，这里设置为同意
        SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
        // 初始化百度地图 SDK
        SDKInitializer.initialize(this);
        // 设置坐标类型为百度坐标 BD09LL，也可以设置为其他类型如 GCJ02
        SDKInitializer.setCoordType(CoordType.BD09LL);
    }
}
