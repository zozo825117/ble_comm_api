package com.wearlink.ble_uart;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;


import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.apkfuns.logutils.file.LogFileFilter;
import com.wearlink.blecomm.BleCommMethod;
import com.wearlink.blecomm.BleService;
import com.wearlink.blecomm.OnBleCommProgressListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BleComm";
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 2;
    private BleService bleService=null;

    private ServiceConnection connection = null;

    boolean first;

    Button con_btn,discon_btn;
//    Button con_test_btn;
//    Button adv_btn;
    Button set_btn;
    TextView con_addr;
    private List<String> Spinnerlist = new ArrayList<String>();

//    byte[] conn_pass= new byte[]{1,2,3,4,5,6};
    byte[] conn_pass= new byte[]{1,2,3,4,5,6};

    private static String deviceAddr;
    private static String deviceName;
    private static String deviceSetName;
    private static String deviceConAddr;
    private static byte deviceAdvFlag;
    private static int intdeviceErrCode;
    boolean is_adverting = false;
    boolean is_connect = false;


    EditText tx_edit;
    EditText name_edit;
    TextView rx_txt;

    String rx_string;
    String strErrorMsg;
    static int rx_index = 0;

    static int ui_model=0;
    private static final int UI_MODEL_DISCON= 0;
    private static final int UI_MODEL_SCAN_DEVICE= 1;
    private static final int UI_MODEL_CONNED_DEVICE= 2;
    private static final int UI_MODEL_RECIVE_DAT= 3;
    private static final int UI_MODEL_ADV_START = 4;
    private static final int UI_MODEL_ADV_STOP = 5;
    private static final int UI_MODEL_SCAN_DEVICE_TIMEOUT = 6;
    private static final int UI_MODEL_CON_DEVICE_TIMEOUT = 7;
    private static final int UI_MODEL_NAME_SET = 8;
    private static final int UI_MODEL_START_FAILURE = 9;
    private static final int UI_MODEL_SEND_OK = 10;
    private static final int UI_MODEL_SPINNER_SELECT = 10;

    private CheckBox checkbox_hex;

    private Timer mClearAbvInfoTimer = null;

    private BleCommMethod bleCommMethod;

    private class ScanDevice{
        private String mac_address;
        private int time;
        private byte adv_flag;

        ScanDevice(String mac, byte flag){
            this.mac_address = mac;
            this.adv_flag = flag;
            this.time = 0;
        }

        public String getMac(){
            return this.mac_address;
        }
//
//        public void setAdvFlag(byte flag){
//            this.adv_flag = flag;
//        }

        public byte getAdvFlag(){
            return this.adv_flag;
        }

        public void setTimer(int t){
            this.time = t;
        }

        public int getTime(){
            return this.time;
        }

    }

    ArrayList<ScanDevice> ScanDeviceList = new ArrayList<ScanDevice>();

    boolean addMac(String mac, byte flag){
        ScanDevice _scan_device = new ScanDevice(mac,flag);

        if(mClearAbvInfoTimer == null){
            startAdvinfoTimer(8000);
        }

        for(ScanDevice scan_device : ScanDeviceList){
            if(scan_device.getMac().equals(mac)){
                scan_device.setTimer(0);
                return false;
            }
        }
        Spinnerlist.add(mac);
        ScanDeviceList.add(_scan_device);
        return true;
    }

    int removeMac(int interval){
        for(ScanDevice _scan_device:ScanDeviceList){
            if(_scan_device.getTime() > 10*1000){
                Spinnerlist.remove(_scan_device.getMac());
                ScanDeviceList.remove(_scan_device);

                break;
            }else{
                _scan_device.setTimer(_scan_device.getTime() + interval);
            }
        }

        return ScanDeviceList.size();
    }

    private void startAdvinfoTimer(final int con_time_out) {
        mClearAbvInfoTimer = new Timer();
        mClearAbvInfoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
//                deviceAddr = "";
//                deviceName = "";
//                deviceAdvFlag = 0;
//                ui_model = UI_MODEL_SCAN_DEVICE_TIMEOUT;
//                handler.post(mUpdateUI);
                if(removeMac(con_time_out) == 0){

                }
            }
        }, con_time_out,con_time_out/* 表示1000毫秒之後，執行一次 */);
    }

    private void cancelAdvinfoTimer() {
            if(mClearAbvInfoTimer != null) {
                mClearAbvInfoTimer.cancel();
                mClearAbvInfoTimer = null;
            }
    }



    final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // todo
            String strText;
            switch (msg.what){
                case UI_MODEL_SPINNER_SELECT:
                    String url = (String) msg.obj;
                    String[] str_array = url.split(",");

                    String s = "已选择 MAC:" + str_array[1];
                    con_addr.setText(s);

                    Toast.makeText(MainActivity.this,
                            str_array[0] + ":" + str_array[1],Toast.LENGTH_SHORT).show();
                    break;

                case UI_MODEL_SCAN_DEVICE:
                    strText = "扫描成功";
                    con_addr.setText(strText);
                    break;

                case UI_MODEL_CONNED_DEVICE:

                    strText = "已连接  MAC :"+deviceAddr;
                    con_addr.setText(strText);
//                    strText = "连接NAME :"+deviceName;
//                    con_name.setText(strText);
                    Toast.makeText(MainActivity.this,deviceConAddr + "连接成功",Toast.LENGTH_SHORT).show();

                    break;
                case  UI_MODEL_CON_DEVICE_TIMEOUT:
//                con_btn.setEnabled(false);
//                discon_btn.setEnabled(false);
                    LogUtils.d( "UI_MODEL_CON_DEVICE_TIMEOUT");
                    strText = "连接超时 MAC: " + deviceConAddr;
                    con_addr.setText(strText);
//                    con_name.setText("");
//                  Looper.prepare();//给当前线程初始化Looper
                    Toast.makeText(MainActivity.this,deviceConAddr + "连接超时",Toast.LENGTH_SHORT).show();
                    break;

                case UI_MODEL_DISCON:
                    break;

            }
            return true;
        }
    });

    final Runnable mUpdateUI = new Runnable() {
        @Override
        public void run() {
            if(ui_model == UI_MODEL_DISCON) {
//               con_btn.setEnabled(false);
//               discon_btn.setEnabled(false);
                con_addr.setText("");
//                con_name.setText("");
                rx_txt.setText("");
               if(intdeviceErrCode == BleCommStatus.BLE_ERROR_OK){
                   Toast.makeText(
                           MainActivity.this,
                           deviceConAddr + "已正常断开",
                           Toast.LENGTH_SHORT).show();
               }else if(intdeviceErrCode == BleCommStatus.BLE_ERROR_CONNECTION_TIMEOUT){
                   Toast.makeText(
                           MainActivity.this,
                           deviceConAddr + "连接超时断开",
                           Toast.LENGTH_SHORT).show();
               }
               deviceConAddr = null;
            }
//            else if(ui_model == UI_MODEL_SCAN_DEVICE){
////                con_btn.setEnabled(true);
////                discon_btn.setEnabled(false);
//                String strText;
//                strText = "扫描MAC :"+deviceAddr;
//                con_addr.setText(strText);
//                strText = "扫描NAME :"+deviceName;
//                con_name.setText(strText);
//
//            }
//            else if(ui_model == UI_MODEL_CONNED_DEVICE){
//                String strText;
//                strText = "连接MAC :"+deviceAddr;
//                con_addr.setText(strText);
//                strText = "连接NAME :"+deviceName;
//                con_name.setText(strText);
//                Toast.makeText(MainActivity.this,deviceConAddr + "连接成功",Toast.LENGTH_SHORT).show();
////                con_btn.setEnabled(false);
////                discon_btn.setEnabled(true);
//            }
            else if(ui_model == UI_MODEL_RECIVE_DAT){
                Toast.makeText(MainActivity.this, rx_string, Toast.LENGTH_SHORT).show();
                rx_txt.append(rx_string);
                int offset = rx_txt.getLineCount() * rx_txt.getLineHeight();
                LogUtils.i( "offset:" + offset + "  rx_txt height:" + rx_txt.getHeight());
                if (offset > (rx_txt.getHeight() + 60)) {
                    rx_txt.scrollTo(0, offset - rx_txt.getHeight());
                }
            }else if(ui_model == UI_MODEL_ADV_START){
//                adv_btn.setEnabled(false);
//                adv_btn.setText("停止广播");
            }else if(ui_model == UI_MODEL_ADV_STOP){
//                adv_btn.setEnabled(true);
//                adv_btn.setText("广播");
            }
//            else if(ui_model == UI_MODEL_SCAN_DEVICE_TIMEOUT){
////                con_btn.setEnabled(false);
////                discon_btn.setEnabled(false);
//                con_addr.setText("");
//                con_name.setText("");
//                Toast.makeText(MainActivity.this,"扫描超时",Toast.LENGTH_SHORT).show();
//                LogUtils.i( "UI_MODEL_SCAN_DEVICE_TIMEOUT");
//            }
//            else if(ui_model == UI_MODEL_CON_DEVICE_TIMEOUT) {
////                con_btn.setEnabled(false);
////                discon_btn.setEnabled(false);
//                LogUtils.d( "UI_MODEL_CON_DEVICE_TIMEOUT");
//                con_addr.setText("");
////                con_name.setText("");
////                Looper.prepare();//给当前线程初始化Looper
//                Toast.makeText(MainActivity.this,deviceConAddr + "连接超时",Toast.LENGTH_SHORT).show();
////                Looper.loop();
//            }
            else if(ui_model == UI_MODEL_NAME_SET) {
//                name_btn.setEnabled(false);
//                adv_btn.setEnabled(true);
//                adv_btn.setText("广播");
                Toast.makeText(MainActivity.this,"设置设备名:" + deviceSetName,Toast.LENGTH_SHORT).show();
            }else if(ui_model == UI_MODEL_START_FAILURE){
                Toast.makeText(MainActivity.this,strErrorMsg,Toast.LENGTH_SHORT).show();
            }else if(ui_model == UI_MODEL_SEND_OK){
                Toast.makeText(MainActivity.this,"发送成功",Toast.LENGTH_SHORT).show();
            }

        }
    };

    private void ensureBleFeaturesAvailable() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            LogUtils.e( "Bluetooth not supported");
            finish();
        }else{
            LogUtils.i( "Bluetooth supported");
        }
        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if(mBluetoothLeAdvertiser == null){
            Toast.makeText(this, "the device not support peripheral", Toast.LENGTH_SHORT).show();
            LogUtils.e( "the device not support peripheral");
//          finish();
        } else{
            LogUtils.i( "the device support peripheral");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 禁用横屏
        setContentView(R.layout.activity_main);

        checkbox_hex=(CheckBox) findViewById(R.id.checkBox_hex);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                first = true;
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }

        ensureBleFeaturesAvailable();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogUtils.i("onBindService");
                bleService = ((BleService.MyBinder)service).getService();
                bleService.setOnBleCommProgressListener(onServiceProgressListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                LogUtils.e("service disconnected.");
            }

        };

        if(!first) {
            Intent intent = new Intent(this, BleService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }
        tx_edit = findViewById(R.id.tx_txt);
        rx_txt = findViewById(R.id.rx_txt);
        rx_txt.setMovementMethod(ScrollingMovementMethod.getInstance());
        con_addr = findViewById(R.id.conn_addr);
//        con_name = findViewById(R.id.con_name);
        //    TextView con_name;
        Spinner spinnerAddr = findViewById(R.id.spinner_addr);

        Spinnerlist.add("Scan Device");
//        Spinnerlist.add("2");
//        Spinnerlist.add("3");
        ArrayAdapter spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Spinnerlist);

        spinnerAddr.setAdapter(spinnerAdapter);
