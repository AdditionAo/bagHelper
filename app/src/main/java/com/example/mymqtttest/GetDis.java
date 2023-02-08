package com.example.mymqtttest;

public class GetDis {

    /**
     * 地球赤道半径(km)
     * */
    public final static double EARTH_RADIUS = 6378.137;
    /**
     * 地球每度的弧长(km)
     * */
    public final static double EARTH_ARC = 111.199;

    /**
     * 转化为弧度(rad)
     * */
    public static double rad(double d) {
        return d * Math.PI / 180.0;
    }
    public static double GetDistanceOne(double lon1, double lat1, double lon2,
                                        double lat2) {
        double r1 = rad(lat1);
        double r2 = rad(lon1);
        double a = rad(lat2);
        double b = rad(lon2);
        double s = Math.acos(Math.cos(r1) * Math.cos(a) * Math.cos(r2 - b)
                + Math.sin(r1) * Math.sin(a))
                * EARTH_RADIUS;
        return s;
    }
}
