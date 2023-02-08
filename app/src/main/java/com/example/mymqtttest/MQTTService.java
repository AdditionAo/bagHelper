package com.example.mymqtttest;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class MQTTService extends Service {

    public static final String TAG = MQTTService.class.getSimpleName();
    private Notification notification;
    private double myLongitude;
    private double myLatitude;
    private GetDis getDis=new GetDis();
    private String dpjLon;
    private double dpjLongitude;
    double distance;
    private int isRain=1;
    private String dpjLat;
    private double dpjLatitude;
    private boolean checkFlag = true;
    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private static String host = "";

    private static String userName = "admin";
    private static String passWord = "admin";
    private static String myTopic ;      //要订阅的主题
    private  static double save_distance=200000;

    private static String clientId = "paho263815291130899";//客户端标识
    private IGetMessageCallBack IGetMessageCallBack;
    Intent intent;

    public static void setSave_distance(double save_distance) {
        MQTTService.save_distance = save_distance;
    }

    public static void setHost(String host) {
        MQTTService.host = host;
    }

    public static void setUserName(String userName) {
        MQTTService.userName = userName;
    }

    public static void setPassWord(String passWord) {
        MQTTService.passWord = passWord;
    }

    public static void setMyTopic(String myTopic) {
        MQTTService.myTopic = myTopic;
    }

    public static void setClientId(String clientId) {
        MQTTService.clientId = clientId;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        init();
        initLocation();



    }

    public static void publish(String msg){
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        try {
            if (client != null){
                client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public static void publish(String msg,String topic){
        Integer qos = 0;
        Boolean retained = false;
        try {
            if (client != null){
                client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        // 服务器地址（协议+地址+端口号）
        String uri = host;
        Log.e("fuwuhost",host);
        client = new MqttAndroidClient(this, uri, clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);

        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(20);
        // 用户名
//        conOpt.setUserName(userName);
        // 密码
//        conOpt.setPassword(passWord.toCharArray());     //将字符串转换为字符串数组

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + clientId + "\"}";
        Log.e(getClass().getName(), "message是:" + message);
        String topic = myTopic;
        Integer qos = 0;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
            //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
            //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。

            try {
                conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.i(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }

        if (doConnect) {
            doClientConnection();
        }

    }


    @Override
    public void onDestroy() {
        stopSelf();
        Log.e("服务暂停", "");
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /** 连接MQTT服务器 */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNormal()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
                Log.d("1111", "111111");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                // 订阅myTopic话题
                client.subscribe(myTopic,1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Log.i(TAG, "连接失败 ");
            arg1.printStackTrace();
            // 连接失败，重连
        }
    };

    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.e("服务中check1", "");
            checkRain(message);
            checkDis(message);


            Log.e("服务中check2", "");
            String str1 = new String(message.getPayload());
            if (IGetMessageCallBack != null){
                IGetMessageCallBack.setMessage(str1);
            }

            String str2 = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + str1);
            Log.i(TAG, str2);

        }


        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendNotification(String tips){
        Log.e("服务sendNo", "");
        String id = "channel_001";
        NotificationManager manager;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(id, "channelNAME", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }else{
            manager= (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if(tips=="Rain"){
            notification = new NotificationCompat.Builder(MQTTService.this)
                    .setChannelId(id)
                    .setContentTitle("您的包包进水了")
                    .setSmallIcon(R.mipmap.icon_earth)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .build();
        }else if(tips=="Dis"){
            notification = new NotificationCompat.Builder(MQTTService.this)
                    .setChannelId(id)
                    .setContentTitle("您的包包丢失了")
                    .setSmallIcon(R.mipmap.icon_earth)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .build();
        }


//        Vibrator vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);
//        vibrator.vibrate(1200);
        Vibrator mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
//        long[] patern = {0,1000,1000};
        AudioAttributes audioAttributes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM) //key
                    .build();
            mVibrator.vibrate( 1200, audioAttributes);
        }else {
            mVibrator.vibrate( 1200);
        }


//             SoundPool soundPool = new SoundPool.Builder().build();
//             int soundID = soundPool.load(this, R.raw.alarm, 1);
//            soundPool.play(
//                    soundID,
//                    0.9f,   //左耳道音量【0~1】
//                    0.9f,   //右耳道音量【0~1】
//                    1,     //播放优先级【0表示最低优先级】
//                    0,     //循环模式【0表示循环一次，-1表示一直循环，其他表示数字+1表示当前数字对应的循环次数】
//                    1     //播放速度【1是正常，范围从0~2】
//            );



//        MediaPlayer music = MediaPlayer.create(this, R.raw.alarm);
//        music.start();
        manager.notify(1,notification);

    }

    /** 判断网络是否连接 */
    private boolean isConnectIsNormal() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(getClass().getName(), "onBind");
        //Bundle bundle = intent.getBundleExtra("Message");
//        host=bundle.getString("ip");
//        userName = bundle.getString("userName");
//        passWord = bundle.getString("pwd");
//        myTopic = bundle.getString("topic");
//        clientId = bundle.getString("clientId");
        Log.d("username22222222", userName);
        return (IBinder) new CustomBinder();
    }

    public void setIGetMessageCallBack(IGetMessageCallBack IGetMessageCallBack){
        this.IGetMessageCallBack = IGetMessageCallBack;
    }

    public class CustomBinder extends Binder {
        public MQTTService getService(){
            return MQTTService.this;
        }
    }

    public  void toCreateNotification(String message){
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(this,MQTTService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);//3、创建一个通知，属性太多，使用构造器模式

        Notification notification = builder
                .setTicker("测试标题")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("")
                .setContentText(message)
                .setContentInfo("")
                .setContentIntent(pendingIntent)//点击后才触发的意图，“挂起的”意图
                .setAutoCancel(true)        //设置点击之后notification消失
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(0, notification);
        notificationManager.notify(0, notification);

    }
    public void locationUpdates(Location location) {
        if (location != null) {
            myLongitude = location.getLongitude();
            myLatitude = location.getLatitude();
        } else {
            Log.e("服务中我的纬度",String.valueOf(myLatitude));
            Log.e("服务中我的经度",String.valueOf(myLongitude));
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkRain(MqttMessage message){
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(String.valueOf(message));
            isRain = jsonObject.getInt("Rain");
            Log.e("service","rainjson:"+isRain);
            if(isRain==0){
                sendNotification("Rain");
            }
        Log.e(TAG,"服务Rain:"+isRain);

        } catch (JSONException e) {

            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkDis(MqttMessage message) {

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(String.valueOf(message));
            dpjLon = jsonObject.getString("Longitude");

            if (Double.parseDouble(new String(dpjLon.substring(0, dpjLon.length() - 5))) != 0) {
                dpjLongitude = Double.parseDouble(new String(dpjLon.substring(0, dpjLon.length() - 5)));
            }
            Log.e("服务经度", String.valueOf(dpjLongitude));
            Log.d("服务经度", new String(String.valueOf(dpjLongitude)));


        } catch (JSONException e) {

            e.printStackTrace();
        }
        try {
            jsonObject = new JSONObject(String.valueOf(message));
            dpjLat = jsonObject.getString("Latitude");
            if (Double.parseDouble(new String(dpjLat.substring(0, dpjLat.length() - 5))) != 0) {
                dpjLatitude = Double.parseDouble(new String(dpjLat.substring(0, dpjLat.length() - 5)));
            }
            Log.d("服务纬度", new String(String.valueOf(dpjLatitude)));


        } catch (JSONException e) {

            e.printStackTrace();
        }
        Log.e("经纬度", dpjLon + dpjLat);
        if (dpjLon != null && dpjLat != null) {
            Log.e("check结束","2111112222222333");
            LatLng myLocation = new LatLng(myLatitude, myLongitude);
            LatLng dpjLocation = new LatLng(dpjLatitude, dpjLongitude);

            // double distance=DistanceUtil. getDistance(myLocation, dpjLocation);
            distance = getDis.GetDistanceOne(myLongitude, myLatitude, dpjLongitude, dpjLatitude);

//            String dis = new String(String.valueOf(distance));
//            DecimalFormat df = new DecimalFormat("0.0000");
            Log.e("check结束","22222222333");
            Log.e("service",String.valueOf(save_distance));

            if (distance*1000> save_distance) {
                if (checkFlag) {
                    sendNotification("Dis");
                    checkFlag = false;
                }
            } else {
                checkFlag = true;
            }

        }
        Log.e("check结束","22222222");
    }

    public void initLocation(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

            }
        });
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationUpdates(location);
    }




}