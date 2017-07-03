package com.soa_unlam.ar.smart_cradle;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigActivity extends Activity {

    private static final AppService APP_SERVICE = AppServiceImpl.getInstance();

    private String minTemp;
    private String maxTemp;

    private TextView textMinTemp;
    private EditText editMinTemp;

    private TextView textMaxTemp;
    private EditText editMaxTemp;

    private Button update;
    private Button cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        textMinTemp = (TextView) findViewById(R.id.textMinTemp);
        editMinTemp = (EditText) findViewById(R.id.editMinTemp);
        textMaxTemp = (TextView) findViewById(R.id.textMaxTemp);
        editMaxTemp = (EditText) findViewById(R.id.editMaxTemp);

        update = (Button) findViewById(R.id.update);
        cancel = (Button) findViewById(R.id.cancel);

        minTemp = APP_SERVICE.getMinTemp();
        maxTemp = APP_SERVICE.getMaxTemp();
        String textStrMinTemp = "Temperatura Mínima Actual: " + minTemp + " °C";
        textMinTemp.setText(textStrMinTemp);
        String textStrMaxTemp = "Temperatura Máxima Actual: " + maxTemp + " °C";
        textMaxTemp.setText(textStrMaxTemp);

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String minValue = editMinTemp.getText().toString();
                String maxValue = editMaxTemp.getText().toString();
                boolean isInputError = false;
                try {
                    Integer minTemp = Integer.valueOf(minValue);
                    Integer maxTemp = Integer.valueOf(maxValue);
                    boolean valueMin = (minTemp > 0 && minTemp < 40);
                    boolean valueMax = (maxTemp > 10 && maxTemp < 60);
                    if (minTemp > maxTemp) {
                        Toast.makeText(getBaseContext(), "La mínima no puede exceder la máxima", Toast.LENGTH_LONG).show();
                        isInputError = true;
                    } else if (!valueMin) {
                        Toast.makeText(getBaseContext(), "Mínima fuera de rango, debe estar entre 0-40", Toast.LENGTH_LONG).show();
                        isInputError = true;
                    } else if (!valueMax) {
                        Toast.makeText(getBaseContext(), "Máxima fuera de rango, debe estar entre 10-60", Toast.LENGTH_LONG).show();
                        isInputError = true;
                    }
                } catch (NumberFormatException nfe) {
                    Toast.makeText(getBaseContext(), "Debe ingresar Números", Toast.LENGTH_LONG).show();
                    isInputError = true;
                }
                if (!isInputError) {
                    String fullUpdate = "T" + minValue + "-" + maxValue;
                    ConnectedThread thread = (ConnectedThread)APP_SERVICE.getConnectedThread();
                    thread.write(fullUpdate);
                    APP_SERVICE.setMinTemp(minTemp);
                    APP_SERVICE.setMaxTemp(maxTemp);
                    Toast.makeText(getBaseContext(), "CAMBIOS REALIZADOS", Toast.LENGTH_LONG).show();
                    APP_SERVICE.setUpdateTempConfig(AppConstants.TEMP_CONFIG_STOPPED);
                    finish();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "Configuración Cancelada!", Toast.LENGTH_LONG).show();
                APP_SERVICE.setUpdateTempConfig(AppConstants.TEMP_CONFIG_STOPPED);
                finish();
            }
        });


    }
}
