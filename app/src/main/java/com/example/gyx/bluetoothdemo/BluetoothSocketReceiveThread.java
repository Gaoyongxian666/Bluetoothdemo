package com.example.gyx.bluetoothdemo;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class BluetoothSocketReceiveThread extends Thread {

    private static final String TAG = BluetoothSocketReceiveThread.class.getSimpleName();
    private volatile long lastReceiveTime = System.currentTimeMillis();
    private volatile String name;
    private volatile boolean isCancel = false;
    private InputStream inputStream;


    public BluetoothSocketReceiveThread(String name, InputStream bufferedReader) {
        this.name = name;
        this.inputStream = bufferedReader;
    }

    public void run() {
        final Thread currentThread = Thread.currentThread();
        currentThread.setName(name);

        Log.e(TAG, "BluetoothSocketReceiveThread已经开启");
        int bytes = 0;
        while (!isCancel) {
            lastReceiveTime = System.currentTimeMillis();
            byte buffer[] = new byte[1024];

            Message msg = new Message();
            msg.what = 1;
            Bundle bundle = new Bundle();
            bundle.putLong("time", lastReceiveTime);  //往Bundle中存放数据
            msg.setData(bundle);//mes利用Bundle传递数据
            MainActivity.handler.sendMessage(msg);//用activity中的handler发送消息

            if (inputStream != null) {
                try {
                    bytes = inputStream.read(buffer);

                } catch (IOException e) {
                    Log.e(TAG,"inputStream读取失败");
                    close();
                    e.printStackTrace();
                }
                if (bytes != -1) {
                    String receiverData = bytes2HexString(subBytes(buffer, 0, bytes));

                    Message msg1 = new Message();
                    msg1.what = 4;
                    Bundle bundle1 = new Bundle();
                    bundle1.putString("receiverData", receiverData);  //往Bundle中存放数据
                    msg1.setData(bundle1);//mes利用Bundle传递数据
                    MainActivity.handler.sendMessage(msg1);//用activity中的handler发送消息

                    Log.e(TAG, "receiverData是" + receiverData);

                }

            }
        }
        if (inputStream != null) {
            synchronized (inputStream) {
                try {
                    inputStream.close();
                    Log.e(TAG,"输入流关闭成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static String bytes2HexString(byte[] b) {
        StringBuffer result = new StringBuffer();
        String hex;
        for (int i = 0; i < b.length; i++) {
            hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            result.append(hex.toUpperCase());
        }
        return result.toString();
    }

    public void close() {
        isCancel = true;
    }

}