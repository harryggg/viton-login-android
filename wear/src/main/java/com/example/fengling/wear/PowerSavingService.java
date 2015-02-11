package com.example.fengling.wear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class PowerSavingService extends Service implements SensorEventListener{
    private static final String TAG = "PowerSavingService";
    private static SensorManager mSensorManager;
    private static Sensor countSensor;
    public long stepUpdateTime = SystemClock.elapsedRealtime();
    public long previousTime = SystemClock.elapsedRealtime();
    public static float previousStep ;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"powersaving on");
        previousTime = SystemClock.elapsedRealtime();
        stepUpdateTime = SystemClock.elapsedRealtime();
        mSensorManager = ((SensorManager) this.getSystemService(SENSOR_SERVICE));
        countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_STEP_COUNTER:
                Log.i(TAG, "step detected" + (event.values[0]));
                if (SystemClock.elapsedRealtime() - stepUpdateTime < 1000 * 10) break;
                stepUpdateTime = SystemClock.elapsedRealtime();
                float temp = previousStep;
                previousStep = event.values[0];
                Toast.makeText(this, "step: " + event.values[0], Toast.LENGTH_LONG).show();
                if (event.values[0] - temp < 100) {  //<100
                    //start power save mode
                    Log.i(TAG, "not running");
                    Context context = this.getApplicationContext();
                    startService(new Intent(this, MyService.class));
                    stopSelf();

                }else{
                    Log.i(TAG, "still running");
                }
                break;
            default:
                Log.i(TAG,event.sensor.getType()+"detected");
                break;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
        mSensorManager.unregisterListener(this);
    }
}
