package com.example.fengling.wear;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class watchWearableListenerService extends WearableListenerService{

    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "watch service";



    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        Log.i(TAG, "msg received");
        Log.i(TAG,String.valueOf(messageEvent.getPath()));
        //Toast.makeText(this, "msg received", Toast.LENGTH_LONG).show();
        String s = messageEvent.getPath();



        if (s.equals(Flags.START_ACTIVITY_PATH_START)) {
            startService(new Intent(this, MyService.class));
            Log.i(TAG, "measurement started");

        } else if (s.equals(Flags.START_ACTIVITY_PATH_STOP)) {
            stopService(new Intent(this, MyService.class));
            Log.i(TAG, "measurement stopped");
        }else if(s.equals(Flags.START_ACTIVITY_PATH_GETDATA)){
            Log.i(TAG,"prepare to send file to phone");
            sendFile();
        } else {
            Log.e(TAG,"unknown command received. command is "+s);
        }
    }

   private float checkBatteryLevel() {
        Context context = this.getApplicationContext();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;
        return batteryPct;
    }

    public void sendFile() {
        float batteryLevel = checkBatteryLevel();
        Log.i(TAG, "battery level:" + batteryLevel);
       /* if (batteryLevel < 0.20) {
            //Toast.makeText(this, "Battery Level too low", Toast.LENGTH_LONG).show();
            Log.i(TAG, "battery level low");
            stopService(new Intent(this, MyService.class));

            return;

        } else {*/

            //first read all files in the folder rawdata
            File folder = new File((getFilesDir().getAbsoluteFile() + "/rawData/"));
            if (!folder.exists()) {
                folder.mkdir();
                Log.i(TAG, "folder is in place");
            } else {
                Log.i(TAG, "folder is already there");
            }
            // Toast.makeText(this, "sending file", Toast.LENGTH_LONG).show();
            //read all of the files into string and send to phone
            File[] listOfFiles = folder.listFiles();
            final ArrayList<String> dataToPhone = new ArrayList<String>();
            for (int i = 0; i < listOfFiles.length; i++) {
                Log.i(TAG, "reading data from file " + listOfFiles[i].getName());
                try {
                    FileInputStream fstream = new FileInputStream(listOfFiles[i].getAbsoluteFile());
                    DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        dataToPhone.add(strLine);
                        Log.i(TAG, strLine);
                    }
                    br.close();
                } catch (Exception e) {
                    Log.i(TAG, "error in reading file " + listOfFiles[i].getName());
                    Log.i(TAG, String.valueOf(e));
                }
            }


            //send data to phone
            //first connect to phone
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.i(TAG, "onConnected: " + connectionHint);
                            // Now you can use the data layer API
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.i(TAG, "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.e(TAG, "onConnectionFailed: " + result);
                        }
                    })
                    .addApi(Wearable.API)
                    .build();
            //start connection
            mGoogleApiClient.connect();

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {

                    //send buffer data to phone
                    PutDataMapRequest dataMap = PutDataMapRequest.create(Flags.BUFFERDATA_PATH);
                    dataMap.getDataMap().putStringArrayList(Flags.BUFFERDATA_PATH, dataToPhone);
                    PutDataRequest request = dataMap.asPutDataRequest();
                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "failed to send buffer data");
                                // TODO : actions to store and resend the data
                            } else {
                                //Log.i(TAG, "package sent");
                            }
                        }
                    });
                    return null;
                }
            }.execute();

            //delete all files
            for (int i = 0; i < listOfFiles.length; i++) {
                listOfFiles[i].delete();
            }

    }


}