//        spinnerAddr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerAddr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                LogUtils.i("onItemSelected position=" + pos);

                if(pos != 0 ){
                    Message msg = Message.obtain(); // 实例化消息对象
                    msg.what = UI_MODEL_SPINNER_SELECT; // 消息标识

                    StringBuilder url= new StringBuilder();

                    url.append("您选择的MAC地址是");
                    url.append(",");
                    url.append(Spinnerlist.get(pos));
                    msg.obj = url.toString(); // 消息内容存放

                    handler.sendMessage(msg);

                    for(ScanDevice _scan_device:ScanDeviceList){
                        if(_scan_device.getMac().equals(Spinnerlist.get(pos))){
                            deviceAddr = _scan_device.getMac();
                            deviceAdvFlag = _scan_device.getAdvFlag();
                            LogUtils.i("Spinner select deviceAddr=%s deviceAdvFlag=%d",
                                    deviceAddr, deviceAdvFlag);
                        }
                    }
                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        con_btn = findViewById(R.id.con_btn);
//        con_test_btn = findViewById(R.id.con_test_btn);
        discon_btn = findViewById(R.id.discon_btn);
//        adv_btn = findViewById(R.id.adv_btn);
        name_edit = findViewById(R.id.name_txt);
        set_btn = findViewById(R.id.set_btn);
        con_btn.setEnabled(true);
//        con_test_btn.setEnabled(true);
        discon_btn.setEnabled(true);
//        adv_btn.setEnabled(true);
        set_btn.setEnabled(true);

        LogUtils.getLogConfig()
                .configAllowLog(true)  // 是否在Logcat显示日志
                .configTagPrefix(TAG) // 配置统一的TAG 前缀
//                .configFormatTag("%d{HH:mm:ss:SSS} %t %c{-5}") // 首行显示信息(可配置日期，线程等等)
                .configShowBorders(false) // 是否显示边框
                .configLevel(LogLevel.TYPE_VERBOSE); // 配置可展示日志等级

        // 支持输入日志到文件
        String filePath = getExternalFilesDir(null) + "/LogUtils/logs/";
        LogUtils.getLog2FileConfig()
                .configLog2FileEnable(true)  // 是否输出日志到文件
                .configLogFileEngine(new LogFileEngineFactory(this)) // 日志文件引擎实现
                .configLog2FilePath(filePath)  // 日志路径
                .configLog2FileNameFormat("app-%d{yyyyMMddhhmm}.txt") // 日志文件名称
                .configLog2FileLevel(LogLevel.TYPE_VERBOSE) // 文件日志等级
                .configLogFileFilter(new LogFileFilter() {  // 文件日志过滤
                    @Override
                    public boolean accept(int level, String tag, String logContent) {
                        return true;
                    }
                });


        LogUtils.v("More convenient and easy to use android Log manager");

        LogUtils.d("More convenient and easy to use android Log manager %d",
                LogLevel.TYPE_DEBUG);
        LogUtils.i("More convenient and easy to use android Log manager %d",
                LogLevel.TYPE_INFO);
        LogUtils.w("More convenient and easy to use android Log manager %d",
                LogLevel.TYPE_WARM);
        LogUtils.e("More convenient and easy to use android Log manager %s",
                "dfs");


    }

    private static BluetoothAdapter mBluetoothAdapter;

    public void CheckEnableBluetoothAdapt(){
//        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(!mBluetoothAdapter.isEnabled()){
                LogUtils.i("mBluetoothAdapter enable");
                mBluetoothAdapter.enable();
                while(!mBluetoothAdapter.isEnabled());
            }else{
                LogUtils.i("mBluetoothAdapter is enabled");
            }
