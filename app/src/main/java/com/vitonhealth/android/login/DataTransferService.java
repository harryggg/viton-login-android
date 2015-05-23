package com.vitonhealth.android.login;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

public class DataTransferService extends Service {
    private static final String TAG = "DataTransferService";
    private GoogleApiClient mGoogleApiClient;
    private static int startAttemptCount = 0;
    private static int attemptCount = 0;
    private static int terminationCount = 0;

    private AlarmManager alarmMgr;
    @Override
    public void onCreate(){
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
        Log.i(TAG,"created");
        mGoogleApiClient.connect();
        Log.i(TAG,""+mGoogleApiClient.isConnecting());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        if (intent.getAction()=="TERMINATION"){ //action to terminate the service
            Log.i(TAG,"TERMINATION");
            sendMessageToGetDataFromWatch(); //get the remaining data
            sendMessageToStopService();
            setNextAlarm(0);

            //scheduleTaskExecutor.shutdownNow();
            stopSelf();
        }else if(intent.getAction()=="REQUEST_DATA"){ //action to send message to request data
            Log.i(TAG,"fileTransfer");
            Log.i(TAG, "send message");
            sendMessageToGetDataFromWatch();
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            setNextAlarm(1);
          /*  if (hour>17){
                stopSelf();
            }else {
                setNextAlarm(1);
                stopSelf();
            }*/
        }
        else{
            Log.i(TAG,"initialize");
            sendMessageToStartService();
            startAttemptCount = 0;
            setNextAlarm(1);





        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
       return null;
    }
    public void sendMessageToGetDataFromWatch() {

        if (mGoogleApiClient.isConnected()){
            Log.i(TAG,"connected");

            new Thread (new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                    for (final Node node : nodes.getNodes()){
                        Log.i(TAG, "Node: " + node.getId());
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Flags.START_ACTIVITY_PATH_GETDATA, "Retrieve measurement".getBytes()).await();
                        if (!result.getStatus().isSuccess()){
                            Log.e(TAG,"msg not sent");
                            //Toast.makeText(getParent(), "test", Toast.LENGTH_LONG).show();
                        } else {
                            Log.i(TAG,"msg sent");
                        }
                    }
                }
            }).start();
        }else {
            Log.i(TAG,"attempt:"+attemptCount);
            if (attemptCount> Flags.MAXRETRY){
                Log.e(TAG,"tried 5 times to request,give up");
                attemptCount = 0;
            }else {
                try {
                    Thread.sleep(((int) (1000 * Math.pow(2, attemptCount))));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                attemptCount++;
                Log.i(TAG, "not connected,restart,attempts: "+attemptCount);

                Context context = this.getApplicationContext();

                Intent myIntent1 = new Intent(context, DataTransferService.class);
                myIntent1.setAction("REQUEST_DATA");
                context.startService(myIntent1);
            }
        }
        //saveFile();
    }
    public void sendMessageToStopService() {
        Log.i(TAG,"shut down");


        if (mGoogleApiClient.isConnected()){
            Log.i(TAG,"connected");

            new Thread (new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                    for (final Node node : nodes.getNodes()){
                        Log.i(TAG, "Node: " + node.getId());
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Flags.START_ACTIVITY_PATH_STOP, "Stop data measurement".getBytes()).await();
                        if (!result.getStatus().isSuccess()){
                            Log.e(TAG,"msg not sent");
                            //Toast.makeText(getParent(), "test", Toast.LENGTH_LONG).show();
                        } else {
                            Log.i(TAG,"msg sent");
                        }
                    }
                }
            }).start();
        }else {
            Log.i(TAG,"terminationcount:"+terminationCount);
            if (terminationCount>Flags.MAXRETRY){
                Log.e(TAG,"tried 5 times to terminate,give up");
                terminationCount = 0;
            }else {
                /*try {
                    Thread.sleep(((int) (1000 * Math.pow(2, terminationCount))));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                terminationCount++;
                Log.i(TAG, "not connected,restart,attempts: "+terminationCount);

                Context context = this.getApplicationContext();

                Intent myIntent1 = new Intent(context, DataTransferService.class);
                myIntent1.setAction("REQUEST_DATA");
                context.startService(myIntent1);
            }
        }
        //saveFile();
    }

    public void sendMessageToStartService() {
        if (mGoogleApiClient.isConnected()){
            Log.i(TAG,"connected");

            new Thread (new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                    for (final Node node : nodes.getNodes()){
                        Log.i(TAG, "Node: " + node.getId());
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Flags.START_ACTIVITY_PATH_START, "Stop data measurement".getBytes()).await();
                        if (!result.getStatus().isSuccess()){
                            Log.e(TAG,"msg not sent");
                        } else {
                            Log.i(TAG,"start msg sent");
                        }
                    }
                }
            }).start();
        }else{
            Log.i(TAG,"attemptstartcount:"+startAttemptCount);
            if (startAttemptCount>Flags.MAXRETRY){
                Log.e(TAG,"try to start for 5 times, give up");
                startAttemptCount = 0;
            }else {
                /*try {
                    Thread.sleep(((int) (1000 * Math.pow(2, startAttemptCount))));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                startAttemptCount++;
                Log.e(TAG, "not connected,restarting,attempt: "+startAttemptCount);
                Context context = this.getApplicationContext();

                Intent myIntent1 = new Intent(context, DataTransferService.class);
                context.startService(myIntent1);
            }

        }
        //saveFile();
    }



    public void setNextAlarm(int onOff){
        //0 is off, 1 is on


        Context context = this.getApplicationContext();
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataTransferService.class);
        intent.setAction("REQUEST_DATA");
        PendingIntent alarmIntent = PendingIntent.getService(context, 0, intent, 0);
        if (onOff==1) {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() +
                            Flags.UPDATEINTERVAL, alarmIntent);
            Log.i(TAG,"setNextAlarm");
        }else{
            Log.i(TAG,"cancelNextAlarm");
            alarmMgr.cancel(alarmIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Destroyed");
    }
}


