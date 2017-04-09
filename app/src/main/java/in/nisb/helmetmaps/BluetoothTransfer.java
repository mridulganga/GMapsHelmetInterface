package in.nisb.helmetmaps;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by mridul on 5/4/17.
 */

public class BluetoothTransfer extends AsyncTask<Void,Void,Void>{

    public BluetoothSocket btSocket = null;
    public Boolean isBtConnected = false;
    BluetoothAdapter myBluetooth = null;


    public void ToastIt(String s){
        Toast.makeText(null,s,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Void doInBackground(Void... params) {

        String address="54:14:73:55:EE:5D";
        UUID myUUID= UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
        Log.e("test","background");
        try{
            if (btSocket == null || !isBtConnected)
            {
                myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                btSocket.connect();//start connection
            }
        }catch (IOException e){}


        return null;
    }
}