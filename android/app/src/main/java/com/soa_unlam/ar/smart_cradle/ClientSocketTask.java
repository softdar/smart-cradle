package com.soa_unlam.ar.smart_cradle;

import android.os.AsyncTask;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Created by A646241 on 21/05/2017.
 */

public class ClientSocketTask extends AsyncTask<Void, Void, Void> {


    private String dstAddress;
    private int dstPort;
    private String response = "";
    private TextView textResponse;

    public ClientSocketTask(String addr, int port, TextView textResponse){
        this.dstAddress = addr;
        this.dstPort = port;
        this.textResponse = textResponse;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Socket socket = null;

        try {
            socket = new Socket(dstAddress, dstPort);

            /* Enviando datos al servidor */
            PrintStream output = new PrintStream(socket.getOutputStream());
            output.println("Hello World!");

            /* Haciendo la recepción de datos */
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        textResponse.setText(response);
    }
}
