package com.example.gyx.bluetoothdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final int MESSAGE_READ = 30;
    private static final int BT_REQUEST_CODE=2;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice bluetoothDevice;
    private volatile Thread jiance_thead=null;

    private BluetoothSocket bluetoothSocket;

    private BluetoothServerSocket bluetoothServerSocket;

    private volatile static Long str1=System.currentTimeMillis();

    private ArrayAdapter<String> mArrayAdapter;

    private static int FLAG=0;

    private static String ip="";


    private static TextView zhangtai_text;
    private static String receiverData;

    // static 才能访问 static
    private static volatile BluetoothClient bluetoothClient=null;
    private static volatile BluetoothServer bluetoothServer=null;


    @SuppressLint("HandlerLeak")
    public static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MainActivity.MESSAGE_READ:
                    break;

                case 3:
                    Log.e("dd","连接创建成功");
                    FLAG=msg.getData().getInt("FLAG");
                    String zhangtai=msg.getData().getString("zhuangtai");
                    zhangtai_text.setText(ip+zhangtai);
                    break;
                case 4:
                    receiverData=msg.getData().getString("receiverData");
                    if (receiverData==null){
                        Toast.makeText(MyApplication.getContext(), "接收数据为空", Toast.LENGTH_SHORT).show();
                    }else {
                        Log.e("receiver_MAIN", receiverData);
                        if (receiverData.equals("55AAAA55")) {
                            bluetoothServer.sendData("55BBBB55");
                            Log.e("bluetoothServer", "心跳包回应完成");
                        } else {
                            // 静态方法获取context
                            Toast.makeText(MyApplication.getContext(), "接收数据为："+receiverData, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case 2:
                    ip=msg.getData().getString("ip");
                    break;
                case 1 :
                    str1 = msg.getData().getLong("time");
                    Log.e("time","获取当前时间："+str1);
                    break;

                default:break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int n=0;
                for (int i = 0; i < mArrayAdapter.getCount() - 1; i++) {
                    if(mArrayAdapter.getItem(i).equals(device.getName() + "\n" + device.getAddress())){
                        n=1;
                        break;
                    }
                }
                if (n==0){
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }

            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.e("dd","搜索结束!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "搜索结束!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        zhangtai_text=findViewById(R.id.zhuangtai);
        // 请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, BT_REQUEST_CODE);
            } else {
                init();
            }
        } else {
        init();
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //通过requestCode来识别是否同一个请求
        if (requestCode == BT_REQUEST_CODE) {
            Log.e("ee", "蓝牙权限请求回调");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Log.e("ee", "用户不同意，向用户展示该权限作用");
                showWaringDialog();
            }
        }

    }

    private void init() {
        // 初始化 listview ：设置数据源，adapter
        // 数据源改变 可以通过调用adapter的notify方法进行动态加载
        // 自定义的adapter：homeAdapter = new HomeAdapter(R.layout.home_item_view, mDataList);
        // ArrayAdapter: 可以直接add，或者更改数据。
        // 还有simpleadapter，SimpleCursorAdapter涉及到数据库
        mArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.peidui_devices);
        listView.setAdapter(mArrayAdapter);

        Button scan = findViewById(R.id.scan);
        Button open = findViewById(R.id.open);
        Button cancel=findViewById(R.id.cancel);
        Button fuwuqiopen=findViewById(R.id.fuwuqiopen);

        // 这将返回一个表示设备自身的蓝牙适配器（蓝牙无线装置）的 BluetoothAdapter。
        // 整个系统有一个蓝牙适配器，并且您的应用可使用此对象与之交互。
        // 如果 getDefaultAdapter() 返回 null，则该设备不支持蓝牙，您的操作到此为止
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // 下一步，您需要确保已启用蓝牙。调用 isEnabled() 以检查当前是否已启用蓝牙。
        // 如果此方法返回 false，则表示蓝牙处于停用状态。
        // 要请求启用蓝牙，请使用 ACTION_REQUEST_ENABLE 操作 Intent 调用 startActivityForResult()。这将通过系统设置发出启用蓝牙的请求（无需停止您的应用）。
        // 这个是一个异步的操作
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // 如图 1 所示，将显示对话框，请求用户允许启用蓝牙。如果用户响应“Yes”，系统将开始启用蓝牙，并在该进程完成（或失败）后将焦点返回到您的应用。
            // 传递给 startActivityForResult() 的 REQUEST_ENABLE_BT 常量是在局部定义的整型（必须大于 0），系统会将其作为 requestCode 参数传递回您的 onActivityResult() 实现。
            // 如果成功启用蓝牙，您的 Activity 将会在 onActivityResult() 回调中收到 RESULT_OK 结果代码。 如果由于某个错误（或用户响应“No”）而没有启用蓝牙，则结果代码为 RESULT_CANCELED。
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            bluetoothServer=new BluetoothServer(mBluetoothAdapter);
            Toast.makeText(MainActivity.this,"蓝牙已经开启",Toast.LENGTH_SHORT).show();
        }







        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothClient == null) {
                    Toast.makeText(MainActivity.this,"你还没有连接一个服务器",Toast.LENGTH_SHORT).show();
                } else {
                    // 发送数据
                    String b = "55 AA 66 AA 77 AA";
                    bluetoothClient.sendData(b);
                }
            }

        });
        fuwuqiopen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 发送数据
                if (bluetoothServer==null){
                    Toast.makeText(MainActivity.this,"你还没有一个客户端连接你",Toast.LENGTH_SHORT).show();
                }else {
                    String b = "55 BB 66 BB 77 BB";
                    bluetoothServer.sendData(b);
                }
            }
        });


        // 注册广播，用来当执行：搜索设备操作，搜索结束操作 之后的逻辑处理
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 搜索操作
                mArrayAdapter.clear();

                // 获取连接过的设备：已经配对过的设备
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
                if (mBluetoothAdapter.startDiscovery()) {
                    // 开启搜索操作，在我们注册的广播接收器中可以直接 进行操作
                    Toast.makeText(MainActivity.this, "成功开启扫描", Toast.LENGTH_LONG).show();
                }
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemstr = ((TextView) view).getText().toString();


                //连接新设备
                //连接新的设备时,断开当前连接
                //当前连接分为 有连接和无连接
                //创建新连接，就是new一个Blueclient，每一个blueclient会自动创建一个Blueclientthread。
                //当我们执行stop方法，当前blueclient也就没用了
                final String mac = itemstr.substring(itemstr.length() - 17, itemstr.length());
                Toast.makeText(MainActivity.this, mac, Toast.LENGTH_LONG).show();
                new Thread() {
                    public void run() {
                        Log.e("dd", "断开当前连接 ");
                        if (bluetoothClient!=null) {
                            bluetoothClient.stopClient();
                        }
                        if (jiance_thead!=null){
                            jiance_thead.interrupt();
                        }

                        // 要在两台设备上的应用之间创建连接，必须同时实现服务器端和客户端机制，
                        // 因为其中一台设备必须开放服务器套接字，而另一台设备必须发起连接（使用服务器设备的 MAC 地址发起连接）。
                        // 当服务器和客户端在同一 RFCOMM 通道上分别拥有已连接的 BluetoothSocket 时，二者将被视为彼此连接。
                        // 这种情况下，每台设备都能获得输入和输出流式传输，并且可以开始传输数据，

                        // AcceptThread acceptThread=new AcceptThread(); // 实现服务端的线程
                        // acceptThread.start();
                        try {
                            ip=mac;

                            bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);
                            bluetoothClient=new BluetoothClient(bluetoothDevice,mBluetoothAdapter);
                            jiance_thead=new Thread() {
                                public void run() {
                                    final Thread currentThread = Thread.currentThread();
                                    currentThread.setName("bt-lastReceiveTime");
                                    try {
                                        while (true) {
                                            long curr = str1;
                                            Log.e("ddddddddddsadfs", String.valueOf(curr));
                                            Thread.sleep(10000);
                                            if (curr == str1) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                Log.e("ddd", "用户掉线了。。。即将重连。。。");
                                                bluetoothClient.stopSocket();
                                            } else {

                                                Message msg = new Message();
                                                msg.what = 3;
                                                Bundle bundle = new Bundle();
                                                bundle.putString("zhuangtai", "当前连接状态：正 常      ");
                                                bundle.putInt("APP_FLAG", 1); // 可进行发数据，关闭当前蓝牙操作
                                                msg.setData(bundle);
                                                MainActivity.handler.sendMessage(msg);

                                                Log.e("ddd", "用户连接正常。。。。");
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                            Log.e("dd","InterruptedExceptionInterruptedException");
                                            e.printStackTrace();
                                    }

                                }
                            };
                            jiance_thead.start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
            }

        });


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 关闭蓝牙客户端线程
                if (jiance_thead!=null){
                    Toast.makeText(MainActivity.this,"关闭检测线程，不用断线重连",Toast.LENGTH_SHORT).show();
                    jiance_thead.interrupt();
                }
                if (bluetoothClient==null){
                    Toast.makeText(MainActivity.this,"蓝牙客户端还未创建",Toast.LENGTH_SHORT).show();
                }else {
                    bluetoothClient.stopClient();
                    Toast.makeText(MainActivity.this,"蓝牙客户端线程关闭",Toast.LENGTH_SHORT).show();
                    bluetoothClient=null;
                }

            }
        });

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (mBluetoothAdapter.isEnabled()) {

                Log.e("ee", "蓝牙请求打开回掉");

                // 在执行设备发现之前，有必要查询已配对的设备集，以了解所需的设备是否处于已知状态。
                // 为此，请调用 getBondedDevices()。 这将返回表示已配对设备的一组 BluetoothDevice。
                // 例如，您可以查询所有已配对设备，然后使用 ArrayAdapter 向用户显示每台设备的名称：
                // false 不能获取
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }

                bluetoothServer=new BluetoothServer(mBluetoothAdapter);
            } else {
                finish();
            }
        }
    }


    private void showWaringDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("警告！")
                .setMessage("请前往设置->应用->PermissionDemo->权限中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }



















    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播
        unregisterReceiver(mReceiver);
        //关闭蓝牙
        mBluetoothAdapter.disable();
    }

}



