
package com.hlidskialf.android;
import android.hardware.SensorManager;
import android.hardware.SensorListener;
import android.content.Context;
import java.lang.System;

public class ShakeManager implements SensorListener
{
    public static final float SHAKE_THRESHOLD = 0.5f;
    public static final int SHAKE_TIMEOUT = 600;

    public interface ShakeListener {
        abstract public void onShake(int direction);
    }

    private SensorManager mManager;
    private ShakeListener mListener;
    private int mSensorMask;

    private long mMinTs[] = {0,0,0},mMaxTs[] = {0,0,0}; 
    private boolean mMinHit[] = {false,false,false},mMaxHit[] = {false,false,false};

    public ShakeManager(Context context)
    {
        mManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mSensorMask = SensorManager.SENSOR_ACCELEROMETER;
    }

    public void resume() {
        if (mListener != null)
            mManager.registerListener(this, mSensorMask);
    }
    public void stop() {
        if (mListener != null)
            mManager.unregisterListener(this);
    }

    public boolean registerListener(ShakeListener listener)
    {
        if (mListener != null) return false;
        mListener = listener;
        return true;
    }
    public void unregisterListener(ShakeListener listener)
    {
        mListener = null;
    }

    public void onAccuracyChanged(int sensor, int accuracy) 
    {
    }

    public void onSensorChanged(int sensor, float[] values)  
    {
        //android.util.Log.v("sensorChanged", String.valueOf(sensor) +":: "+ String.valueOf(values[0]) +","+ String.valueOf(values[0]) +","+ String.valueOf(values[0]) );

        if (mListener == null) return;

        int i;
        for (i=0; i < 3; i++) {
            if (check_shake_dir(i, values[i])) {
                mListener.onShake(i);
                android.util.Log.v("sensor",String.valueOf(i));
            }
        }
    }

    private boolean check_shake_dir(int idx, float value)
    {
        if (Math.abs(value) >= SHAKE_THRESHOLD) {
            if (value < 0) {
                mMinHit[idx] = true;
                mMinTs[idx] = System.currentTimeMillis();
            }
            else {
                mMaxHit[idx] = true;
                mMaxTs[idx] = System.currentTimeMillis();
            }

            if (mMinHit[idx] && mMaxHit[idx]) {
                if (Math.abs(mMinTs[idx] - mMaxTs[idx]) < SHAKE_TIMEOUT) {
                    return true;
                }
                mMinTs[idx] = mMaxTs[idx] = 0;
                mMinHit[idx] = mMaxHit[idx] = false;
            }
        }
        return false;
    }
}