//        }
    }

    public void onStartConn(View view){
        if(deviceAdvFlag == (BleCommStatus.ADV_LE_FLAG | BleCommStatus.ADV_BR_EDR_FLAG)) {
            bleCommMethod.bleStartConnect(deviceAddr,conn_pass,10000); /* 5000毫秒之後,连接超时*/
//            cancelAdvinfoTimer();
            is_connect = true;
        } else {
            if ((deviceAdvFlag & BleCommStatus.ADV_BR_EDR_FLAG) == 0) {
                Toast.makeText(this,"this device support BR/EDR can not connect",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onStartConnTest(View view){
        bleCommMethod.bleStartConnect(deviceAddr,conn_pass,5000); /* 5000毫秒之後,连接超时*/
        cancelAdvinfoTimer();
        is_connect = true;
    }

    public void onNameSet(View view){
        bleCommMethod.bleClose();
        String s = name_edit.getText().toString();
        LogUtils.d( "name = " + s);
        if(s.length() == 0){
            Toast.makeText(this,"先设置设备名称",Toast.LENGTH_SHORT).show();
            return;
        }
        deviceSetName = s;
//        bleCommMethod.bleStopAdvertisement();
        CheckEnableBluetoothAdapt();
        bleCommMethod.bleRestart(deviceSetName);
        ui_model = UI_MODEL_NAME_SET;
        handler.post(mUpdateUI);
    }


    public void onAdvSimulate(View view){
        if(!is_adverting){
            bleCommMethod.bleClose();
            bleCommMethod.bleStartAdvertisementSimulate(deviceSetName);
            is_adverting = true;
            ui_model = UI_MODEL_ADV_START;
            handler.post(mUpdateUI);
        }else {
            bleCommMethod.bleStopAdvertisement();
            String name = name_edit.getText().toString();

            if(name.length() == 0){
                name = "bt uart";
            }
            LogUtils.d( "name = " + name);
            CheckEnableBluetoothAdapt();
            bleCommMethod.bleRestart(name);
            is_adverting = false;
            ui_model = UI_MODEL_ADV_STOP;
            handler.post(mUpdateUI);
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    byte test_send = 0;
    public void onSendDirect(View view){
        try{
            LogUtils.i( "onSendDirect :");
            if(!deviceAddr.equals("")){
                byte [] data;
//            byte [] data = { test_send,test_send,test_send,test_send,test_send};
                String s = tx_edit.getText().toString().trim();
//            LogUtils.i( "hexStringToByteArray=" +  hexStringToByteArray(s));
                if (checkbox_hex.isChecked()){
                    data = hexStringToByteArray(s);
                    for (byte b: data){
                        LogUtils.i(String.format("0x%2x", b));
                    }
                }else{
                    data = s.getBytes();
                }

                if(data.length > 10){
                    Toast.makeText(this,"数据太长!",Toast.LENGTH_SHORT).show();
                    return;
                }


                bleCommMethod.bleScanResponse(deviceAddr, data);
                test_send += 1;

            }
        }catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"数据格式错误",Toast.LENGTH_SHORT).show();
        }

    }

    public void onDisConn(View view){
        bleCommMethod.bleDisConnect();
    }

    public void OnSendDat(View view){

        try{
            byte ble_oper = bleCommMethod.bleGetOperator();
            LogUtils.i( "bleGetOperator :" + ble_oper);
            if(ble_oper == BleCommStatus.OPER_TRAN){
                byte [] data;
                String s = tx_edit.getText().toString().trim();
                if (checkbox_hex.isChecked()){
                    LogUtils.i("send data=0x%s",s);
                    data = hexStringToByteArray(s);
//                    for (byte b: data){
//                        LogUtils.i(String.format("0x%2x", b));
//                    }
                }else{
                    data = s.getBytes();
                }

                if(data.length > 16){
                    Toast.makeText(this,"数据太长!",Toast.LENGTH_SHORT).show();
                    return;
                }

                bleCommMethod.bleSendMessage(data,(byte)data.length);

            }else{
                Toast.makeText(this,"连接未建立",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"数据格式错误",Toast.LENGTH_SHORT).show();
        }


    }

    private  Timer mWaittoFinsh = null;

    private void startWaittoCloseTimer() {
        LogUtils.i("startWaittoFinshTimer");
        if(mWaittoFinsh == null){
            mWaittoFinsh = new Timer();
            mWaittoFinsh.schedule(new TimerTask() {
                @Override
                public void run() {

                    if(!is_connect){
                        LogUtils.i("now disconnect");
                        mWaittoFinsh.cancel();
                        mWaittoFinsh = null;
//                        finish();
                    }
                }
            }, 0,50/* 表示1000毫秒之後，執行一次 */);
        }

    }

    @Override
    public void onBackPressed(){
        LogUtils.i( "onBackPressed :");
        if(is_connect){
            LogUtils.i( "need to wait ble disconnect :");
            bleCommMethod.bleDisConnect();
            startWaittoCloseTimer();
            Toast.makeText(MainActivity.this,
                    "正在断开设备" + deviceConAddr + "请稍后重试",Toast.LENGTH_SHORT).show();
        }else{

            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {
        LogUtils.i( "onDestroy :");
        bleCommMethod.bleClose();
        unbindService(connection);
        connection = null;
        super.onDestroy();
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    OnBleCommProgressListener onServiceProgressListener = new OnBleCommProgressListener() {
        @Override
        public void onScanDevice(String device_addr, String device_name, byte adv_flag) {
            LogUtils.d( "onScanDevice " + device_addr + ' ' + device_name);
            if(!is_connect){
//                cancelAdvinfoTimer();
//                deviceAddr = device_addr;
//                deviceName = device_name;
//                deviceAdvFlag = adv_flag;
//                ui_model = UI_MODEL_SCAN_DEVICE;
//                handler.post(mUpdateUI);
//                startAdvinfoTimer();
                if(addMac(device_addr, adv_flag)){
                    LogUtils.i("new device find device_addr=%s" , device_addr);
//                    Spinnerlist.add(device_addr);
                    deviceAddr = device_addr;
                    deviceName = device_name;
                    deviceAdvFlag = adv_flag;
                    handler.sendEmptyMessage(UI_MODEL_SCAN_DEVICE);
                }




            }

        }

        @Override
        public void onConnection(String device_addr,int errorCode) {
            if(errorCode == BleCommStatus.BLE_ERROR_OK){
                LogUtils.i( "onConnection ok");
//                ui_model = UI_MODEL_CONNED_DEVICE;
                deviceAddr = device_addr;
                deviceConAddr = device_addr;
//                handler.post(mUpdateUI);
                handler.sendEmptyMessage(UI_MODEL_CONNED_DEVICE);
            } else {
                is_connect = false;
                LogUtils.e( "onConnection error:" + errorCode);
                switch (errorCode){
                    case BleCommStatus.BLE_ERROR_CONNECTION_TIMEOUT:
                        deviceConAddr = device_addr;
                        deviceAddr = "";
                        deviceName = "";
                        deviceAdvFlag = 0;
//                        ui_model = UI_MODEL_CON_DEVICE_TIMEOUT;
//                        handler.post(mUpdateUI);
                        handler.sendEmptyMessage(UI_MODEL_CON_DEVICE_TIMEOUT);
                        break;

                    default:
                        break;
                }
            }
        }

        @Override
        public void onDisConnection(String device_address, int errorCode) {
            intdeviceErrCode = errorCode;
            is_connect = false;
            ui_model = UI_MODEL_DISCON;
            handler.post(mUpdateUI);

        }

        @Override
        public void onReceive(byte[] dat, int len) {
            if(len>0) {
                byte[] b = new byte[len];
                rx_string = rx_index + " |  len:" + len + "   dat:";

                System.arraycopy(dat, 0, b, 0, len);
                rx_string += bytesToHexString(b);
                rx_string += "\n";
                LogUtils.i( "onReceive rx string = " + rx_string);
                rx_index++;
//                ui_model = UI_MODEL_RECIVE_DAT;
//                handler.post(mUpdateUI);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, rx_string, Toast.LENGTH_SHORT).show();
                        rx_txt.append(rx_string);
                        int offset = rx_txt.getLineCount() * rx_txt.getLineHeight();
                        LogUtils.i( "offset:" + offset + "  rx_txt height:" + rx_txt.getHeight());
                        if (offset > (rx_txt.getHeight() + 60)) {
                            rx_txt.scrollTo(0, offset - rx_txt.getHeight());
                        }
                    }
                });
            }

        }

        @Override
        public void onSendSta(int code) {
            LogUtils.i( "onSendSta index= " + code);
//            ui_model = UI_MODEL_SEND_OK;
//            handler.post(mUpdateUI);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,"发送成功",Toast.LENGTH_SHORT).show();
                    LogUtils.i( "发送成功");
                }
            });


        }

        @Override
        public void onServiceOpen() {
            bleCommMethod = bleService.getBleCommMethod();
            String name = name_edit.getText().toString();
            if(name.length() == 0){
                name = "bt uart";
            }
            LogUtils.i( "onServiceOpen name = " + name);
            CheckEnableBluetoothAdapt();
            bleCommMethod.bleOpen(name);
        }

        @Override
        public void onStartSuccess(byte oper){

            switch (oper){
                case BleCommStatus.OPER_ADV:
                    LogUtils.d("onStartSuccess bleStartAdvertisementSimulate success");
                    break;
                case BleCommStatus.OPER_CON_REQ:
                    LogUtils.d("onStartSuccess bleStartConnect success");
                    break;
                case BleCommStatus.OPER_DISCON_REQ:
                    LogUtils.d("onStartSuccess bleDisConnect success");
                    break;

                case BleCommStatus.OPER_TRAN:
                    LogUtils.d("onStartSuccess send message requirement success");
                    break;

                case BleCommStatus.OPER_OPEN:
                    LogUtils.d("onStartSuccess ble communication session open success");
                    break;

                case BleCommStatus.OPER_CLOSE:
                    LogUtils.d("onStartSuccess ble communication session close success");
                    break;

                default:
                    LogUtils.d( "onStartSuccess reserve operation  " + oper);
                    break;
            }
        }

        @Override
        public void onStartFailure(byte oper, int errorCode){
//            LogUtils.d( "onStartFailure  " + oper + ' ' + errorCode);
            switch (oper){
                case BleCommStatus.OPER_ADV:
                    if(errorCode == BleCommStatus.BLE_ERROR_INVALID_PARAMETER){ // 1
                        LogUtils.e("onStartFailure bleStartAdvertisementSimulate parameter fail");
                        strErrorMsg = "onStartFailure bleStartAdvertisementSimulate parameter fail";
                    }else if(errorCode == BleCommStatus.BLE_ERROR_INVALID_OPERATION){
                        LogUtils.e("onStartFailure bleStartAdvertisementSimulate operation error");
                        strErrorMsg = "onStartFailure bleStartAdvertisementSimulate operation error";
                    }
                    break;
                case BleCommStatus.OPER_CON_REQ:
                    if(errorCode == BleCommStatus.BLE_ERROR_INVALID_PARAMETER){ // 1
                        LogUtils.e("onStartFailure bleStartConnect parameter incorrect");
                        strErrorMsg = "onStartFailure bleStartConnect parameter incorrect";
                    } else if(errorCode == BleCommStatus.BLE_ERROR_INVALID_OPERATION){
                        LogUtils.e("onStartFailure bleStartConnect operation error");
                        strErrorMsg = "onStartFailure bleStartConnect operation error";
                    }
                    break;
                case BleCommStatus.OPER_DISCON_REQ:
                    if(errorCode == BleCommStatus.BLE_ERROR_INVALID_PARAMETER){ // 1
                        LogUtils.e("onStartFailure bleDisConnect parameter incorrect");
                        strErrorMsg = "onStartFailure bleDisConnect parameter incorrect";
                    } else if(errorCode == BleCommStatus.BLE_ERROR_INVALID_OPERATION){
                        LogUtils.e("onStartFailure bleDisConnect operation error");
                        strErrorMsg = "onStartFailure bleDisConnect operation error";
                    }
                    break;

                case BleCommStatus.OPER_TRAN:
                    if(errorCode == BleCommStatus.BLE_ERROR_INVALID_PARAMETER){ // 1
                        LogUtils.e("onStartFailure bleSendMessage parameter incorrect");
                        strErrorMsg = "onStartFailure bleSendMessage parameter incorrect";
                    } else if(errorCode == BleCommStatus.BLE_ERROR_INVALID_OPERATION){
                        LogUtils.e("onStartFailure ble connection interval operation error");
                        strErrorMsg = "onStartFailure ble connection interval operation error";
                    }
                    break;

                default:
                    LogUtils.e( "onStartFailure reserve operation  " + oper + ' ' + errorCode);
                    strErrorMsg = "onStartFailure reserve operation  " + oper + ' ' + errorCode;
                    break;
            }
            ui_model = UI_MODEL_START_FAILURE;
            handler.post(mUpdateUI);
        }

        @Override
        public void onStateChange(int state){
            LogUtils.i("onStateChange= %d", state);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent i = new Intent(this, BleService.class);
                bindService(i, connection, BIND_AUTO_CREATE);

            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

}
