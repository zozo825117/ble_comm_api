package com.wearlink.ble_uart;

/**
 * ble communication api operation status.
 */
abstract class BleCommStatus {
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
    /**
     * Advertisement Flag : LE General Discoverable Mode.
     */
    static final byte ADV_LE_FLAG = 0x02;
    /**
     * Advertisement Flag : BR/EDR Not Supported.
     */
    static final byte ADV_BR_EDR_FLAG = 0x04;

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
}
