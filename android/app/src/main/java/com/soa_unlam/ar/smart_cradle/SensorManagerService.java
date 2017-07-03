package com.soa_unlam.ar.smart_cradle;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by A646241 on 26/06/2017.
 */

public class SensorManagerService extends IntentService implements SensorEventListener {

    private SensorManager sensorManager;

    private long lastUpdate = 0L;

    private long lastLightUpdate = System.currentTimeMillis();

    private float lastValueX;
    private float lastValueY;
    private float lastValueZ;

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;
    private static final int AXIS_Z = 2;

    private static final int SHAKE_THRESHOLD = 1300;

    private static final int SENSOR_SENSITIVITY = 4;

    private static final int LIGHT_UPDATE_THRESHOLD = 20000;

    private ResultReceiver receiver;

    private AtomicInteger atomicInteger;
    private static final AppService APP_SERVICE = AppServiceImpl.getInstance();

    public SensorManagerService() {
        super(SensorManagerService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
        Log.d("MyService", "onCreate");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);          // get an instance of the SensorManager class, lets us access sensors.
        atomicInteger = APP_SERVICE.getAtomicInteger();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("MyService", "onStartCommand");
        receiver = intent.getParcelableExtra("receiver");

        List<Sensor> listaSensores = sensorManager.getSensorList(Sensor.TYPE_ALL);
        listaSensores = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if (!listaSensores.isEmpty()) {
            Sensor acelerometerSensor = listaSensores.get(0);
            sensorManager.registerListener(this, acelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        listaSensores = sensorManager.getSensorList(Sensor.TYPE_LIGHT);

        if (!listaSensores.isEmpty()) {
            Sensor lightSensor = listaSensores.get(0);
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }

        listaSensores = sensorManager.getSensorList(Sensor.TYPE_PROXIMITY);

        if (!listaSensores.isEmpty()) {
            Sensor proximitySensor = listaSensores.get(0);
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //    // if sensor value is changes, change the values in the respective textview.
    public void onSensorChanged(SensorEvent event) {
        float currentValueX;
        float currentValueY;
        float currentValueZ;
        synchronized (atomicInteger) {
            while (atomicInteger.get() == 0 && APP_SERVICE.getDeviceStatus() == AppConstants.SHAKE_ON) {
                try {
                    atomicInteger.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Encender o Apagar Dispositivo
                float[] values = event.values;
                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    currentValueX = values[AXIS_X];
                    currentValueY = values[AXIS_Y];
                    currentValueZ = values[AXIS_Z];
                    float sumCurrentValues = (currentValueX + currentValueY + currentValueZ);
                    float sumLastValues = (lastValueX + lastValueY + lastValueZ);
                    float speed = Math.abs(sumCurrentValues - sumLastValues) / diffTime * 10000;

                    if (speed > SHAKE_THRESHOLD) {
                        //sensorAppService.getSensorShake().setText("SensorShake_Value: " + speed);
                        //ConnectedThread connectedThread = APP_SERVICE.getConnectedThread();
                        if (APP_SERVICE.getDeviceStatus() == AppConstants.SHAKE_ON) {
                            receiver.send(AppConstants.SHAKE_OFF, Bundle.EMPTY);
                        } else {
                            receiver.send(AppConstants.SHAKE_ON, Bundle.EMPTY);
                        }
                        //Toast.makeText("text", "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                    }
                    lastValueX = currentValueX;
                    lastValueY = currentValueY;
                    lastValueZ = currentValueZ;
                }
            } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                // Controlar el brillo del dispositivo
                float lightLevel = event.values[0];
                long curTime = System.currentTimeMillis();
                //sensorAppService.getSensorLight().setText("SensorLight_Value: " + lightLevel);
                long difTime = (curTime - lastLightUpdate);
                boolean isConfigRunning = APP_SERVICE.getUpdateTempConfig() == AppConstants.TEMP_CONFIG_STOPPED;
                if (difTime > LIGHT_UPDATE_THRESHOLD && isConfigRunning) {
                    lastLightUpdate = curTime;
                    Bundle bundle = new Bundle();
                    bundle.putFloat("LIGHT_LEVEL", lightLevel);
                    receiver.send(AppConstants.UPDATE_LIGHT, bundle);
                }
                lastLightUpdate -= 10L;
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                // abrir la configuración
                float currentReader = event.values[0];
                if (currentReader >= -SENSOR_SENSITIVITY && currentReader <= SENSOR_SENSITIVITY) {
                    //near
                    //sensorAppService.getSensorProximity().setText("SensorProximity_Value: Near - " + currentReader);
                    if (APP_SERVICE.getUpdateTempConfig() == AppConstants.TEMP_CONFIG_STOPPED) {
                        Toast.makeText(SensorManagerService.this, "Cambiar Temperatura !!!", Toast.LENGTH_SHORT).show();
                        APP_SERVICE.setUpdateTempConfig(AppConstants.TEMP_CONFIG_RUNNING);
                        receiver.send(AppConstants.CHANGE_TEMP_CONFIG, Bundle.EMPTY);
                    }
                } else {
                    Log.d("SENSOR_MANAGER", "Proximity is far");
                    //sensorAppService.getSensorProximity().setText("SensorProximity_Value: Far - " + currentReader);
                    //receiver.send(4, Bundle.EMPTY);
                    //Toast.makeText(SensorManagerService.this, "Lejos !!!", Toast.LENGTH_SHORT).show();
                }
            }
            if (APP_SERVICE.getDeviceStatus() == AppConstants.SHAKE_ON && atomicInteger.get() > 0) {
                atomicInteger.incrementAndGet();
            }
            atomicInteger.notifyAll();
        }

        /* unregister if we just want one result. */
//        sensorManager.unregisterListener(this);

    }
}
