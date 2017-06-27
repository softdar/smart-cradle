package com.soa_unlam.ar.smart_cradle;

import android.bluetooth.BluetoothSocket;

/**
 * Created by A646241 on 22/06/2017.
 */

public class AppServiceImpl implements AppService {

    private Thread connectedThread = null;

    private String minTemp;

    private String maxTemp;

    private static final AppServiceImpl INSTANCE = new AppServiceImpl();

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
}
