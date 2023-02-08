package com.example.mymqtttest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class SPHelper {
    private String defaultModelName;

    private static SPHelper spHelperInstant;

    private SPHelper() {

    }

    public static SPHelper getInstant(Context context) {
        if (spHelperInstant == null) {
            synchronized (SPHelper.class) {
                if (spHelperInstant == null) {
                    spHelperInstant = new SPHelper();
                    spHelperInstant.defaultModelName = "";
                }
            }
        }
        return spHelperInstant;
    }

    /**
     * sharepreference方式存储
     *
     * @param context   context
     * @param modelName XML的文件名，通常取模块名
     * @param key       存储在XML中的key
     * @param value     值
     */
    public void putData2SP(Context context, String modelName, String key, Object value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(modelName, Context.MODE_PRIVATE); // 私有数据
        Editor editor = sharedPreferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else {
            return;
        }
        editor.apply();
    }

    public void putData2SP(Context context, String key, Object value) {
        putData2SP(context, defaultModelName, key, value);
    }

    public String getStringFromSP(Context context, String key) {
        return getStringFromSP(context, defaultModelName, key);
    }

    public  String getStringFromSP(Context context, String modelName, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(modelName, Context.MODE_PRIVATE); // 私有数据
        return sharedPreferences.getString(key, "");
    }

    public Boolean getBooleanFromSP(Context context, String modelName, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(modelName, Context.MODE_PRIVATE); // 私有数据
        return sharedPreferences.getBoolean(key, true);
    }

    public String getStringFromSPDef(Context context, String key, String defVaule) {
        return getStringFromSPDef(context, defaultModelName, key, defVaule);
    }

    public  String getStringFromSPDef(Context context, String modelName, String key, String defVaule) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(modelName, Context.MODE_PRIVATE); // 私有数据
        return sharedPreferences.getString(key, defVaule);
    }
}
