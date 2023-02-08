package com.example.mymqtttest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private EditText userNameEdit;
    private EditText pwdEdit;
    private ImageView background;
    private ImageView mShowpswImageView;
    private Boolean mShowPassword = false;
    private CheckBox rememberPass;
    private int yourChoice;
    private MyServiceConnection serviceConnection;

    private SPHelper spHelper;

    private Button login_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        spHelper = SPHelper.getInstant(getApplicationContext());

        //读写存储空间权限
        if(ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        pref= PreferenceManager.getDefaultSharedPreferences(this);

        userNameEdit = findViewById(R.id.userName);
        pwdEdit = findViewById(R.id.pwd);
        mShowpswImageView=findViewById(R.id.showpsw_imageView);
        mShowpswImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowPassword) {
                    mShowpswImageView.setBackgroundResource(R.mipmap.ico_eye_off);
                    pwdEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    pwdEdit.setSelection(pwdEdit.getText().toString().length());
                    mShowPassword = !mShowPassword;//改变状态值
                } else {
                    mShowpswImageView.setBackgroundResource(R.mipmap.ico_eye_on);
                    pwdEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    pwdEdit.setSelection(pwdEdit.getText().toString().length());
                    mShowPassword = !mShowPassword;
                }
            }
        });
        rememberPass=(CheckBox)findViewById(R.id.remreber_pass);
        login_btn=findViewById(R.id.signIn);
        background=findViewById(R.id.background);
        userNameEdit.setText(DataCache.getUserName(getApplicationContext()));
        pwdEdit.setText(DataCache.getPwd(getApplicationContext()));

        Boolean isRemember=pref.getBoolean("remember_password",false);
        if (isRemember) {
            //将账号密码ip主题客户id都设置到文本框中
            String userName=pref.getString("userName","");
            String pwd=pref.getString("pwd","");
            userNameEdit.setText(userName);
            pwdEdit.setText(pwd);

            rememberPass.setChecked(true);
        }

//        userName.setText("admin");
//        pwd.setText("admin");
//        ip.setText("tcp://192.168.1.132:1883");
//        topic.setText("esp/test");
//        clientId.setText("addition");

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName= userNameEdit.getText().toString();
                String pwd= pwdEdit.getText().toString();

                if(userName.equals("admin") || pwd.equals("admin")){
                    editor=pref.edit();
                    if(rememberPass.isChecked()){//检查复选框是否被选中
                        editor.putBoolean("remember_password",true);
                        editor.putString("userName",userName);
                        editor.putString("pwd",pwd);
                    }else {
                        editor.clear();
                    }
                    editor.apply();
                    signIn();
                }else {
                    Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void signIn() {
        String platformAddress = spHelper.getStringFromSP(getApplicationContext(), Constant.SETTING_PLATFORM_ADDRESS);
        String port = spHelper.getStringFromSP(getApplicationContext(), Constant.SETTING_PORT);
        Log.e("得到",platformAddress);
        Log.e("得到",port);

        final String userName = userNameEdit.getText().toString();
        final String pwd = pwdEdit.getText().toString();
        final String host =DataCache.getBaseUrl(getApplicationContext());
        final String led_topic = spHelper.getStringFromSP(getApplicationContext(),Constant.LED_TOPIC_DEFAULT_VALVE);
        final String pos_topic = spHelper.getStringFromSP(getApplicationContext(),Constant.POS_TOPIC_DEFAULT_VALVE);
        final String clientId = spHelper.getStringFromSP(getApplicationContext(),Constant.CLIENT_ID_DEFAULT_VALUE);

        Log.e("用户名 密码",userName+" "+pwd+" ");
        Log.e("host",host);
        //  Log.e("主题 客户端标识",topic+" "+clientId);

        MQTTService.setUserName(userName);
        MQTTService.setPassWord(pwd);
        MQTTService.setHost(host);
        MQTTService.setMyTopic(pos_topic);
        MQTTService.setClientId(clientId);

        Bundle bundle = new Bundle();
        Intent intent1 = new Intent(LoginActivity.this, MQTTService.class);
        bundle.putString("userName", userName);
        bundle.putString("pwd", pwd);
        bundle.putString("ip", host);
        bundle.putString("topic", pos_topic);
        bundle.putString("clientId",clientId);
        intent1.putExtra("Message", bundle);
        serviceConnection = new MyServiceConnection();
        bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);
        Intent intent_login = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent_login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.setting_item:
                Intent intent=new Intent(LoginActivity.this,SettingActivity.class);
                startActivity(intent);
                break;
            case R.id.topic:
                chooseTopic();
            break;
        }
        return true;
    }
    public void chooseTopic(){

            final String[] items = { "小狗","经典"};
            yourChoice = -1;
            AlertDialog.Builder singleChoiceDialog =
                    new AlertDialog.Builder(LoginActivity.this);
            singleChoiceDialog.setTitle("选择主题");
            // 第二个参数是默认选项，此处设置为0
            singleChoiceDialog.setSingleChoiceItems(items, 0,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            yourChoice = which;
                        }
                    });
            singleChoiceDialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (yourChoice != -1) {
                                if(items[yourChoice]=="小狗"){
                                    background.setBackgroundResource(R.drawable.ct);
                                }else  if(items[yourChoice]=="经典"){
                                    background.setBackgroundResource(R.drawable.address);
                                }
                                Toast.makeText(LoginActivity.this,
                                        "你选择了" + items[yourChoice],
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            singleChoiceDialog.show();



    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // unbindService(serviceConnection);
        super.onDestroy();
    }
}