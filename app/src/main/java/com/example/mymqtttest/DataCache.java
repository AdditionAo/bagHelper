package com.example.mymqtttest;

import android.content.Context;
import android.text.TextUtils;

public class DataCache {
    private static String baseUrl;
    private static String userName;
    private static String pwd;
    private static String topic;
    private static String clientId;


    public static String getBaseUrl(Context context) {
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = SPHelper.getInstant(context).getStringFromSP(context, Constant.BASE_URL);
        }
        return baseUrl;
    }

    public static void updateBaseUrl(Context context, String value) {
        baseUrl = value;
        SPHelper.getInstant(context).putData2SP(context, Constant.BASE_URL, value);
    }

    public static String getUserName(Context context) {
        if (TextUtils.isEmpty(userName)) {
            userName = SPHelper.getInstant(context).getStringFromSP(context, Constant.LOGIN_USER_NAME);
        }
        return userName;
    }

    public static void updateUserName(Context context, String value) {
        userName = value;
        SPHelper.getInstant(context).putData2SP(context, Constant.LOGIN_USER_NAME, value);
    }

    public static String getPwd(Context context) {
        if (TextUtils.isEmpty(pwd)) {
            pwd = SPHelper.getInstant(context).getStringFromSP(context, Constant.LOGIN_PWD);
        }
        return pwd;
    }

    public static void updatePwd(Context context, String value) {
        pwd = value;
        SPHelper.getInstant(context).putData2SP(context, Constant.LOGIN_PWD, value);
    }

//    public static String getTopic(Context context) {
//        if (TextUtils.isEmpty(topic)) {
//            topic = SPHelper.getInstant(context).getStringFromSP(context, Constant.TOPIC_DEFAULT_VALVE);
//        }
//        return topic;
//    }
//
//    public static void updateTopic(Context context, String value) {
//        topic = value;
//        SPHelper.getInstant(context).putData2SP(context, Constant.TOPIC_DEFAULT_VALVE, value);
//    }

    public static String getClientId(Context context) {
        if (TextUtils.isEmpty(clientId)) {
            clientId = SPHelper.getInstant(context).getStringFromSP(context, Constant.CLIENT_ID_DEFAULT_VALUE);
        }
        return clientId;
    }

    public static void updateClientId(Context context, String value) {
        clientId = value;
        SPHelper.getInstant(context).putData2SP(context, Constant.CLIENT_ID_DEFAULT_VALUE, value);
    }

}
