package com.soa_unlam.ar.smart_cradle;

import android.app.Activity;
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

/**
 * Created by A646241 on 26/06/2017.
 */

public class SensorManagerServiceImpl extends IntentService implements SensorEventListener {

    private SensorManager sensorManager;

    private long lastUpdate = 0L;

    private float lastValueX;
    private float lastValueY;
    private float lastValueZ;

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;
    private static final int AXIS_Z = 2;

    private static final int SHAKE_THRESHOLD = 800;

    private static final int SENSOR_SENSITIVITY = 4;

    private ResultReceiver receiver;

    private static final AppService APP_SERVICE = AppServiceImpl.getInstance();

    public SensorManagerServiceImpl() {
        super(SensorManagerServiceImpl.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
        Log.d("MyService", "onCreate");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);          // get an instance of the SensorManager class, lets us access sensors.


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
            //sensorAppService.getSensorLight().setText("SensorLight_Value: " + lightLevel);
            Bundle bundle = new Bundle();
            bundle.putFloat("CHANGE_LIGHT_DEVICE", lightLevel);
            receiver.send(AppConstants.UPDATE_LIGHT, bundle);
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // abrir la configuración
            float currentReader = event.values[0];
            if (currentReader >= -SENSOR_SENSITIVITY && currentReader <= SENSOR_SENSITIVITY) {
                //near
                //sensorAppService.getSensorProximity().setText("SensorProximity_Value: Near - " + currentReader);

                receiver.send(3, Bundle.EMPTY);
                //Toast.makeText(SensorManagerServiceImpl.this, "Cerca !!!", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("SENSOR_MANAGER", "Proximity is far");
                //sensorAppService.getSensorProximity().setText("SensorProximity_Value: Far - " + currentReader);
                //receiver.send(4, Bundle.EMPTY);
                //Toast.makeText(SensorManagerServiceImpl.this, "Lejos !!!", Toast.LENGTH_SHORT).show();
            }
        }
        /* unregister if we just want one result. */
//        sensorManager.unregisterListener(this);

    }
}
