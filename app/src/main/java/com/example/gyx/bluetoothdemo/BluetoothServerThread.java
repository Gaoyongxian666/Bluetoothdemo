package com.example.gyx.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/***
 连接为服务端
 服务器设备和客户端设备分别以不同的方法获得需要的 BluetoothSocket。
 服务器将在传入连接被接受时收到套接字。
 客户端将在其打开到服务器的 RFCOMM 通道时收到该套接字。

 一种实现技术是自动将每台设备准备为一个服务器，从而使每台设备开放一个服务器套接字并侦听连接。
 然后任一设备可以发起与另一台设备的连接，并成为客户端。 或者，其中一台设备可显式“托管”连接并按需开放一个服务器套接字，而另一台设备则直接发起连接。

 当您需要连接两台设备时，其中一台设备必须通过保持开放的 BluetoothServerSocket 来充当服务器。
 服务器套接字的用途是侦听传入的连接请求，并在接受一个请求后提供已连接的 BluetoothSocket。
 从 BluetoothServerSocket 获取 BluetoothSocket 后，可以（并且应该）舍弃 BluetoothServerSocket，除非您需要接受更多连接。

 通用唯一标识符 (UUID) 是用于唯一标识信息的字符串 ID 的 128 位标准化格式。
 UUID 的特点是其足够庞大，因此您可以选择任意随机值而不会发生冲突。 在此示例中，它被用于唯一标识应用的蓝牙服务。
 要获取 UUID 以用于您的应用，您可以使用网络上的众多随机 UUID 生成器之一，然后使用 fromString(String) 初始化一个 UUID。
 ***/
class BluetoothServerThread extends Thread {
    private BluetoothSocketSendThread bluetoothSocketSendThread=null;
    private BluetoothServerSocket mmServerSocket=null;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> mac_list=new ArrayList<String>();
    private BluetoothSocketReceiveThread bluetoothSocketReceiveThread;
    private BluetoothSocket lastsocket;



    public BluetoothServerThread(BluetoothAdapter bluetoothAdapter) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        this.bluetoothAdapter=bluetoothAdapter;

        try {
            // 通过调用 listenUsingRfcommWithServiceRecord(String, UUID) 获取 BluetoothServerSocket。
            // 该字符串是您的服务的可识别名称，系统会自动将其写入到设备上的新服务发现协议
            // (SDP) 数据库条目（可使用任意名称，也可直接使用您的应用名称）。 UUID 也包含在 SDP 条目中，并且将作为与客户端设备的连接协议的基础。
            // 也就是说，当客户端尝试连接此设备时，它会携带能够唯一标识其想要连接的服务的 UUID。 两个 UUID 必须匹配，在下一步中，连接才会被接受。
            // MY_UUID is the app's UUID string, also used by the client code
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Mainactivity", uuid);
        } catch (IOException e) {
        }
        mmServerSocket = tmp;
    }

    // 获取本机的bluesocket
    public void run() {
        BluetoothSocket socket;
        Log.e("BluetoothServerSocket", "Processing-BluetoothServerSocket");



        // 在此示例中，只需要一个传入连接，因此在接受连接并获取 BluetoothSocket 之后，应用会立即将获取的 BluetoothSocket 发送到单独的线程，
        // 关闭 BluetoothServerSocket 并中断循环。
        // 请注意，当 accept() 返回 BluetoothSocket 时，表示套接字已连接好，因此您不应该像在客户端那样调用 connect()。
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {

                // 通过调用 accept() 开始侦听连接请求。
                // 这是一个阻塞调用。它将在连接被接受或发生异常时返回。
                // 仅当远程设备发送的连接请求中所包含的 UUID 与向此侦听服务器套接字注册的 UUID 相匹配时，连接才会被接受。
                // 操作成功后，accept() 将会返回已连接的 BluetoothSocket。
                Log.e("dd", " mmServerSocket.accept()");
                socket = mmServerSocket.accept();
                try {
                    stopThread();
                    Log.e("dd", " bluetoothSocketReceiveThread");
                    String address = socket.getRemoteDevice().getAddress();

                    // 接收到一个新连接之后都会再次新开两个线程 接收和发送线程
                    bluetoothSocketReceiveThread = new BluetoothSocketReceiveThread("Receive"+address + "BTReceiveThread",
                             socket.getInputStream());
                    bluetoothSocketSendThread = new BluetoothSocketSendThread("Send"+address + "BTSendThread", socket.getOutputStream());

                } catch (IOException e) {
                        e.printStackTrace();

                }
                bluetoothSocketReceiveThread.start();
                bluetoothSocketSendThread.start();

                // 在完成传入连接的侦听后，通常应立即关闭您的 BluetoothServerSocket。
                // 在此示例中，获取 BluetoothSocket 后立即调用 close()。 您也可能希望在您的线程中提供一个公共方法，
                // 以便在需要停止侦听服务器套接字时关闭私有 BluetoothSocket。
                // 除非您想要接受更多连接，否则请调用 close()。
                // 这将释放服务器套接字及其所有资源，但不会关闭 accept() 所返回的已连接的 BluetoothSocket。
                // 与 TCP/IP 不同，RFCOMM 一次只允许每个通道有一个已连接的客户端，因此大多数情况下，
                // 在接受已连接的套接字后立即在 BluetoothServerSocket 上调用 close() 是行得通的。
                //try {
                //    mmServerSocket.close();
                //    Log.e("dd"," mmServerSocket.close");
                //} catch (IOException e) {
                //   e.printStackTrace();
                //}
                //    break;
                //}
            } catch(IOException e){
                    break;
            }


        }
    }

    public void sendMsg(String data) {
        if (bluetoothSocketSendThread != null) {
            bluetoothSocketSendThread.sendMsg(data);
        }
    }


    /**
     * Will cancel the listening socket, and cause the thread to finish
     */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
        }
    }

    public synchronized void stopThread() throws IOException {
        //关闭接收线程
        closeReceiveTask();
        //唤醒发送线程并关闭
        wakeSendTask();
    }

    private void wakeSendTask() {
        if (bluetoothSocketSendThread != null) {
            bluetoothSocketSendThread.wakeSendTask();
        }
    }


    private void closeReceiveTask() {
        if (bluetoothSocketReceiveThread != null) {
            bluetoothSocketReceiveThread.close();
            bluetoothSocketReceiveThread = null;
        }
    }

}


