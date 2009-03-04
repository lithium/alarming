package com.hlidskialf.android.alarmclock;


import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
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

import android.os.Handler;
import java.lang.Runnable;

import android.view.animation.AnimationUtils;


public class NightClock extends Activity  implements View.OnClickListener, ViewSwitcher.ViewFactory
{
  private TextSwitcher mSwitcher;
  private Calendar mCal;
  private Handler mHandler;
  private Runnable mCallback;
  private LayoutInflater mInflater;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    getWindow().requestFeature(Window.FEATURE_NO_TITLE);

    mInflater = getLayoutInflater();

    setContentView(R.layout.nightclock);

    mSwitcher = (TextSwitcher)findViewById(R.id.nightclock_time);
    mSwitcher.setFactory(this);
    mSwitcher.setInAnimation( AnimationUtils.loadAnimation(this, android.R.anim.fade_in) );
    mSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, android.R.anim.fade_out) );
    //mSwitcher.setInAnimation( AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left) );
    //mSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right) );
    mSwitcher.setOnClickListener(this);

    mCal = Calendar.getInstance();

    updateClock();

    mCallback = new Runnable() { 
      public void run() {
        NightClock.this.updateClock();
        mHandler.postDelayed(mCallback, 1000);
      }
    };
    mHandler = new Handler();
    mHandler.postDelayed(mCallback, 1000);
  }
  @Override
  public void onStop() {
    super.onStop();
    if (mHandler != null && mCallback != null) 
      mHandler.removeCallbacks(mCallback);
  }
  public void updateClock()
  {
    mCal.setTime(new Date());
    String time = Alarms.formatTimeWithSeconds(this, mCal);
    mSwitcher.setText(time == null ? "null" : time);
  }

  public View makeView() {
    return mInflater.inflate(R.layout.nightclock_text, null);
    /*
    TextView tv = new TextView(NightClock.this);
    tv.setTextSize(85);
    return tv;
    */
  }

  public void onClick(View v) {
    finish();
  }

}