// UUID
// 蓝牙串口服务
// SerialPortServiceClass_UUID = '{00001101-0000-1000-8000-00805F9B34FB}'
// LANAccessUsingPPPServiceClass_UUID = '{00001102-0000-1000-8000-00805F9B34FB}'
// 拨号网络服务
// DialupNetworkingServiceClass_UUID = '{00001103-0000-1000-8000-00805F9B34FB}'
// 信息同步服务
// IrMCSyncServiceClass_UUID = '{00001104-0000-1000-8000-00805F9B34FB}'
// SDP_OBEXObjectPushServiceClass_UUID = '{00001105-0000-1000-8000-00805F9B34FB}'
// 文件传输服务
// OBEXFileTransferServiceClass_UUID = '{00001106-0000-1000-8000-00805F9B34FB}'
// IrMCSyncCommandServiceClass_UUID = '{00001107-0000-1000-8000-00805F9B34FB}'
// SDP_HeadsetServiceClass_UUID = '{00001108-0000-1000-8000-00805F9B34FB}'
// CordlessTelephonyServiceClass_UUID = '{00001109-0000-1000-8000-00805F9B34FB}'
// SDP_AudioSourceServiceClass_UUID = '{0000110A-0000-1000-8000-00805F9B34FB}'
// SDP_AudioSinkServiceClass_UUID = '{0000110B-0000-1000-8000-00805F9B34FB}'
// SDP_AVRemoteControlTargetServiceClass_UUID = '{0000110C-0000-1000-8000-00805F9B34FB}'
// SDP_AdvancedAudioDistributionServiceClass_UUID = '{0000110D-0000-1000-8000-00805F9B34FB}'
// SDP_AVRemoteControlServiceClass_UUID = '{0000110E-0000-1000-8000-00805F9B34FB}'
// VideoConferencingServiceClass_UUID = '{0000110F-0000-1000-8000-00805F9B34FB}'
// IntercomServiceClass_UUID = '{00001110-0000-1000-8000-00805F9B34FB}'
// 蓝牙传真服务
// FaxServiceClass_UUID = '{00001111-0000-1000-8000-00805F9B34FB}'
// HeadsetAudioGatewayServiceClass_UUID = '{00001112-0000-1000-8000-00805F9B34FB}'
// WAPServiceClass_UUID = '{00001113-0000-1000-8000-00805F9B34FB}'
// WAPClientServiceClass_UUID = '{00001114-0000-1000-8000-00805F9B34FB}'
// 个人局域网服务
// ANUServiceClass_UUID = '{00001115-0000-1000-8000-00805F9B34FB}'
// 个人局域网服务
// NAPServiceClass_UUID = '{00001116-0000-1000-8000-00805F9B34FB}'
// 个人局域网服务
// GNServiceClass_UUID = '{00001117-0000-1000-8000-00805F9B34FB}'

