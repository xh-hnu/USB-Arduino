package com.example.usbarduino;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.usbarduino.message.NewDeviceMsg;
import com.example.usbarduino.message.RemoveDevicemsg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private UsbDevice mDevice;
    private UsbUtil usbUtil;
    private int venderId = 6790;
    private int produceId = 29987;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbUtil = UsbUtil.getInstance();
        usbUtil.init(getApplication());
        mDevice = usbUtil.getUsbDevice(venderId, produceId);
        EventBus.getDefault().register(this);

        findViewById(R.id.send_data_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDevice != null){
                    checkAndSend();
                }else {
                    mDevice = usbUtil.getUsbDevice(venderId, produceId);
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void newDeviceConnect(NewDeviceMsg message) {

        this.mDevice = message.getDevice();
        if (!usbUtil.hasPermission(mDevice)){
            usbUtil.requestPermission(mDevice);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void removeDevice(RemoveDevicemsg message){
        usbUtil.closeport(500);
        this.mDevice = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    void checkAndSend(){
        if (!usbUtil.hasPermission(mDevice)){
            usbUtil.requestPermission(mDevice);
        }else {
            if (usbUtil.openPort(mDevice)){
                String data = "Hello,Arduino.";
                if (usbUtil.sendMessage(data.getBytes()) > 0){
                    Toast.makeText(MainActivity.this, "发送成功",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
