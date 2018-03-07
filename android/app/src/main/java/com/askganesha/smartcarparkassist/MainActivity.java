package com.askganesha.smartcarparkassist;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice connected_device;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case 0:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Unable to turn bluetooth on", Toast.LENGTH_SHORT).show();
                    findBT();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((Button) findViewById(R.id.start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    findBT();
                    SelectDevice();
                    beginListenForData();
                } catch (Exception e){
                    Log.d("jatin", "onCreate: " + e.toString());
                }
            }
        });

    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    List<BluetoothDevice> getDevices(){
        List<BluetoothDevice> devices = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

        Iterator<BluetoothDevice> iterator;
        Log.e("jatin", "Started");
        for (iterator = bondedDevices.iterator(); iterator.hasNext();){
            BluetoothDevice device = iterator.next();
            devices.add(device);
        }

        if (bondedDevices.size() <= 0){
            Toast.makeText(this, "No Appropriate paired devices", Toast.LENGTH_SHORT).show();
        }
        return devices;

    }

    void SelectDevice(){
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select the device");
            final List<BluetoothDevice> devices = getDevices();
            CharSequence[] devicesChars = new CharSequence[devices.size()];
            for (int i=0; i<devices.size(); i++){
                devicesChars[i] = devices.get(i).getName();
            }
            builder.setItems(devicesChars, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface,final int i) {
                    Timer timer = new Timer();
                    final int[] flag = {0};
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (flag[0] != 1){
                                    flag[0] = 1;
                                    connected_device = devices.get(i);
                                    ParcelUuid[] uuids = connected_device.getUuids();
                                    BluetoothSocket socket = connected_device.createInsecureRfcommSocketToServiceRecord(uuids[0].getUuid());
                                    socket.connect();
                                    mmInputStream = socket.getInputStream();
                                } else {
                                    this.cancel();
                                }
                            } catch (IOException e){
                                Toast.makeText(MainActivity.this, "Error Occured: " + e.toString(), Toast.LENGTH_SHORT).show();
                                this.cancel();
                            }
                        }
                    }, 0, 5000);

                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        } else {
            Toast.makeText(this, "Unable to Connect !", Toast.LENGTH_SHORT).show();
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.d("jatin", "run: " + data);
                                            // data contains the line
                                            String res[] = data.split(", ");
                                            int left = Integer.parseInt(res[0]);
                                            int right = Integer.parseInt(res[1]);

                                            if (left < 10) {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#ff0000"));
                                            } else if (left < 40) {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#FFFFAA00"));
                                            } else {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#FF1AFF00"));
                                            }

                                            if (right < 10) {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#ff0000"));
                                            } else if (right < 40) {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#FFFFAA00"));
                                            } else {
                                                ((LinearLayout) findViewById(R.id.left)).setBackgroundColor(Color.parseColor("#FF1AFF00"));
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

}
