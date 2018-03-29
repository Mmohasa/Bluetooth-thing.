package com.example.acer.fuckyoubitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothadapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView listView;
    Button startconnection;
    BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    TextView incomingdata;
    StringBuilder messages;

    BluetoothDevice mBTDevice;



    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothadapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothadapter.ERROR);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onreceive: State off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onreceive: State turning off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onreceive: State on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onreceive: State turning off");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(mBluetoothadapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, mBluetoothadapter.ERROR);

                switch (mode){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discover enabled");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discover Disabled, able to receive connection");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discover Disabled, not able to receive connection");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Conected");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: Action found");
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                listView.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: Action found");
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //case 1 bonded already
                if(mdevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "mBroadcastReceiver4: Bond bonded");
                    mBTDevice = mdevice;
                }
                //case 2: making a bond
                if(mdevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "mBroadcastReceiver4: Bond bonding");
                }
                //case 3 breaking a bond
                if(mdevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "mBroadcastReceiver4: Bond none");
                }

            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnonoff = findViewById(R.id.btnonoff);
        Button btndiscover = findViewById(R.id.btndiscover);
        listView = findViewById(R.id.listview);
        mBTDevices = new ArrayList<>();
        startconnection = findViewById(R.id.startconnection);
        incomingdata = findViewById(R.id.incomingdata);
        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("theMessage"));

        listView.setOnItemClickListener(MainActivity.this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothadapter = BluetoothAdapter.getDefaultAdapter();

        btnonoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onclick enabling/disabling BT");
                enabledisableBT();
            }
        });
        startconnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");

            messages.append(text + "\n");

            incomingdata.setText(messages);
        }
    };

    //The app will crash if it isnt paired with the device first
    public void startConnection(){
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }
    public void enabledisableBT(){
        if(mBluetoothadapter == null){
            Log.d(TAG, "enableDisableBT: Dont have bt");
        }
        if(!mBluetoothadapter.isEnabled()){
            Log.d(TAG, "enabledisableBT: Enabling BT");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothadapter.isEnabled()){
            Log.d(TAG, "enabledisableBT: disabling BT");
            mBluetoothadapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }

    public void btnEnableDisable_discover(View view) {
        Log.d(TAG, "btnEnableDisable_discover: making device discover 30 seconds");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothadapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    public void Discover(View view) {
        Log.d(TAG, "Discover: Looking for unpaired devices");

        if(mBluetoothadapter.isDiscovering()){
            mBluetoothadapter.cancelDiscovery();
            Log.d(TAG, "Discover: Canceling discovery");

            mBluetoothadapter.startDiscovery();
            IntentFilter discoverDeviceIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDeviceIntent);
        }
        if(!mBluetoothadapter.isDiscovering()){
            mBluetoothadapter.startDiscovery();
            IntentFilter discoverDeviceIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDeviceIntent);
        }
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "startBTConnection: Initiating RFCOM Bluetooth connection");

            mBluetoothConnection.startClient(device, uuid);

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothadapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: you cliked a device");
        String devicename = mBTDevices.get(i).getName();
        String deviceAdress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: devicename = " + devicename);
        Log.d(TAG, "onItemClick: deviceadress = " + deviceAdress);

        mBTDevices.get(i).createBond();
        mBTDevice = mBTDevices.get(i);
        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Ondestroy called");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }



}
