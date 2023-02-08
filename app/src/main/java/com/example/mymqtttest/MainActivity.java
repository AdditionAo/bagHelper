package com.example.mymqtttest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.baidu.mapapi.map.SupportMapFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements IGetMessageCallBack,View.OnClickListener {
    FragmentManager mFragmentManager;
    SupportMapFragment mMapFragment;
    GetDis getDis =new GetDis();
    private TextView textView;
    private TextView dpj_altitude;
    private  TextView dpj_speed;
    private  TextView dpj_dis;
    private TextView tv_temperature;
    private  TextView tv_humidity;
    private MediaPlayer music_rain;
    private MediaPlayer music_dis;
    private Button btn_navigate;
    private boolean isRainDialog=false;
    private boolean isDisDialog=false;
    private int isRain;
    private double longitude1,latitude1,longitude2,latitude2;
    private String speed;
    private String altitude;
    private String lon2 = null,lat2=null;
    private TextView dpj;
    private SPHelper spHelper;
    private EditText et_saveDis;
    private Button button_find;
    private Button button_map;
    private Boolean isFinding=false;//当前是否正在一键查找状态
    private ImageButton img_btn_lock;
    private double  save_distance=200;
    private String temperature;
    private String humidity;
    private TextView isMiss;
    private boolean soundFlag=true;
    private MyServiceConnection serviceConnection;
    private MQTTService mqttService;
    private Notification notification;
    private Button btn_hide_top;
    private Button btn_hide_bottom;
    private boolean isLocked=false;

    StringBuilder stringBuilder=new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider
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
                MQTTService.publish("{ \"msg\": \"lock/on\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));

            }
        });


        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        textView = (TextView) findViewById(R.id.text);
        dpj_altitude = findViewById(R.id.dpj_altitude);
        dpj_speed=findViewById(R.id.dpj_speed);
        dpj_dis=findViewById(R.id.dpj_dis);
        locationUpdates(location);
        isMiss = findViewById(R.id.isMiss);
        btn_navigate =findViewById(R.id.button_daohang);
        dpj = findViewById(R.id.dpj);
        serviceConnection = new MyServiceConnection();
        button_find=(Button)findViewById(R.id.button_find);
        button_map=(Button)findViewById(R.id.button_map);
        tv_humidity=findViewById(R.id.humidity);
        tv_temperature = findViewById(R.id.temperature);
        et_saveDis = findViewById(R.id.et_distance);
        serviceConnection.setIGetMessageCallBack(MainActivity.this);
        spHelper=SPHelper.getInstant(this);
        img_btn_lock =findViewById(R.id.imgbtn_lock);
        btn_hide_top=findViewById(R.id.btn_hide_top);
        btn_hide_bottom=findViewById(R.id.btn_hide_bottom);
