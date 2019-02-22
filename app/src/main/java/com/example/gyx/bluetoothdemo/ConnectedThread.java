package com.example.gyx.bluetoothdemo;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public String getmac(){
        return mmSocket.getRemoteDevice().getAddress();
    }
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                if (bytes != -1) {
                    String receiverData = bytes2HexString(subBytes(buffer, 0, bytes));

                    Message msg1 = new Message();
                    msg1.what = 4;
                    Bundle bundle1 = new Bundle();
                    bundle1.putString("receiverData", receiverData);  //往Bundle中存放数据
                    bundle1.putString("mac", mmSocket.getRemoteDevice().getAddress());  //往Bundle中存放数据
                    msg1.setData(bundle1);//mes利用Bundle传递数据
                    MainActivity.handler.sendMessage(msg1);//用activity中的handler发送消息

                    Log.e("receiverData", "receiverData是" + receiverData);

                }

            } catch (IOException e) {
                Log.e("dd","用户取消连接");
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String mac,String data) {
        if (mac.equals(mmSocket.getRemoteDevice().getAddress())){
            try {
                byte[] bstream=hexStrToBinaryStr(data);
                mmOutStream.write(bstream);
            } catch (IOException e) { }
        }
    }
    /* Call this from the main activity to send data to the remote device */
    public void write(String data) {
        try {
                byte[] bstream=hexStrToBinaryStr(data);
                mmOutStream.write(bstream);
        } catch (IOException e) { }

    }


    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
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

}