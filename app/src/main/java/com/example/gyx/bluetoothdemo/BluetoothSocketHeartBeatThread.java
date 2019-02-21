package com.example.gyx.bluetoothdemo;

import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


//"55 AA AA 55" "66 BB BB 66"
public class BluetoothSocketHeartBeatThread extends Thread {

    private static final String TAG = BluetoothSocketHeartBeatThread.class.getSimpleName();
    private volatile String name;
    private static final int REPEAT_TIME = 5000;
    private boolean isCancel = false;
    private final OutputStream outputStream;
    private BluetoothSocket bluetoothSocket;


    public BluetoothSocketHeartBeatThread(String name, OutputStream printWriter,BluetoothSocket mSocket) {
        this.name = name;
        this.outputStream = printWriter;
        this.bluetoothSocket = mSocket;
    }

    public void run() {
        final Thread currentThread = Thread.currentThread();
        currentThread.setName("BluetoothSocketHeartBeatThread");
        Log.e(TAG, "BluetoothSocketHeartBeatThread已经开启");
        while (!isCancel) {
            if (outputStream != null) {
                synchronized (outputStream) {
                    try {
                        write2Stream("55 AA AA 55", outputStream);
                        Log.e(TAG,"心跳包发送成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        close();
                    }
                }
                try {
                    Thread.sleep(REPEAT_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    close();
                }
            }
        }
        if (outputStream != null) {
            synchronized (outputStream) {
                try {
                    outputStream.close();
                    Log.e(TAG,"输出流关闭成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void write2Stream(String data, OutputStream printWriter) throws IOException {
        if (data == null) {
            return;
        }
        if (printWriter != null) {
            byte[] bstream=hexStrToBinaryStr(data);
            Log.e(TAG,data);
            printWriter.write(bstream);
        }
    }

    public static byte[] hexStrToBinaryStr(String hexString) {
        if (TextUtils.isEmpty(hexString)) {
            return null;
        }
        hexString = hexString.replaceAll(" ", "");

        int len = hexString.length();
        int index = 0;


        byte[] bytes = new byte[len / 2];

        while (index < len) {

            String sub = hexString.substring(index, index + 2);

            bytes[index/2] = (byte)Integer.parseInt(sub,16);

            index += 2;
        }


        return bytes;
    }

    public void close()  {
        isCancel = true;
    }

}