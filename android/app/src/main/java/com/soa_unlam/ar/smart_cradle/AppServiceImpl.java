package com.soa_unlam.ar.smart_cradle;

import android.bluetooth.BluetoothSocket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by A646241 on 22/06/2017.
 */

public class AppServiceImpl implements AppService {

    private Thread connectedThread = null;

    private String minTemp;

    private String maxTemp;

    private int deviceStatus = Integer.parseInt(AppConstants.APAGADO);

    private int updateTempConfig = AppConstants.TEMP_CONFIG_STOPPED;

    private static final AppServiceImpl INSTANCE = new AppServiceImpl();

    private static final AtomicInteger atomicInteger = new AtomicInteger(0);

    private AppServiceImpl() {}

    public static AppServiceImpl getInstance() {
        return INSTANCE;
    }

    public Thread getConnectedThread() {
        return connectedThread;
    }

    public void setConnectedThread(Thread connectedThread) {
        this.connectedThread = connectedThread;
    }

    public String getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(String minTemp) {
        this.minTemp = minTemp;
    }

    public String getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(String maxTemp) {
        this.maxTemp = maxTemp;
    }

    public int getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(int deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public int getUpdateTempConfig() {
        return updateTempConfig;
    }

    public void setUpdateTempConfig(int updateTempConfig) {
        this.updateTempConfig = updateTempConfig;
    }

    public AtomicInteger getAtomicInteger() {
        return AppServiceImpl.atomicInteger;
    }
}
