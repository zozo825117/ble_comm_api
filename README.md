ble_comm_api
========

# 描述
`blecomm-api.jar`
可实现与模拟蓝牙设备实现基本的收发通讯的基本方法,本样例就是一种基于api的实现方法.

![](http://34.92.38.105/laizhi_doc/ble_comm_jar/20190528001442.png)

# 要求
* android 最低支持的版本:   
  Android 5.1 (LOLLIPOP_MR1  API22)
* ble peripheral 支持
  目前同时用到ble的Central角色和peripheral角色,所以在android版本满足的前提下,还需要检测手机是否支持peripheral角色,参考如下:
    MainActivity.java
    ```java
     private void ensureBleFeaturesAvailable() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
            finish();
        }else{
            Log.i(TAG, "Bluetooth supported");
        }
        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if(mBluetoothLeAdvertiser == null){
            Toast.makeText(this, "the device not support peripheral", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "the device not support peripheral");
            finish();
        } else{
            Log.i(TAG, "the device support peripheral");
        }
    }
    ```
# 特性
- [x] bleStartAdvertisementSimulate
- [x] bleStopAdvertisement
- [x] bleStartConnect
- [x] bleDisConnect wear
- [x] bleSendMessage
- [x] onReceive
- [x] onStartSuccess
- [x] onStartFailure

# TODO
- [ ] onSendSta

# Project Organization
![](http://34.92.38.105/laizhi_doc/ble_comm_jar/2019-05-27_235301.png)

# Useage
1. 在bulid.gradle中加入`blecomm-api.jar`的依赖
2. 需要在manifest中加入蓝牙相关权限
3. 需要在manifest中注册com.wearlink.blecomm.BleService服务
4. 在activity用如下方法开启服务:  
  `((BleService.MyBinder)service).getService()`
5. 开启服务同时,用如下方法设置以实现的接口`onServiceProgressListener`:
  `bleService.setOnBleCommProgressListener(onServiceProgressListener);`
6. 在此接口的`onServiceOpen`方法中通过如下方法获取主要方法接口:
  `bleService.getBleCommMethod();`

# 主要方法
由BleCommMethod接口提供:
```java
/**
 * ble communication method
 */

public interface BleCommMethod {
    /**
     * ble communication session open
     * @param device_name : device name
     */
    void bleOpen(String device_name);
    /**
     * ble communication session restart
     * @param device_name : device name
     */
    void bleRestart(String device_name);
    /**
     * ble communication session close
     */
    void bleClose();
    /**
     * get now ble communication operator
     * @return byte : operation mode
     */
    byte bleGetOperator();
    /**
     * ble advertisement start by special device name
     * only simulate method because ble communication session
     * support device need equal  LE General Discoverable Mode and
     * BR/EDR Not Supported.
     * @param device_name : device name
     */
    void bleStartAdvertisementSimulate(String device_name);
    /**
     * ble advertisement stop
     */
    void bleStopAdvertisement();
    /**
     * ble communication connect requirement
     * @param device_addr : device mac address
     * @param conn_pass : ble communication pass word
     * @param time_out :  ble connection timeout
     */
    void bleStartConnect(String device_addr, byte[] conn_pass, int time_out);
    /**
     * ble communication disconnect requirement
     */
    void bleDisConnect();
    /**
     * ble communication send message requirement
     * @param dat : ble send message
     * @param len :  length of send message maximum is 15 bytes.
     */
    boolean bleSendMessage(byte[] dat, byte len);

}
```
# 主要事件回调
由OnBleCommProgressListener接口提供,客户可根据需求重写相应方法:
```java
/**
 * ble communication progress listener interface.
 */

public interface OnBleCommProgressListener {
    /**
     * scan device return
     * @param device_address: device mac address
     * @param device_name : device name
     * @param adv_flag : advertisement flag
     */
    void onScanDevice(String device_address, String device_name, byte adv_flag);
    /**
     * scan device return
     * @param device_address: device mac address
     * @param errorCode :  BLE_ERROR_OK or BLE_ERROR_CONNECTION_TIMEOUT
     */
    void onConnection(String device_address,int errorCode);
    /**
     * scan device return
     * @param device_address : device mac address
     * @param errorCode :
     *                  BLE_ERROR_OK : response for OPER_DISCON_REQ
     *                  BLE_ERROR_CONNECTION_TIMEOUT
     */
    void onDisConnection(String device_address, int errorCode);
    /**
     * ble communication send message requirement
     * @param dat : ble receive message
     * @param len :  length of receive message maximum is 15 bytes.
     */
    void onReceive(byte[] dat,int len);
    void onSendSta(int code);
    /**
     * service open success callback
     * ble communication core is a service named BleService
     * user need start it in user's Activity like this:
     * ((BleService.MyBinder)service).getService()
     * and register service in manifest like this
     * <service android:name="com.wearlink.blecomm.BleService"/>
     */
    void onServiceOpen();
    /**
     * ble communication requirement success
     * @param Oper :
     *             OPER_ADV : bleStartAdvertisementSimulate
     *             OPER_CON_REQ : bleStartConnect
     *             OPER_DISCON_REQ : bleDisConnect
     *             OPER_TRAN : bleSendMessage
     */
    void onStartSuccess(byte Oper);
    /**
     * ble communication requirement failed
     * @param Oper :
     *             OPER_ADV : bleStartAdvertisementSimulate
     *             OPER_CON_REQ : bleStartConnect
     *             OPER_DISCON_REQ : bleDisConnect
     *             OPER_TRAN : bleSendMessage
     * @param errorCode :  BLE_ERROR_INVALID_PARAMETER or BLE_ERROR_INVALID_OPERATION
     */
    void onStartFailure(byte Oper, int errorCode);
}
```

# 操作模式(状态机)
```java
    /**
     * operation mode: advertisement.
     */
    static final byte OPER_ADV       =   0x00;
    /**
     * operation mode: connection requirement.
     */
    static final byte OPER_CON_REQ   =   0x01;
    /**
     * operation mode: connection response.
     */
    static final byte OPER_CON_RSP   =   0x02;
    /**
     * operation mode: disconnection requirement.
     */
    static final byte OPER_DISCON_REQ =   0x03;
    /**
     * operation mode: send message requirement.
     */
    static final byte OPER_TRAN  =   0x04;
    /**
     * operation mode: ble communication session open
     */
    static final byte OPER_OPEN = 0x05;
    /**
     * operation mode: ble communication session clos
     */
    static final byte OPER_CLOSE = 0x06;
```
# 回调函数返回状态
```java
    /**
     * No Error occurred
     */
    static final byte  BLE_ERROR_OK = 0x00;

    /**
     * Error: At least one of the input parameters is invalid
     */
    static final byte BLE_ERROR_INVALID_PARAMETER = 0x01;

    /**
     * Error: Operation is not permitted
     */
    static final byte BLE_ERROR_INVALID_OPERATION = 0x02;
    /**
     * Error: connection requirement failed
     * time out
     */
    static final byte BLE_ERROR_CONNECTION_TIMEOUT = 0x03;
    /**
     * Error: connection has disconnected
     * time out
     */
    static final byte BLE_ERROR_CONNECTION_DISCONNECTED = 0x04;
```