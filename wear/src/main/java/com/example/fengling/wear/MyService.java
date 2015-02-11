package com.example.fengling.wear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MyService extends Service implements
        SensorEventListener{
//        GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener{

    private static final long updateInterval = 6000;        //interval for sending data to phone
    private static final long restartInterval = 20000;       //if after this duration no update in hpm, restart sensor.
    private static final String TAG = "MyService";
    private  Collection<String> nodes;
    private  int countHeartBeat = 0;
    private ArrayList<String> bufferData = new ArrayList<String>();         //buffer data to be sent to phone
    private ArrayList<String> tempBuffer = new ArrayList<String>();
    private ArrayList<Float> bufferHPM = new ArrayList<Float>();
    private ArrayList<Float> tempBufferHPM = new ArrayList<Float>();
    private ArrayList<Long> bufferTime = new ArrayList<Long>();
    private ArrayList<Long> tempBufferTime = new ArrayList<Long>();

    private PowerManager.WakeLock wl;
    //define sensors
    private static SensorManager mSensorManager;
    private static Sensor mHeartRateSensor;
    private static Sensor countSensor;
    public static float hpm = 0.0f;
    public long startTime = SystemClock.elapsedRealtime();
    public long hpmUpdateTime = SystemClock.elapsedRealtime();
    public long stepUpdateTime = SystemClock.elapsedRealtime();
    public long motionUpdateTime = SystemClock.elapsedRealtime();
    public long previousTime = SystemClock.elapsedRealtime();
    public static float previousStep = 0;

    //used for getting the handler from other class for sending messages
    public static Handler 		mMyServiceHandler 			= null;
    //used for keep track on Android running status
    public static Boolean 		mIsServiceRunning 			= false;



    @Override
    public void onCreate() {

        Log.i(TAG, "onCreate " + startTime);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction()=="TERMINATION"){
            Log.i(TAG,"TERMINATION");
            stopSelf();
        }else {
            startTime = SystemClock.elapsedRealtime();
            hpmUpdateTime = SystemClock.elapsedRealtime();
            motionUpdateTime = SystemClock.elapsedRealtime();
            stepUpdateTime = SystemClock.elapsedRealtime();
            previousTime = SystemClock.elapsedRealtime();
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wl.acquire();

            //start sensor service
            mSensorManager = ((SensorManager) this.getSystemService(SENSOR_SERVICE));
            //get heart rate sensors
            mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
           /*
            if (countSensor != null) {
                mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
            }*/
            //mHeartRateSensor = mSensorManager.getDefaultSensor(65562);  //only for Gear Live

            //start sensors
            mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);

            MyThread myThread = new MyThread();
            myThread.start();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mIsServiceRunning = true; // set service running status = true


            //Log.e("check sensor",String.valueOf(mHeartRateSensor));

            Toast.makeText(this, "Congrats! My Service Started", Toast.LENGTH_LONG).show();
            // We need to return if we want to handle this service explicitly.
            //return START_STICKY;
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (wl != null) {
            wl.release();
        }
        Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onDestroy");

        mIsServiceRunning = false; // make it false, as the service is already destroyed.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //Your inner thread class is here to getting response from Activity and processing them
    class MyThread extends Thread
    {
        private static final String INNER_TAG = "MyThread";

        public void run()
        {
            this.setName(INNER_TAG);

            // Prepare the looper before creating the handler.
            Looper.prepare();
            mMyServiceHandler = new Handler()
            {
                //here we will receive messages from activity(using sendMessage() from activity)
                public void handleMessage(Message msg)
                {
                    Log.i(TAG,"handleMessage(Message msg)" );
                    switch(msg.what)
                    {
                        case 0: // we sent message with what value =0 from the activity. here it is
                            //Reply to the activity from here using same process handle.sendMessage()
                            //So first get the Activity handler then send the message
                            if(null != MyActivity.mUiHandler)
                            {
                                //first build the message and send.
                                //put a integer value here and get it from the Activity handler
                                //For Example: lets use 0 (msg.what = 0;)
                                //for receiving service running status in the activity
                                Message msgToActivity = new Message();
                                msgToActivity.what = 0;
                                if(true ==mIsServiceRunning)
                                    //msgToActivity.obj  = "Request Received. Service is Running "+String.valueOf(motion_xyz); // you can put extra message here
                                    msgToActivity.obj = countHeartBeat;//"The uptime of the service is "+String.valueOf((SystemClock.uptimeMillis() - startTime)/1000)+" seconds";//motion_xyz;
                                else
                                    msgToActivity.obj  = "Request Received. Service is not Running"; // you can put extra message here

                                MyActivity.mUiHandler.sendMessage(msgToActivity);
                            }
                            break;

                        case 1:// command to send data to phone
                            saveFile();
                            break;

                        default:
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Update your data. This check is very raw. You should improve it when the sensor is unable to calculate the heart rate
        switch(event.sensor.getType()) {/*
            case Sensor.TYPE_STEP_COUNTER:

                Log.i(TAG,"step detected,previous"+(previousStep));
                Log.i(TAG,"value[0]:"+event.values[0]);
                if (SystemClock.elapsedRealtime() - stepUpdateTime < 1000*10) break;
                stepUpdateTime = SystemClock.elapsedRealtime();
                float temp = previousStep;
                previousStep = event.values[0];
                Log.i(TAG,""+previousStep);
                Toast.makeText(this, "step: "+event.values[0], Toast.LENGTH_LONG).show();
                if (event.values[0]-temp>100){
                    if (temp==0){
                        Log.i(TAG,"Just initialized");
                        break;
                    }
                    //start power save mode
                    Log.i(TAG,"running");
                    Context context = this.getApplicationContext();
                    context.startService(new Intent(context,PowerSavingService.class));
                    stopSelf();

                }
                break;
            */
            case Sensor.TYPE_HEART_RATE:
            //case 65562: //only for Gear Live
                if (SystemClock.elapsedRealtime() - hpmUpdateTime < 950 || event.values[0] <= - 10.0) break;
                hpmUpdateTime = SystemClock.elapsedRealtime();
                bufferHPM.add(event.values[0]);
                bufferTime.add(System.currentTimeMillis());
                countHeartBeat ++;

                if(SystemClock.elapsedRealtime() - previousTime > updateInterval){
                    previousTime = SystemClock.elapsedRealtime();
                    //copy the data to tem buffer
                    tempBufferTime = new ArrayList<Long>(bufferTime);
                    tempBufferHPM = new ArrayList<Float>(bufferHPM);
                    bufferHPM.clear();
                    bufferTime.clear();
                    saveFile();
                    //Log.i(TAG,"saving file");
                }

//                try {
//                    Thread.sleep(950);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                break;

            default:
                Log.i(TAG,event.sensor.getType()+"detected");
                break;
        }
    }

    private void restartHeartRateSensor() {
        if ((SystemClock.uptimeMillis() - hpmUpdateTime > restartInterval) && (SystemClock.uptimeMillis() - motionUpdateTime > restartInterval)){
            motionUpdateTime = SystemClock.uptimeMillis();
            mSensorManager.unregisterListener(this, mHeartRateSensor);
            SystemClock.sleep(5000);
            mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i(TAG,"heart rate sensor restarted");
        }
    }

/*    public void sendData() {

        //check if the data is empty
        if (bufferData.size() == 0) {
            Log.e(TAG, "empty dataset");
            return;
        }

        //copy the stored data into a temp memory for sending.
        tempBuffer = new ArrayList<String>(bufferData);      //convert to primitive
        Log.i(TAG,"dataset copied " + countHeartBeat + "beats");

        //check connection
        //Log.i(TAG, String.valueOf(mGoogleApiClient.isConnected()));
        //clear buffered data to collect new data
        bufferData.clear();
        //use background to send data to phone
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {

            //send buffer data to phone
        PutDataMapRequest dataMap = PutDataMapRequest.create(BUFFERDATA_PATH);
        dataMap.getDataMap().putStringArrayList(BUFFERDATA_PATH, tempBuffer);
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
                //Log.i(TAG, "TESTING");
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                //Log.i(TAG,"file sent status is "+this.getStatus());
            }
        }.execute();

        *//*
        //send msg instead of data to all connected devices - usually only one which is the phone
        for (String node : nodes) {

            //Log.i("wear", "Node: " + node);
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node, START_ACTIVITY_PATH, "Hello".getBytes()).await();
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "msg not sent");
            } else {
                Log.i(TAG, "msg sent");
            }
        }
        *//*
    }


    //get the connected phone
    private Collection<String> getNodes() {
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }*/

    private void saveFile() {

        //String fDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = sdf.format(new Date());
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        File folder = new File((getFilesDir().getAbsoluteFile() + "/rawData/"));
        if (!folder.exists()){
            folder.mkdir();
            Log.i(TAG,"folder is in place");
        }else {
            Log.i(TAG,"folder is already there");
        }

        File file = new File(folder.getAbsolutePath(), filename);

        OutputStream out = null;
        Log.i(TAG,"output file is "+file.getAbsoluteFile());

        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            for (int i=0;i<tempBufferHPM.size();i++){
                out.write((String.valueOf(sdf.format(tempBufferTime.get(i))) + " " + String.valueOf(tempBufferHPM.get(i))).getBytes());
                out.write("\n".getBytes());
            }
            Log.i(TAG,"file "+filename +" is written");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        File file = new File(filename+".txt");
//        OutputStream out = null;
//        Log.i(TAG,"output file is "+file.getAbsoluteFile());
//
//        try {
//            out = new BufferedOutputStream(new FileOutputStream(file));
//            for (int i=0;i<dataFromWatch.size();i++){
//                out.write(String.valueOf(dataFromWatch.get(i)).getBytes());
//                out.write("\n".getBytes());
//            }
//            Log.i(TAG,"file is written");
//            out.flush();
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //list files

        //File [] listOfFiles = folder.listFiles();
        //Log.i(TAG,"listing files in " + folder.getAbsoluteFile());

//        for (int i=0;i<listOfFiles.length;i++){
//            Log.i(TAG,listOfFiles[i].getName());
//        }

        //String path3 = getFilesDir().getAbsolutePath() + "/"+file.getName();
        //File f3 = new File(path3);
        //file.setReadable(true,false);

        //copy the file to public directory
        //String sourcePath = getFilesDir().getAbsolutePath();
        //String destinationPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        //copyFile(String.valueOf(sourcePath),"/test.txt",String.valueOf(destinationPath));
    }

}