//    DirectPrintingServiceClass_UUID = '{00001118-0000-1000-8000-00805F9B34FB}'
//
//    ReferencePrintingServiceClass_UUID = '{00001119-0000-1000-8000-00805F9B34FB}'
//
//    ImagingServiceClass_UUID = '{0000111A-0000-1000-8000-00805F9B34FB}'
//
//    ImagingResponderServiceClass_UUID = '{0000111B-0000-1000-8000-00805F9B34FB}'
//
//    ImagingAutomaticArchiveServiceClass_UUID = '{0000111C-0000-1000-8000-00805F9B34FB}'
//
//    ImagingReferenceObjectsServiceClass_UUID = '{0000111D-0000-1000-8000-00805F9B34FB}'
//
//    SDP_HandsfreeServiceClass_UUID = '{0000111E-0000-1000-8000-00805F9B34FB}'
//
//    HandsfreeAudioGatewayServiceClass_UUID = '{0000111F-0000-1000-8000-00805F9B34FB}'
//
//    DirectPrintingReferenceObjectsServiceClass_UUID = '{00001120-0000-1000-8000-00805F9B34FB}'
//
//    ReflectedUIServiceClass_UUID = '{00001121-0000-1000-8000-00805F9B34FB}'
//
//    BasicPringingServiceClass_UUID = '{00001122-0000-1000-8000-00805F9B34FB}'
//
//    PrintingStatusServiceClass_UUID = '{00001123-0000-1000-8000-00805F9B34FB}'
//#人机输入服务
//
//    HumanInterfaceDeviceServiceClass_UUID = '{00001124-0000-1000-8000-00805F9B34FB}'
//
//
//
//    HardcopyCableReplacementServiceClass_UUID = '{00001125-0000-1000-8000-00805F9B34FB}'
//
//
//
//            #蓝牙打印服务
//
//            HCRPrintServiceClass_UUID = '{00001126-0000-1000-8000-00805F9B34FB}'
//
//
//
//    HCRScanServiceClass_UUID = '{00001127-0000-1000-8000-00805F9B34FB}'
//
//    CommonISDNAccessServiceClass_UUID = '{00001128-0000-1000-8000-00805F9B34FB}'
//
//    VideoConferencingGWServiceClass_UUID = '{00001129-0000-1000-8000-00805F9B34FB}'
//
//    UDIMTServiceClass_UUID = '{0000112A-0000-1000-8000-00805F9B34FB}'
//
//    UDITAServiceClass_UUID = '{0000112B-0000-1000-8000-00805F9B34FB}'
//
//    AudioVideoServiceClass_UUID = '{0000112C-0000-1000-8000-00805F9B34FB}'
//
//    SIMAccessServiceClass_UUID = '{0000112D-0000-1000-8000-00805F9B34FB}'
//
//    PnPInformationServiceClass_UUID = '{00001200-0000-1000-8000-00805F9B34FB}'
//
//    GenericNetworkingServiceClass_UUID = '{00001201-0000-1000-8000-00805F9B34FB}'
//
//    GenericFileTransferServiceClass_UUID = '{00001202-0000-1000-8000-00805F9B34FB}'
//
//    GenericAudioServiceClass_UUID = '{00001203-0000-1000-8000-00805F9B34FB}'
//
//    GenericTelephonyServiceClass_UUID = '{00001204-0000-1000-8000-00805F9B34FB}'