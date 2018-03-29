package com.example.acer.fuckyoubitch;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Acer on 28-03-2018.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionService";
    private static final String appNmae = "MYAPP";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }
    //This thread runs while listening for incoming connections, it behaves like a server-side client, it runs until a connection is accepted, or until cancelled
    private class AcceptThread extends Thread {
        //local server socket
        private final BluetoothServerSocket mServersocket;

        @SuppressLint("LongLogTag")
        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            //creates a listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appNmae, MY_UUID_INSECURE);
                Log.d(TAG, "AcptThread:Setting up server using" + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "Accepthread: IOException " + e.getMessage());
            }
            mServersocket = tmp;
        }
        @SuppressLint("LongLogTag")
        public void run(){
            Log.d(TAG, "run: AcceptThread: running");

            BluetoothSocket socket = null;

            try {
                //This is a blocking call and will only return on a succesfull connection or an exception
                Log.d(TAG, "run: RFCOM server socket start");

                socket = mServersocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection");
            } catch (IOException e) {
                Log.e(TAG, "Accepthread: IOException " + e.getMessage());
            }

            if(socket != null){
                connected(socket, mDevice);
            }

            Log.d(TAG, "End acceptThread");
        }

        @SuppressLint("LongLogTag")
        public void cancel(){
            Log.d(TAG, "cancel: canceling acceptthread");

            try {
                mServersocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close of accepthread server socket failed " + e.getMessage());
            }
        }
    }

    //This thread runs while attempting to make an outgoing  connection with a device, it runs straight through, the connection either fails or succeed.
    private class ConnectThread extends Thread{
        private BluetoothSocket msocket;
        @SuppressLint("LongLogTag")
        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started");
            mDevice = device;
            deviceUUID = uuid;
        }

        @SuppressLint("LongLogTag")
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "run: ConnectThread");

            //get a bluetoothsocket for a connection with the given bluetoothdevice
            try {
                Log.d(TAG, "Connectthread: Trying to create insecurerfcomsocket using UUID: " + MY_UUID_INSECURE);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "connectThread: could not create insecureifcomsocket " + e.getMessage());
            }

            msocket = tmp;

            //Always cancel discovery, it takes alot of memory
            mBluetoothAdapter.cancelDiscovery();

            //make a connection to the bluetooth socket
            //This is a blocking call and will only return on a succesfull connection or an exception
            try {
                msocket.connect();

                Log.d(TAG, "run: connectthread connected");
            } catch (IOException e) {
                try {
                    msocket.close();
                    Log.e(TAG, "run: Closed socket");
                } catch (IOException e1) {
                    Log.e(TAG, "ConnectThread: unable to close connection in socket" + e1.getMessage());
                }
                Log.d(TAG, "Connectthread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }
            connected(msocket, mDevice);
        }
        @SuppressLint("LongLogTag")
        public void cancel(){
            Log.d(TAG, "cancel: closing client socket");

            try {
                Log.d(TAG, "cancel: closing client socket");
                msocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close of msocket in connectthread failed " + e.getMessage());
            }
        }
    }

    //start acceptthread to begin a session in listening(server) mode. called by the activity onresyme()
    @SuppressLint("LongLogTag")
    public synchronized void start(){
        Log.d(TAG, "start");

        //cancel any thread attempting to make a connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    //Acceptthread starts and waits for a connection, then connectthread starts and attempts to make a connection with other devices.
    @SuppressLint("LongLogTag")
    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: started");

        mProgressDialog = ProgressDialog.show(mContext, "connecting bluetooth", "please wait", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket msocket;
        private final InputStream mInputstream;
        private final OutputStream mOutputStream;

        @SuppressLint("LongLogTag")
        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: Starting");

            msocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is made
            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                e.printStackTrace();
            }


            try {
                tmpIn = msocket.getInputStream();
                tmpOut = msocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInputstream = tmpIn;
            mOutputStream = tmpOut;
        }

        @SuppressLint("LongLogTag")
        public void run(){
            byte[] buffer = new byte[1024]; //buffer for stream
            int bytes; //bytes returned from read()

            //keep listening to the inputstream until an exception happens
            while(true){
                //read from inputstream
                try {
                    bytes = mInputstream.read(buffer);
                    String incomingdata = new String(buffer, 0, bytes);
                    Log.d(TAG, "Inputstream: " + incomingdata);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingdata);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);

                } catch (IOException e) {
                    Log.e(TAG, "write: error reading from inputstreAm " + e.getMessage());
                    break;
                }
            }
        }

        //call this from the main activity to send data to the romote device
        @SuppressLint("LongLogTag")
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: writing to output stream " + text);
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: error writing to output streAm " + e.getMessage());
            }
        }

        //Call this from the main activity to cancel the connection
        public void cancel(){
            try {
                msocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("LongLogTag")
    private void connected(BluetoothSocket msocket, BluetoothDevice mDevice) {
        Log.d(TAG, "connected: startinf");

        //start the thread to manage the connection
        mConnectedThread = new ConnectedThread(msocket);
        mConnectedThread.start();
    }

    //write to the connectedthread in an unsynchronized way
    @SuppressLint("LongLogTag")
    public void write(byte[] out){
        //create temporary object
        ConnectedThread r;

        //synchronize a copy of the connectedThread
        Log.d(TAG, "write: write called");
        //perform the write
        mConnectedThread.write(out);
    }

}
