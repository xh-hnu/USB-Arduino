package com.example.usbarduino;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.example.usbarduino.message.NewDeviceMsg;
import com.example.usbarduino.message.RemoveDevicemsg;

import org.greenrobot.eventbus.EventBus;

class USBReceiver extends BroadcastReceiver {

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            // 获取权限结果的广播
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    //call method to set up device communication
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.e("USBReceiver", "获取权限成功：" + device.getDeviceName());

                    } else {
                        Log.e("USBReceiver", "获取权限失败：" + device.getDeviceName());
                    }
                }
            }
        }else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // 有新的设备插入了，在这里一般会判断这个设备是不是我们想要的，是的话就去请求权限
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            NewDeviceMsg msg = new NewDeviceMsg(device);
            EventBus.getDefault().post(msg);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // 有设备拔出了
            RemoveDevicemsg msg = new RemoveDevicemsg("device remove");
            EventBus.getDefault().post(msg);
        }

    }
}
