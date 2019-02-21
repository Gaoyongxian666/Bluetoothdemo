package com.example.gyx.bluetoothdemo;

import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothSocketSendThread extends Thread {

    private static final String TAG = BluetoothSocketSendThread.class.getSimpleName();
    private volatile String name;
    private volatile boolean isCancel = false;
    private boolean closeSendTask;
    private final OutputStream outputStream;
    private volatile ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<>();
    public BluetoothSocketSendThread(String name, OutputStream printWriter) {
        this.name = name;
        this.outputStream = printWriter;
    }

    @Override
    public void run() {
        final Thread currentThread = Thread.currentThread();
        currentThread.setName(name);
        Log.e(TAG, "SocketSendThread已经开启");
        while (!isCancel) {
            String dataContent = dataQueue.poll();
            if (dataContent == null) {
                //没有发送数据则等待
                Log.e(TAG, "dataContent == null");
                toWait(dataQueue, 0);
                if (closeSendTask) {
                    //notify()调用后，并不是马上就释放对象锁的，所以在此处中断发送线程
                    close();
                }
            } else if (outputStream != null) {
                Log.e(TAG, "dataContent ！= null");
                synchronized (outputStream) {
                    Log.e(TAG, dataContent);
                    try {
                        write2Stream(dataContent, outputStream);
                        Log.e(TAG,"发送数据成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        close();
                    }
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


    public static void toWait(Object o, long millis) {
        synchronized (o) {
            try {
                o.wait(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void toNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }


    public void sendMsg(String data) {
        dataQueue.add(data);
        //有新增待发送数据，则唤醒发送线程
        toNotifyAll(dataQueue);
    }

    public void clearData() {
        dataQueue.clear();
    }

    public void close() {
        isCancel = true;
    }

    public void wakeSendTask() {
        closeSendTask = true;
        toNotifyAll(dataQueue);
    }

    public void setCloseSendTask(boolean closeSendTask) {
        this.closeSendTask = closeSendTask;
    }
}
