package com.example.mymqtttest;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {
    private static String TAG = "SettingActivity";
    private EditText ipEdit;
    private EditText portEdit;
    private EditText led_topicEdit;
    private EditText pos_topicEdit;
    private EditText clientIdEdit;

    private SPHelper spHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        spHelper = SPHelper.getInstant(getApplicationContext());
        initView();
        initViewData();
        registerListener();
    }

    private void initView() {
        ipEdit = findViewById(R.id.ip);
        portEdit = findViewById(R.id.port);
        led_topicEdit = findViewById(R.id.led_topic);
        pos_topicEdit = findViewById(R.id.pos_topic);
        clientIdEdit =findViewById(R.id.clientId);
    }

    protected void initViewData() {
        ipEdit.setText(spHelper.getStringFromSPDef(getApplicationContext(), Constant.SETTING_PLATFORM_ADDRESS, Constant.IP_DEFAULT_VALUE));
        portEdit.setText(spHelper.getStringFromSPDef(getApplicationContext(), Constant.SETTING_PORT, Constant.PORT_DEFAULT_VALUE));
        led_topicEdit.setText(spHelper.getStringFromSP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE));
        pos_topicEdit.setText(spHelper.getStringFromSP(getApplicationContext(), Constant.POS_TOPIC_DEFAULT_VALVE));

        clientIdEdit.setText(spHelper.getStringFromSP(getApplicationContext(),Constant.CLIENT_ID_DEFAULT_VALUE));
    }

    protected void registerListener() {
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSetting();
            }
        });

        findViewById(R.id.cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void saveSetting() {
        String ipAddress = ipEdit.getText().toString().trim();
        String port = portEdit.getText().toString().trim();
        String led_topic=led_topicEdit.getText().toString().trim();
        String pos_topic=pos_topicEdit.getText().toString().trim();

        String clientId = clientIdEdit.getText().toString().trim();

        if ("".equals(ipAddress)) {
            Toast.makeText(getApplicationContext(), "请填写IP地址", Toast.LENGTH_LONG).show();
            return;
        }
        if ("".equals(port)) {
            Toast.makeText(getApplicationContext(), "请填写平台端口", Toast.LENGTH_LONG).show();
            return;
        }
        if ("".equals(led_topic)) {
            Toast.makeText(getApplicationContext(), "请填写led主题", Toast.LENGTH_LONG).show();
            return;
        }
        if ("".equals(pos_topic)) {
            Toast.makeText(getApplicationContext(), "请填写定位主题", Toast.LENGTH_LONG).show();
            return;
        }
        if ("".equals(clientId)) {
            Toast.makeText(getApplicationContext(), "请填写设备ID", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(ipAddress) && !TextUtils.isEmpty(port)) {
            DataCache.updateBaseUrl(getApplicationContext(), "tcp://" + ipAddress + ":" + port);
        }

        spHelper.putData2SP(getApplicationContext(), Constant.SETTING_PLATFORM_ADDRESS, ipAddress);
        spHelper.putData2SP(getApplicationContext(), Constant.SETTING_PORT, port);
        spHelper.putData2SP(getApplicationContext(), Constant.LED_TOPIC_DEFAULT_VALVE, led_topic);
        spHelper.putData2SP(getApplicationContext(), Constant.POS_TOPIC_DEFAULT_VALVE, pos_topic);
        spHelper.putData2SP(getApplicationContext(), Constant.CLIENT_ID_DEFAULT_VALUE, clientId);

        Toast.makeText(getApplicationContext(), "保存成功,请重启应用", Toast.LENGTH_SHORT).show();
        this.setResult(2);
        finish();
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
