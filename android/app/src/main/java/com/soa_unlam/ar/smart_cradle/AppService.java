package com.soa_unlam.ar.smart_cradle;

import android.bluetooth.BluetoothSocket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by A646241 on 22/06/2017.
 */

public interface AppService {

    Thread getConnectedThread();

    void setConnectedThread(Thread connectedThread);

    String getMinTemp();

    void setMinTemp(String minTemp);

    String getMaxTemp();

    void setMaxTemp(String maxTemp);

    public int getDeviceStatus();

    void setDeviceStatus(int deviceStatus);

    int getUpdateTempConfig();

    void setUpdateTempConfig(int updateTempConfig);

    AtomicInteger getAtomicInteger();

}
