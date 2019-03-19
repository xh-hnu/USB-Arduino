package com.example.usbarduino;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class UsbUtil {

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static UsbUtil instance;
    private PendingIntent mPermissionIntent;
    private UsbManager usbManager;
    private Context context;
    private USBReceiver usbReceiver;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpointIn;
    private UsbEndpoint usbEndpointOut;
    private UsbDeviceConnection usbConnection;

    //单例模式
    private UsbUtil() {
    }

    //初始化
    public void init(Application context) {
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbReceiver = new USBReceiver();
    }

    public static UsbUtil getInstance() {
        if (instance == null) {
            synchronized (UsbUtil.class) {
                if (instance == null) {
                    instance = new UsbUtil();
                }
            }
        }
        return instance;
    }

    /**
     *
     * @param vendorId  厂商ID
     * @param productId 产品ID
     * @return device
     */
    UsbDevice getUsbDevice(int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                Log.e("UsbUtil", "getDeviceList: " + device.getDeviceName());
                return device;
            }
        }
        Toast.makeText(context, "没有对应的设备", Toast.LENGTH_SHORT).show();
        return null;
    }

    /**
     * 判断对应 USB 设备是否有权限
     */
    boolean hasPermission(UsbDevice device) {
        return usbManager.hasPermission(device);
    }

    /**
     * 请求获取指定 USB 设备的权限
     */
    void requestPermission(UsbDevice device) {
        if (device != null) {
            if (usbManager.hasPermission(device)) {
                Toast.makeText(context, "已经获取到权限", Toast.LENGTH_SHORT).show();
            } else {
                if (mPermissionIntent != null) {
                    usbManager.requestPermission(device, mPermissionIntent);
                    Toast.makeText(context, "请求USB权限", Toast.LENGTH_SHORT).show();
                } else {
                    registerReceiver(context);
                    usbManager.requestPermission(device, mPermissionIntent);
                    Toast.makeText(context, "请求USB权限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 打开通信端口
     */
    boolean openPort(UsbDevice device) {
        //获取设备接口，一般只有一个，多个的自己研究去
        usbInterface = device.getInterface(0);

        // 判断是否有权限
        if (hasPermission(device)) {
            // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯
            usbConnection = usbManager.openDevice(device);

            if (usbConnection == null) {
                return false;
            }
            if (!usbConnection.claimInterface(usbInterface, true)) {
                usbConnection.close();
                Toast.makeText(context, "没有找到 USB 设备接口", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(context, "没有 USB 权限", Toast.LENGTH_SHORT).show();
            return false;
        }

        //获取接口上的两个端点，分别对应 OUT 和 IN
        for (int i = 0; i < usbInterface.getEndpointCount(); ++i) {
            UsbEndpoint end = usbInterface.getEndpoint(i);
            if (end.getDirection() == UsbConstants.USB_DIR_IN) {
                usbEndpointIn = end;
            } else {
                usbEndpointOut = end;
            }
        }
        return true;
    }

    int sendMessage(byte[] bytes) {
        return usbConnection.bulkTransfer(usbEndpointOut, bytes, bytes.length, 500);
    }

    public void closeport(int timeout) {
        if (usbConnection == null) {
            return;
        }
        try {
            Thread.sleep((long) timeout);
        } catch (InterruptedException var4) {
            var4.printStackTrace();
        }
        try {
            usbConnection.close();
            usbConnection.releaseInterface(usbInterface);
            usbConnection = null;
            usbEndpointIn = null;
            usbEndpointOut = null;
            usbManager = null;
            usbInterface = null;
            Log.d("device status", "Device closed. ");
        } catch (Exception var3) {
            Log.e("device status", "Exception: " + var3.getMessage());
        }
    }

    /**
     * 注册广播
     */
    public void registerReceiver(Context context) {
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        context.registerReceiver(usbReceiver, filter);
    }

    public void unRegisterReceiver(Context context) {
        context.unregisterReceiver(usbReceiver);
        mPermissionIntent = null;
    }

}
