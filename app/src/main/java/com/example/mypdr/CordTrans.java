package com.example.mypdr;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

public class CordTrans {

    // 定义常量
    static final double PI = 3.141592653589793238463;
    static final double a = 6378245;
    static final double b = 6356752.3141;
    static final double e2 = 0.00669438002290;
    static final double e_ = sqrt(0.00673949677548);

    // 大地坐标转投影坐标
    public static double[] BL2xy(double B, double L) {
        //把度转化为弧度
        B = B * PI / 180.0;
        L = L * PI / 180.0;

        double N, t, n, Xz, m1, m2, m3, m4, m5, m6, a0, a2, a4, a6, a8, M0, M2, M4, M6, M8, x0, y0, l;

        int L_num;
        double L_center;

        //中央子午线经度，3°带
        L_num = (int) (L * 180 / PI / 3.0 + 0.5);
        L_center = 3 * L_num;

        M0 = a * (1 - e2);
        M2 = 3.0 / 2.0 * e2 * M0;
        M4 = 5.0 / 4.0 * e2 * M2;
        M6 = 7.0 / 6.0 * e2 * M4;
        M8 = 9.0 / 8.0 * e2 * M6;

        a0 = M0 + M2 / 2.0 + 3.0 / 8.0 * M4 + 5.0 / 16.0 * M6 + 35.0 / 128.0 * M8;
        a2 = M2 / 2.0 + M4 / 2 + 15.0 / 32.0 * M6 + 7.0 / 16.0 * M8;
        a4 = M4 / 8.0 + 3.0 / 16.0 * M6 + 7.0 / 32.0 * M8;
        a6 = M6 / 32.0 + M8 / 16.0;
        a8 = M8 / 128.0;

        Xz = a0 * B - a2 / 2.0 * sin(2 * B) + a4 / 4.0 * sin(4 * B) - a6 / 6.0 * sin(6 * B) + a8 / 8.0 * sin(8 * B);  //计算子午线弧长

        l = (L / PI * 180 - L_center) * PI / 180.0; //求带号、中央经线、经差

        N = a / sqrt(1 - e2 * sin(B) * sin(B));
        n = e_ * cos(B);
        t = tan(B);

        m1 = N * cos(B);
        m2 = N / 2.0 * sin(B) * cos(B);
        m3 = N / 6.0 * pow(cos(B), 3) * (1 - t * t + n * n);
        m4 = N / 24.0 * sin(B) * pow(cos(B), 3) * (5 - t * t + 9 * n * n + 4 * pow(n, 4));
        m5 = N / 120.0 * pow(cos(B), 5) * (5 - 18 * t * t + pow(t, 4) + 14 * n * n - 58 * n * n * t * t);
        m6 = N / 720.0 * sin(B) * pow(cos(B), 5) * (61 - 58 * t * t + pow(t, 4));
        x0 = Xz + m2 * l * l + m4 * pow(l, 4) + m6 * pow(l, 6);
        y0 = m1 * l + m3 * pow(l, 3) + m5 * pow(l, 5);   //计算x y坐标

        double x = x0;
        double y = y0 + 500000.0;     //化为国家统一坐标

        return new double[]{x, y};
    }

    // 投影坐标转大地坐标
    public static double[] xytoBL(double x, double y, double l0) {
        // l0为中央经度
        double Bf, B0, FBf, M, N, V, t, n, c, y1, n1, n2, n3, n4, n5, n6, a0, a2, a4, a6, M0, M2, M4, M6, M8, l;

        y1 = y - 500000;

        M0 = a * (1 - e2);
        M2 = 3.0 / 2.0 * e2 * M0;
        M4 = 5.0 / 4.0 * e2 * M2;
        M6 = 7.0 / 6.0 * e2 * M4;
        M8 = 9.0 / 8.0 * e2 * M6;

        a0 = M0 + M2 / 2.0 + 3.0 / 8.0 * M4 + 5.0 / 16.0 * M6 + 35.0 / 128.0 * M8;
        a2 = M2 / 2.0 + M4 / 2 + 15.0 / 32.0 * M6 + 7.0 / 16.0 * M8;
        a4 = M4 / 8.0 + 3.0 / 16.0 * M6 + 7.0 / 32.0 * M8;
        a6 = M6 / 32.0 + M8 / 16.0;

        Bf = x / a0;
        B0 = Bf;

        while ((Math.abs(Bf - B0) > 0.0000001) || (B0 == Bf)) {
            B0 = Bf;
            FBf = -a2 / 2.0 * sin(2 * B0) + a4 / 4.0 * sin(4 * B0) - a6 / 6.0 * sin(6 * B0);
            Bf = (x - FBf) / a0;
        } // 迭代求数值为x坐标的子午线弧长对应的底点纬度


        N = a / sqrt(1 - e2 * sin(Bf) * sin(Bf));
        t = tan(Bf);
        M = a * (1 - e2) / sqrt(pow(1 - e2 * sin(Bf) * sin(Bf), 3));
        n = e_ * e_ * cos(Bf) * cos(Bf); // 为n的平方

        n1 = 1 / (N * cos(Bf));
        n2 = -t / (2.0 * M * N);
        n3 = -(1 + 2 * t * t + n) / (6.0 * pow(N, 3) * cos(Bf));
        n4 = t * (5 + 3 * t * t + n - 9 * n * t * t) / (24.0 * M * pow(N, 3));
        n5 = (5 + 28 * t * t + 24 * pow(t, 4) + 6 * n + 8 * n * t * t) / (120.0 * pow(N, 5) * cos(Bf));
        n6 = -t * (61 + 90 * t * t + 45 * pow(t, 4)) / (720.0 * M * pow(N, 5));

        // 度
        double B = (Bf + n2 * y1 * y1 + n4 * pow(y1, 4) + n6 * pow(y1, 6)) / PI * 180;
        double L0 = l0;

        l = n1 * y1 + n3 * pow(y1, 3) + n5 * pow(y1, 5);
        double L = L0 + l / PI * 180; // 反算得大地经纬度

        return new double[]{B, L};
    }
}



