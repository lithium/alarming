package com.hlidskialf.android.alarmclock;


import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.app.Activity;
import android.view.LayoutInflater;
import java.util.Calendar;
import java.util.Date;
import android.view.KeyEvent;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;

import android.os.PowerManager;
import android.os.BatteryManager;
import android.os.Handler;
import java.lang.Runnable;

import android.view.animation.AnimationUtils;


public class BigClock extends Activity  implements View.OnClickListener, ViewSwitcher.ViewFactory
{
  private static final int MENUITEM_FLIP=1;
  private static final int MENUITEM_CLOSE=2;

  private TextSwitcher mSwitcher;
  private Calendar mCal;
  private Handler mHandler;
  private Runnable mCallback;
  private LayoutInflater mInflater;

  private int mOrient;
  private boolean mDoWakeLock;
  private PowerManager mPower;
  private BatteryManager mBattery;
  private BroadcastReceiver mBatteryReceiver;
  private static PowerManager.WakeLock mWakeLock;
  private SharedPreferences mPrefs;


  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    getWindow().requestFeature(Window.FEATURE_NO_TITLE);

    mInflater = getLayoutInflater();

    setContentView(R.layout.bigclock);

    mSwitcher = (TextSwitcher)findViewById(R.id.bigclock_time);
    mSwitcher.setFactory(this);
    mSwitcher.setInAnimation( AnimationUtils.loadAnimation(this, android.R.anim.fade_in) );
    mSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, android.R.anim.fade_out) );
    //mSwitcher.setInAnimation( AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left) );
    //mSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right) );
    mSwitcher.setOnClickListener(this);

    mPrefs = getSharedPreferences(AlarmClock.PREFERENCES, 0);

    mOrient = mPrefs.getInt("bigclock_orientation", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    setRequestedOrientation(mOrient);

    mDoWakeLock = mPrefs.getBoolean("bigclock_wake_lock", false);
    if (mDoWakeLock) {
        mPower = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPower.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, BigClock.class.getName());
    }

    mCal = Calendar.getInstance();
    mHandler = new Handler();
    mBatteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
           int plugged = intent.getIntExtra("plugged", 0);
           if (mWakeLock == null) return;
           if ((plugged == BatteryManager.BATTERY_PLUGGED_AC) || (plugged ==  BatteryManager.BATTERY_PLUGGED_USB) )
           {
               if (!mWakeLock.isHeld()) {
                  mWakeLock.acquire();
                  android.util.Log.v("BigClock","wakelock/acquire-plugged");
               }
           } else {
               if (mWakeLock.isHeld()) {
                  mWakeLock.release();
                  android.util.Log.v("BigClock","wakelock/release-unplug");
               }
           }
        }
    }; 
  }
  @Override 
  protected void onDestroy() {
    if (mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
      android.util.Log.v("BigClock","wakelock/release-destroy");
    }
    super.onDestroy();
  }
  @Override
  public void onResume() {
    super.onResume();

    if (mDoWakeLock && mBatteryReceiver != null) {
      registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    mCallback = new Runnable() { 
      public void run() {
        BigClock.this.updateClock();
        mHandler.postDelayed(mCallback, 1000);
      }
    };
    mHandler.postDelayed(mCallback, 0);
  }
  @Override
  public void onPause() {
    cleanup();
    super.onPause();
  }
  public void updateClock()
  {
    mCal.setTime(new Date());
    String time = Alarms.formatTimeWithSeconds(this, mCal);
    mSwitcher.setText(time == null ? "null" : time);
  }

  public View makeView() {
    return mInflater.inflate(R.layout.bigclock_text, null);
  }

  public void onClick(View v) { finish(); }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuItem mi = menu.add(0,MENUITEM_FLIP,0, R.string.flip_orientation);
    mi.setIcon(android.R.drawable.ic_menu_always_landscape_portrait);
    mi = menu.add(0,MENUITEM_CLOSE,0, R.string.hide_clock);
    mi.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    switch (id)  {
    case MENUITEM_FLIP:
      int orient = getRequestedOrientation();
      orient = (orient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ? 
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      if (orient != mOrient) {
        setRequestedOrientation(mOrient = orient);
        mPrefs.edit().putInt("bigclock_orientation", mOrient).commit();
      }
      return true;
    case MENUITEM_CLOSE:
      cleanup();
      finish();
      return true;
    }
    return false;
  }


  public boolean onKeyDown(int keyCode, KeyEvent event) { 
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      return super.onKeyDown(keyCode,event);
    }

    
    cleanup();
    finish(); 

    return true; 
  }

  public void cleanup() {
    try {
    if (mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
      android.util.Log.v("BigClock","wakelock/release-cleanup");
    }
    if (mDoWakeLock && mBatteryReceiver != null) {
        unregisterReceiver(mBatteryReceiver);
    }
    if (mHandler != null && mCallback != null) {
      mHandler.removeCallbacks(mCallback);
      mCallback = null;
    }
    } catch (Exception e) {
      //just cleanup gracefully
      android.util.Log.v("Alarming!BigClock", "cleanup exception!");
    }
  }

}
