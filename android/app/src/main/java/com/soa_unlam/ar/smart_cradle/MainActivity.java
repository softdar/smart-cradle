package com.soa_unlam.ar.smart_cradle;

/**
 * Created by A646241 on 19/06/2017.
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.soa_unlam.ar.smart_cradle.AppConstants.RECIEVE_MESSAGE;

public class MainActivity extends Activity implements SensorManagerReceiver.Receiver  {
    private static final String TAG = "ArduinoCon_BT";

    private Button btnOn, btnOff, btnConfig;
    private TextView textTemp;
    private TextView textMov;
    private TextView textEstadoSound;
    private TextView textInclDevice;
    private Handler handler;

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;

    private ConnectedThread connectedThread;

    private static final AppService APP_SERVICE = AppServiceImpl.getInstance();

    private SensorManagerReceiver receiver;

    // SPP UUID service
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static final String ADDRESS = "20:16:12:12:22:70";

    private String minTemp = "23";

    private String maxTemp = "25";

    private long lastTime = System.currentTimeMillis();

    private static final int NOTIFICATION_TIME_THRESHOLD = 2000;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        btnOn = (Button) findViewById(R.id.btnOn);					// button LED ON
        btnOff = (Button) findViewById(R.id.btnOff);				// button LED OFF
        btnConfig = (Button) findViewById(R.id.btnConfig);				// button LED OFF
        textTemp = (TextView) findViewById(R.id.textTemp);
        textMov = (TextView) findViewById(R.id.textMov);
        textEstadoSound = (TextView) findViewById(R.id.textEstadoSound);
        textInclDevice = (TextView) findViewById(R.id.textInclDevice);

        APP_SERVICE.setMinTemp(minTemp);
        APP_SERVICE.setMaxTemp(maxTemp);

        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                String[] arrayMsg;
                String strIncom = "";
                try {
                    switch (msg.what) {
                        case RECIEVE_MESSAGE:													// if receive massage
                            byte[] readBuf = (byte[]) msg.obj;
                            arrayMsg = new String(readBuf, 0, msg.arg1).split("\r\n"); // create string from bytes array, and split msgs
                            for (int i = 0; i < arrayMsg.length; i++) {
                                strIncom = arrayMsg[i].replaceAll("\n", "").replaceAll("\r", "");
                                if (!strIncom.isEmpty()) {
                                    // update TextView
                                    updateMessageFromDevice(strIncom.charAt(0));
                                    //txtArduino.setText("Data from Arduino: " + getDeviceMessage(strIncom.charAt(0)));
                                }
                                btnOff.setEnabled(true);
                                btnOn.setEnabled(true);
                            }
                    }
                } catch (Exception e) {
                    errorExit("Fatal Error", "In handleMessage(), fail process info: " + e.getMessage() + "." + strIncom);
                }
            };
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
        checkBTState();

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                onDevice();
                //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                offDevice();
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Cambiando a Configuración", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
                APP_SERVICE.setUpdateTempConfig(AppConstants.TEMP_CONFIG_RUNNING);
                startActivity(intent);
            }
        });

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(ADDRESS);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        bluetoothAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            bluetoothSocket.connect();
            Log.d(TAG, "...Connection ok...");
        } catch (IOException e) {
            try {
                // If fail connection, so it will try native connection.
                BluetoothSocket tmp = device.createRfcommSocketToServiceRecord(APP_UUID);;
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                bluetoothSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
                Thread.sleep(500);
                bluetoothSocket.connect();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e2) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e2);
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            receiver = new SensorManagerReceiver(new Handler());
            receiver.setReceiver(this);
            Intent sensorMngIntent = new Intent(Intent.ACTION_SYNC, null, this, SensorManagerService.class);
            sensorMngIntent.putExtra("receiver", receiver);
            startService(sensorMngIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.setHandler(handler);
        APP_SERVICE.setConnectedThread(connectedThread);
        connectedThread.start();


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "...In onPause()...");

        try {
            bluetoothSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onDestroy() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(bluetoothAdapter ==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private String updateMessageFromDevice(Character msg) {
        if (APP_SERVICE.getDeviceStatus() == Integer.parseInt(AppConstants.APAGADO)) {
            APP_SERVICE.setDeviceStatus(Integer.parseInt(AppConstants.ENCENDIDO));
            btnOn.setEnabled(false);
            btnOn.setTextColor(Color.parseColor("#FFCC99"));
            btnOff.setTextColor(Color.parseColor("#FFFFFF"));
        }
        Integer value = Integer.parseInt(msg.toString());
        String strMessage = "";
        long currentTime = System.currentTimeMillis();
        long difTime = (currentTime - lastTime);
        switch (value) {
            case AppConstants.TEMP_STATUS_KO:
                strMessage = "Temperatura fuera de rango! ";
                textTemp.setText(strMessage);
                if (difTime > NOTIFICATION_TIME_THRESHOLD) {
                    showNotification("smart-cradle", strMessage);
                }
                break;
            case AppConstants.TEMP_STATUS_OK:
                strMessage = "Temperatura en el rango!";
                textTemp.setText(strMessage);
                break;
            case AppConstants.MOV_EXISTS:
                strMessage = "Hay movimiento!";
                textMov.setText(strMessage);
                if (difTime > NOTIFICATION_TIME_THRESHOLD) {
                    showNotification("smart-cradle", strMessage);
                }
                break;
            case AppConstants.MOV_NOT_EXISTS:
                strMessage = "Sin movimiento!";
                textMov.setText(strMessage);
                break;
            case AppConstants.SOUND_ON:
                strMessage = "Sonido detectado!";
                if (difTime > NOTIFICATION_TIME_THRESHOLD) {
                    showNotification("smart-cradle", strMessage);
                }
                textEstadoSound.setText(strMessage);
                break;
            case AppConstants.SOUND_OFF:
                strMessage = "Sonido no detectado!";
                textEstadoSound.setText(strMessage);
                break;
            case AppConstants.INCL_KO:
                strMessage = "Debe acomodar dispositivo!";
                if (difTime > NOTIFICATION_TIME_THRESHOLD) {
                    showNotification("smart-cradle", strMessage);
                }
                textInclDevice.setText(strMessage);
                break;
            case AppConstants.INCL_OK:
                strMessage = "Dispositivo correcto!";
                textInclDevice.setText(strMessage);
        }
        lastTime = currentTime;
        return strMessage;
    }

    private void showNotification(String title, String content) {
        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(content) // message for notification
                .setAutoCancel(true); // clear notification after click
        Intent intent = new Intent(this, MainActivity.class);
        //@SuppressWarnings("WrongConstant")
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());

        mBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case AppConstants.SHAKE_OFF:
                offDevice();
                break;
            case AppConstants.SHAKE_ON:
                onDevice();
                break;
            case AppConstants.UPDATE_LIGHT:
                float lightLevel = resultData.getFloat("LIGHT_LEVEL");
                if (lightLevel < 40) {
                    Toast.makeText(getBaseContext(), "LUZ AMBIENTE MUY BAJA!!!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getBaseContext(), "LUZ AMBIENTE BUENA!!!", Toast.LENGTH_SHORT).show();
                }
                break;
            case AppConstants.CHANGE_TEMP_CONFIG:
                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
                APP_SERVICE.setMinTemp(minTemp);
                APP_SERVICE.setMaxTemp(maxTemp);
                startActivity(intent);
        }
    }

    private void offDevice() {
        btnOff.setEnabled(false);
        btnOn.setEnabled(true);
        btnOff.setTextColor(Color.parseColor("#FFCC99"));
        btnOn.setTextColor(Color.parseColor("#FFFFFF"));
        textTemp.setText("Estado no disponible");
        textMov.setText("Estado no disponible");
        textEstadoSound.setText("Estado no disponible");
        textInclDevice.setText("Estado no disponible");
        APP_SERVICE.setDeviceStatus(Integer.parseInt(AppConstants.APAGADO));
        connectedThread.write(AppConstants.APAGADO + "\n");	// Send "0" via Bluetooth
    }

    private void onDevice() {
        btnOn.setEnabled(false);
        btnOff.setEnabled(true);
        btnOn.setTextColor(Color.parseColor("#FFCC99"));
        btnOff.setTextColor(Color.parseColor("#FFFFFF"));
        APP_SERVICE.setDeviceStatus(Integer.parseInt(AppConstants.ENCENDIDO));
        connectedThread.write(AppConstants.ENCENDIDO + "\n");	// Send "1" via Bluetooth
    }
}