//        btn_lock.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MQTTService.publish("{ \"msg\": \"lock/on\" }",spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
//            }
//        });
        img_btn_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLocked==true) {
                    MQTTService.publish("{ \"msg\": \"lock/off\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                    img_btn_lock.setImageResource(R.mipmap.off);
                    isLocked=false;
                }else {
                    MQTTService.publish("{ \"msg\": \"lock/on\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                    img_btn_lock.setImageResource(R.mipmap.on);
                    isLocked=true;
                }
            }
        });

        btn_hide_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    MQTTService.publish("{ \"msg\": \"beep/on\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                    isFinding = true;
                    disDialog();
            }
        });

        btn_hide_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MQTTService.publish("{ \"msg\": \"beep/off\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                isFinding=false;
            }
        });


        Intent intent = new Intent(this, MQTTService.class);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        /*********发送距离**********/
        button_find.setOnClickListener(this::onClick);
        /********显示地图 传坐标**************/
        button_map.setOnClickListener(this::onClick);
        /********导航*************/
        btn_navigate.setOnClickListener(this::onClick);
        MQTTService.setMyTopic(spHelper.getStringFromSP(getApplicationContext(),Constant.POS_TOPIC_DEFAULT_VALVE));

    }
    public void locationUpdates(Location location){
        if(location!=null){
            stringBuilder.append("经度:");
            longitude1=location.getLongitude();
            latitude1=location.getLatitude();
            stringBuilder.append(location.getLongitude()+" E");
            stringBuilder.append("\n纬度:");
            stringBuilder.append(location.getLatitude()+" N");
            textView.setText(stringBuilder.toString());
        }else{
            textView.setText("没有获取到信息");
            Log.d("没有位置信息","1212112");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void setMessage(String message) {
        Log.e("main收到信",message);
        Log.e("mainmessage", message);
        JSONObject jsonObject= null;

        try {
            jsonObject = new JSONObject(String.valueOf(message));
            String lock_status = jsonObject.getString("Lock");
            if (lock_status.equals("1")){
                isLocked=false;
                img_btn_lock.setImageResource(R.mipmap.off);
            }else if(lock_status.equals("0")){
                isLocked=true;
                img_btn_lock.setImageResource(R.mipmap.on);
            }
        } catch (JSONException e) {
            Log.e("锁","锁");
            e.printStackTrace();
        }

        try {
            jsonObject = new JSONObject(String.valueOf(message));
            humidity = jsonObject.getString("Humidity");
            tv_humidity.setText(""+String.valueOf(humidity));
        } catch (JSONException e) {
            Log.e("湿度","湿度");
            e.printStackTrace();
        }

        try {
            jsonObject = new JSONObject(String.valueOf(message));
            temperature = jsonObject.getString("Temperature");
            tv_temperature.setText(""+String.valueOf(temperature)+"℃");
        } catch (JSONException e) {
            Log.e("温度","温度");
            e.printStackTrace();
        }


        try {
            jsonObject = new JSONObject(String.valueOf(message));
            speed = jsonObject.getString("Speed");
            dpj_speed.setText(""+String.valueOf(speed));
        } catch (JSONException e) {
            Log.e("速度","速度");
            e.printStackTrace();
        }


        try {
            jsonObject = new JSONObject(String.valueOf(message));
            altitude = jsonObject.getString("Altitude");
            dpj_altitude.setText(""+String.valueOf(altitude));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            jsonObject=new JSONObject(String.valueOf(message));
            lon2=jsonObject.getString("Longitude");

            if(Double.parseDouble(new String(lon2.substring(0,lon2.length()-5)))!=0) {
                longitude2 = Double.parseDouble(new String(lon2.substring(0, lon2.length() - 5)));
            }
            Log.e("物品的经度",String.valueOf(longitude2));
            Log.d("物品的经度",new String(String.valueOf(longitude2)));


        } catch (JSONException e) {

            e.printStackTrace();
        }
        try {
            jsonObject=new JSONObject(String.valueOf(message));
            lat2=jsonObject.getString("Latitude");
            if(  Double.parseDouble(new String(lat2.substring(0,lat2.length()-5)))!=0) {
                latitude2 = Double.parseDouble(new String(lat2.substring(0, lat2.length() - 5)));
            }
            Log.d("物品的纬度",new String(String.valueOf(latitude2)));


        } catch (JSONException e) {

            e.printStackTrace();
        }
        Log.e("main物品的经纬度",lon2+lat2);
        if(lon2!=null&&lat2!=null){
            double distance=getDis.GetDistanceOne(longitude1,latitude1,longitude2,latitude2);
            String dis=new String(String.valueOf(distance));
            DecimalFormat df = new DecimalFormat( "0.0000");
            Log.e("main dpj dis",dis);
            dpj.setText(""+"\n"+"经度:"+longitude2+" E"+"\n"+"纬度:"+latitude2+" N");

//            if(distance*1000>save_distance){
//                if(soundFlag){
//                    isMiss.setVisibility(View.VISIBLE);
//
//                    soundFlag=false;
//                }
//            }else{
//                isMiss.setVisibility(View.GONE);
//                soundFlag=true;
//            }
            if(distance*1000>save_distance){
                if(isDisDialog==false){
                    isDisDialog=true;
                    disDialog();

                }

                }

            if(distance<1){

                dpj_dis.setText(""+df.format(distance*1000)+"m");
            }else{

                dpj_dis.setText(""+df.format(distance)+"km");
            }
            String dis1=new String(String.valueOf(getDis.GetDistanceOne(longitude1,latitude1,longitude2,latitude2)));
            MQTTService.publish(dis1,spHelper.getStringFromSP(getApplicationContext(),Constant.LED_TOPIC_DEFAULT_VALVE));

//            dpj.setText("物品的的位置："+"\n"+"经度:"+longitude2+" E"+"纬度:"+latitude2+" N");
        }

        try {
            jsonObject=new JSONObject(String.valueOf(message));
            isRain=jsonObject.getInt("Rain");
            Log.e("main","rainjson:"+isRain);
            if(isRain==0){
                if(isRainDialog==false){
                    isRainDialog=true;
                    rainDialog();

                }

            }

        } catch (JSONException e) {
            Log.e("main","jsonrain");
            e.printStackTrace();
        }

        mqttService = serviceConnection.getMqttService();
        mqttService.toCreateNotification(message);

    }
    public void disDialog(){
        Log.e("main", "disdialog");
        AlertDialog.Builder dialog= new AlertDialog.Builder (MainActivity.this);


        dialog.setCancelable(false);
        dialog.setTitle("您的包包已丢失");
        MQTTService.publish("{ \"msg\": \"all/on\" }",spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
        MQTTService.publish("{ \"msg\": \"lock/on\" }", spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
        img_btn_lock.setImageResource(R.mipmap.on);
        isLocked=true;

        dialog.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                music_dis.stop();
            }
        });

        dialog.show();
        music_dis = MediaPlayer.create(this, R.raw.alarm);
        music_dis.setLooping(true);
        if(!music_dis.isPlaying()){
            music_dis.start();
        }

    }
    public void rainDialog(){
        Log.e("main","raindialog");
        AlertDialog.Builder dialog= new AlertDialog.Builder (MainActivity.this);

        dialog.setCancelable(false);
        dialog.setTitle("您的包包已进水");

        dialog.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                music_rain.stop();
            }
        });

        dialog.show();
        music_rain = MediaPlayer.create(this, R.raw.alarm);
        music_rain.setLooping(true);
        if(!music_rain.isPlaying()){
            music_rain.start();
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.button_find:
                if(!isFinding){//不是正在寻找状态
                    MQTTService.publish("{ \"msg\": \"beep/on\" }",spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                    isFinding=!isFinding;
                    button_find.setBackgroundColor(Color.RED);
                }else {
                    MQTTService.publish("{ \"msg\": \"beep/off\" }",spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
                    isFinding=!isFinding;
                    button_find.setBackgroundColor(Color.rgb(23,131,96));
                }
                break;

            case R.id.button_map:

                Bundle bundle = new Bundle();
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                bundle.putDouble("Log1",longitude1);
                bundle.putDouble("Lat1",latitude1);
                bundle.putDouble("Log2",longitude2);
                bundle.putDouble("Lat2",latitude2);
                intent.putExtra("Message", bundle);
                startActivity(intent);
                break;
            case R.id.button_daohang:
                Log.e("main","navigate");
                Intent intent1=new Intent();
                Log.e("打开标记点",""+latitude2+","+longitude2);
                intent1.setData(Uri.parse("baidumap://map/marker?location="+latitude2+","+longitude2+
                        "&title=包包位置&content=包包位置&coord_type=wgs84" +
                        "&traffic=on&src=andr.baidu.openAPIdemo"));
                startActivity(intent1);
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.setting_item:
                showDialog();
                break;
            case R.id.reset_item:
                isRainDialog=false;
                isDisDialog=false;
                MQTTService.setSave_distance(save_distance);
                MQTTService.publish("{ \"msg\": \"reset\" }",spHelper.getStringFromSP(getApplicationContext(),Constant.LED_TOPIC_DEFAULT_VALVE));
                break;
            default:
                break;
        }
        return true;
    }
    private void showDialog(){
        AlertDialog.Builder dialog= new AlertDialog.Builder (MainActivity.this);
        final EditText edit = new EditText(MainActivity.this);
        dialog.setView(edit);
        dialog.setCancelable(true);
        dialog.setTitle("设置报警距离 单位(米)");



        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String dis = String.valueOf(edit.getText());
                save_distance=Double.valueOf(dis);
                Log.e("main356", dis);
                spHelper.putData2SP(getApplicationContext(),"save_distance",dis);
                MQTTService.setSave_distance(save_distance);
                isRainDialog=false;
                isDisDialog=false;
            }
        });



        dialog.show();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("mainonstart", "sssssssssssss");

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("mainonrestart", "sssssssssssss");
        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack(MainActivity.this);
        Intent intent = new Intent(this, MQTTService.class);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("mainonpause", "sssssssssssss");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("mainonstop", "sssssssssssss");
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        Log.e("毁灭main","main");
        super.onDestroy();
    }

}
