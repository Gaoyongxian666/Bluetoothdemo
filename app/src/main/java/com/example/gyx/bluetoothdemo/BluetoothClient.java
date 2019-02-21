package com.example.gyx.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class BluetoothClient {
    private volatile Thread Processing_001=null;
    private volatile Thread Processing_002=null;

    private static final String TAG = BluetoothClient.class.getSimpleName();

    private volatile BluetoothClientThread bluetoothClientThread;

    public BluetoothClient(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter) throws InterruptedException {
        bluetoothClientThread = new BluetoothClientThread(bluetoothDevice,bluetoothAdapter);
        bluetoothClientThread.start();
        bluetoothClientThread.join();
    }

    public <T> void sendData(T data) {
        String s = (String) data;
        if (TextUtils.isEmpty(s)) {
            Log.e(TAG, "sendData: 消息不能为空");
            return;
        }
        if (bluetoothClientThread != null) {
            bluetoothClientThread.sendMsg(s);
        }else {
            Log.i(TAG, "发送消息失败，socketClientThread为空");
        }
    }

    // 关闭当前连接，并且要求重连
    // 用来处理掉线重连，用来保护当前连接
    public synchronized void stopSocket() {
        Processing_001 = new Thread() {
            @Override
            public void run() {
                final Thread currentThread = Thread.currentThread();
                currentThread.setName("stopsocket");
                Log.e(TAG, "关闭连接并且要求可以重连");
                bluetoothClientThread.setReConnect(true);
                try {
                    bluetoothClientThread.stopThread();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Processing_001.start();
    }

    // 关闭当前连接，并且不重新连接
    // 用来关闭当前连接，开启一个面向新设备服务器端的连接
    public synchronized void stopClient() {
        Processing_002 = new Thread() {
            @Override
            public void run() {
                final Thread currentThread = Thread.currentThread();
                currentThread.setName("stopclient");
                Log.e(TAG, "关闭客户端");
                // 在关闭之前设置不要重连，否则会重连
                bluetoothClientThread.setReConnect(false);
                try {
                    bluetoothClientThread.stopThread();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Processing_002.start();
    }


}
