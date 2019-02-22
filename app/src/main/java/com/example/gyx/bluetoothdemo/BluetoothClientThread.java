package com.example.gyx.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.UUID;

import javax.net.SocketFactory;

public class BluetoothClientThread extends Thread{
    private static final String TAG = BluetoothClientThread.class.getSimpleName();

    // 用来判断我们的心跳线程是否开启
    private boolean isLongConnection = true;

    private boolean isReConnect = true;

    private BluetoothSocketSendThread bluetoothSocketSendThread;
    private BluetoothSocketReceiveThread bluetoothSocketReceiveThread;
    private BluetoothSocketHeartBeatThread bluetoothSocketHeartBeatThread;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;

    private BluetoothDevice device;

    private BluetoothAdapter bluetoothAdapter;


    public BluetoothClientThread(BluetoothDevice device, BluetoothAdapter mBluetoothAdapter) {
        this.device=device;
        this.bluetoothAdapter=mBluetoothAdapter;
    }

    public void run() {
        final Thread currentThread = Thread.currentThread();
        currentThread.setName("BluetoothClientThread");
        Log.e(TAG, "Processing-BluetoothClientThread");
        try {
            initSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initSocket() throws IOException {
        // UUID 可以自定义这样就不会连接到别人的设备上
        // 与服务器端的UUID保持一定
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        // 开启连接 目标设备
        // 获取目标socket
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
        Log.e("dd", "获取目标设备的socket成功");
        bluetoothAdapter.cancelDiscovery();
        // 开启连接 好像默认12秒
        // 由目标端服务器设置超时时间
        bluetoothSocket.connect();
        Log.e("dd", "与目标端连接成功");

        connectedThread=new ConnectedThread(bluetoothSocket);
        connectedThread.start();

        OutputStream outputStream = bluetoothSocket.getOutputStream();

        if (isLongConnection) {
            bluetoothSocketHeartBeatThread = new BluetoothSocketHeartBeatThread("BluetoothSocketHeartBeatThread",
                    outputStream, bluetoothSocket);
            bluetoothSocketHeartBeatThread.start();
        }

        Message msg = new Message();
        msg.what = 2;
        Bundle bundle = new Bundle();
        bundle.putString("zhuangtai", "当前连接状态：正 常      ");  //往Bundle中存放数据
        bundle.putInt("FLAG", 1);  //往Bundle中存放数据
        msg.setData(bundle);//mes利用Bundle传递数据
        MainActivity.handler.sendMessage(msg);//用activity中的handler发送消息
    }


    public void sendMsg(String data) {
        if (connectedThread != null) {
            connectedThread.write(data);
        }
    }


    public synchronized void stopThread() throws IOException {
        connectedThread.cancel();
        closeHeartBeatTask();


        if (isReConnect) {
            isReConnect=false;

            Message msg = new Message();
            msg.what = 2;
            Bundle bundle = new Bundle();
            bundle.putString("zhuangtai","当前连接状态：重连中      ");
            bundle.putInt("FLAG",0);
            msg.setData(bundle);
            MainActivity.handler.sendMessage(msg);
            initSocket();
        }
    }

    private void closeHeartBeatTask() throws IOException {
        if (bluetoothSocketHeartBeatThread != null) {
            bluetoothSocketHeartBeatThread.close();
        }
    }

    public void setReConnect(boolean reConnect) {
        isReConnect = reConnect;
    }

}
