/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hlidskialf.android.alarmclock;

import com.hlidskialf.android.captcha.CaptchaDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.SeekBar;

import java.util.Calendar;
import java.util.GregorianCalendar;



/**
 * AlarmClock application.
 */
public class AlarmClock extends Activity {

    final static String PREFERENCES = "Alarming";
    final static int SET_ALARM = 1;
    final static int SET_PREFERENCES = 2;
    final static String PREF_CLOCK_FACE = "face";
    final static String PREF_SHOW_CLOCK = "show_clock";

    final static int MENU_ITEM_EDIT=1;
    final static int MENU_ITEM_DELETE=2;
    final static int MENU_ITEM_FIRE=3;

    /** Cap alarm count at this number */
    final static int MAX_ALARM_COUNT = 12;

    /** This must be false for production.  If true, turns on logging,
        test code, etc. */
    final static boolean DEBUG = false;

    private SharedPreferences mPrefs;
    private LayoutInflater mFactory;
    private ViewGroup mClockLayout;
    private View mClock = null;
    private MenuItem mAddAlarmItem;
    private MenuItem mToggleClockItem;
    //private MenuItem mAboutItem;
    private MenuItem mSettingsItem;
    private ListView mAlarmsList;
    private Cursor mCursor;

    private boolean mCaptchaDismiss;
    /**
     * Which clock face to show
     */
    private int mFace = -1;

    /*
     * FIXME: it would be nice for this to live in an xml config file.
     */
    final static int[] CLOCKS = {
        R.layout.clock_basic_bw,
        R.layout.clock_googly,
        R.layout.clock_droid2,
        R.layout.clock_droids,
        R.layout.digital_clock
    };

    private class AlarmTimeAdapter extends CursorAdapter {
        public AlarmTimeAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = mFactory.inflate(R.layout.alarm_time, parent, false);
            DigitalClock digitalClock = (DigitalClock)ret.findViewById(R.id.digitalClock);
            digitalClock.setLive(false);
            if (Log.LOGV) Log.v("newView " + cursor.getPosition());
            return ret;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final int id = cursor.getInt(Alarms.AlarmColumns.ALARM_ID_INDEX);
            final int hour = cursor.getInt(Alarms.AlarmColumns.ALARM_HOUR_INDEX);
            final int minutes = cursor.getInt(Alarms.AlarmColumns.ALARM_MINUTES_INDEX);
            final Alarms.DaysOfWeek daysOfWeek = new Alarms.DaysOfWeek(
                    cursor.getInt(Alarms.AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX));
            final boolean enabled = cursor.getInt(Alarms.AlarmColumns.ALARM_ENABLED_INDEX) == 1;
            final String name = cursor.getString(Alarms.AlarmColumns.ALARM_NAME_INDEX);



            CheckBox onButton = (CheckBox)view.findViewById(R.id.alarmButton);
            onButton.setChecked(enabled);
            onButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        boolean isChecked = ((CheckBox) v).isChecked();
                        if (isChecked) {
                            SetAlarm.popAlarmSetToast(
                                    AlarmClock.this, hour, minutes, daysOfWeek);
                        }
                        if (mPrefs.getInt(Alarms.PREF_SNOOZE_ID, -1) != 0) { //not quick snooze 
                          Alarms.enableAlarm(AlarmClock.this, id, isChecked);
                        }
                    }
            });

            DigitalClock digitalClock = (DigitalClock)view.findViewById(R.id.digitalClock);
            if (Log.LOGV) Log.v("bindView " + cursor.getPosition() + " " + id + " " + hour +
                                ":" + minutes + " " + daysOfWeek.toString(context, true) + " dc " + digitalClock);

            digitalClock.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (true) {
                            Intent intent = new Intent(AlarmClock.this, SetAlarm.class);
                            intent.putExtra(Alarms.ID, id);
                            startActivityForResult(intent, SET_ALARM);
                        } else {
                            // TESTING: immediately pop alarm
                            Intent fireAlarm = new Intent(AlarmClock.this, AlarmAlert.class);
                            fireAlarm.putExtra(Alarms.ID, id);
                            fireAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(fireAlarm);
                        }
                    }
                });

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minutes);
            digitalClock.updateTime(c);
            TextView daysOfWeekView = (TextView) digitalClock.findViewById(R.id.daysOfWeek);
            daysOfWeekView.setText(daysOfWeek.toString(AlarmClock.this, false));

            TextView alarmName = (TextView)digitalClock.findViewById(R.id.alarmName);
            alarmName.setText(name);


            // Build context menu
            digitalClock.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View view,
                                                    ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(Alarms.formatTime(AlarmClock.this, c));
                        MenuItem editAlarmItem = menu.add(0, id, MENU_ITEM_EDIT, R.string.edit_alarm);
                        MenuItem deleteAlarmItem = menu.add(0, id, MENU_ITEM_DELETE, R.string.delete_alarm);
                        MenuItem fireAlarmItem = menu.add(0, id, MENU_ITEM_FIRE, R.string.fire_alarm);
                    }
                });
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getOrder()) {
            case MENU_ITEM_EDIT:
                Intent intent = new Intent(AlarmClock.this, SetAlarm.class);
                intent.putExtra(Alarms.ID, item.getItemId());
                startActivityForResult(intent, SET_ALARM);
                break;
            case MENU_ITEM_DELETE:
                Alarms.deleteAlarm(this, item.getItemId());
                updateEmptyVisibility(mAlarmsList.getAdapter().getCount() - 1);
                break;
            case MENU_ITEM_FIRE:
                Alarms.enableAlert(this, item.getItemId(), System.currentTimeMillis());
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // sanity check -- no database, no clock
        if (getContentResolver() == null) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error))
                    .setMessage(getString(R.string.dberror))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }})
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create().show();
            return;
        }

        setContentView(R.layout.alarm_clock);
        mFactory = LayoutInflater.from(this);
        mPrefs = getSharedPreferences(PREFERENCES, 0);

        mCursor = Alarms.getAlarmsCursor(getContentResolver());
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        mAlarmsList.setAdapter(new AlarmTimeAdapter(this, mCursor));
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setItemsCanFocus(true);

        updateEmptyVisibility(mCursor.getCount());

        mClockLayout = (ViewGroup) findViewById(R.id.clock_layout);
        mClockLayout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final Intent intent = new Intent(AlarmClock.this, ClockPicker.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        mClockLayout.setOnLongClickListener( new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
            
                if (!mPrefs.getBoolean("bigclock_enable", true)) return false;

                    final Intent intent = new Intent(AlarmClock.this, BigClock.class);
                    startActivity(intent);
                    return true;
            }
        });

        setVolumeControlStream(android.media.AudioManager.STREAM_ALARM);

        setClockVisibility(mPrefs.getBoolean(PREF_SHOW_CLOCK, true));

        mCaptchaDismiss = mPrefs.getBoolean("captcha_on_dismiss", false);

        updateQuickAlarmVisibility();
    }
    public void quick_alarm_dialog()
    {
      ViewGroup layout = (ViewGroup)mFactory.inflate(R.layout.slider_dialog, null);
      final TextView text_value = (TextView)layout.findViewById(R.id.slider_text);
      final SeekBar slider = (SeekBar)layout.findViewById(R.id.slider_seekbar);
      slider.setMax(59);
      text_value.setText("1 minutes"); 
      slider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
          text_value.setText(String.valueOf( value+1 )+" minutes"); 
        }
        public void onStartTrackingTouch(SeekBar seek) {}
        public void onStopTrackingTouch(SeekBar seek) {}
      });
      AlertDialog d = new AlertDialog.Builder(this)
        .setTitle(R.string.quick_alarm)
        .setView(layout)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.about_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              int snooze_min = 1 + slider.getProgress();
              final long snoozeTarget = System.currentTimeMillis() + 1000 * 60 * snooze_min;
              long nextAlarm = Alarms.calculateNextAlert(AlarmClock.this).getAlert();
              if (nextAlarm < snoozeTarget) {
                // alarm set to trigger before your snooze...
              } 
              else {
                Alarms.saveSnoozeAlert(AlarmClock.this, 0, snoozeTarget);
                Alarms.setNextAlert(AlarmClock.this);
                Toast.makeText(AlarmClock.this,
                               getString(R.string.alarm_alert_snooze_set, snooze_min),
                               Toast.LENGTH_LONG).show();

                updateSnoozeVisibility();
              }
            }
        })
        .create();
      d.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setClockVisibility(mPrefs.getBoolean(PREF_SHOW_CLOCK, true));
        updateEmptyVisibility( mAlarmsList.getAdapter().getCount() );

        int face = mPrefs.getInt(PREF_CLOCK_FACE, 0);
        if (mFace != face) {
            if (face < 0 || face >= AlarmClock.CLOCKS.length)
                mFace = 0;
            else
                mFace = face;
            inflateClock();
        }

        
        updateSnoozeVisibility();
    }
    private void updateSnoozeVisibility()
    {
        long next_snooze = mPrefs.getLong(Alarms.PREF_SNOOZE_TIME, 0);
        View v = (View)findViewById(R.id.snooze_message);
        if (next_snooze != 0) {
            TextView tv = (TextView)v.findViewById(R.id.snooze_message_text);
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(next_snooze);
            String snooze_time = Alarms.formatTime(AlarmClock.this, c);
            tv.setText(getString(R.string.snooze_message_text, snooze_time));

            v.setOnClickListener(dismiss_snooze_listener);
            v.setVisibility(View.VISIBLE);
        }
        else {
            v.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
        mCursor.deactivate();
    }

    protected void inflateClock() {
        if (mClock != null) {
            mClockLayout.removeView(mClock);
        }
        mClock = mFactory.inflate(CLOCKS[mFace], null);
        mClockLayout.addView(mClock, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        mAddAlarmItem = menu.add(0, 0, 0, R.string.add_alarm);
        mAddAlarmItem.setIcon(android.R.drawable.ic_menu_add);

        //mToggleClockItem = menu.add(0, 0, 0, R.string.hide_clock);
        //mToggleClockItem.setIcon(R.drawable.ic_menu_clock_face);
        mToggleClockItem = menu.add(0, 0, 0, R.string.bigclock);
        mToggleClockItem.setIcon(R.drawable.ic_menu_clock_face);

        mSettingsItem = menu.add(0, 0, 0, R.string.preferences);
        mSettingsItem.setIcon(android.R.drawable.ic_menu_preferences);

        //mAboutItem = menu.add(0, 0, 0, R.string.about);
        //mAboutItem.setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }

    /**
     * Only allow user to add a new alarm if there are fewer than
     * MAX_ALARM_COUNT
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mAddAlarmItem.setVisible(mAlarmsList.getChildCount() < MAX_ALARM_COUNT);
        //mToggleClockItem.setTitle(getClockVisibility() ? R.string.hide_clock : R.string.show_clock);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mAddAlarmItem) {
            Uri uri = Alarms.addAlarm(getContentResolver(), this);
            // FIXME: scroll to new item.  mAlarmsList.requestChildRectangleOnScreen() ?
            String segment = uri.getPathSegments().get(1);
            int newId = Integer.parseInt(segment);
            if (Log.LOGV) Log.v("In AlarmClock, new alarm id = " + newId);
            Intent intent = new Intent(AlarmClock.this, SetAlarm.class);
            intent.putExtra(Alarms.ID, newId);
            startActivityForResult(intent, SET_ALARM);
            updateEmptyVisibility(1);
            return true;
        } else if (item == mToggleClockItem) {
            /*
            setClockVisibility(!getClockVisibility());
            saveClockVisibility();
            return true;
            */
            startActivityForResult(new Intent(this, BigClock.class), 0);
        
        /*} else if (item == mAboutItem) {
                View v = getLayoutInflater().inflate(R.layout.about_dialog,null);
                AlertDialog dia = new AlertDialog.Builder(this).
                                    setTitle(R.string.about_title).
                                    setView(v).
                                    setPositiveButton(R.string.about_ok,null).
                                    create();
                dia.show();
        */
    	  } else if (item == mSettingsItem) {
            Intent intent = new Intent(AlarmClock.this, AlarmClockPreferences.class)
                                  .setAction(Intent.ACTION_MAIN)
                                  .addCategory(Intent.CATEGORY_PREFERENCE);
            startActivityForResult(intent, SET_PREFERENCES); 
            return true;
        }


        return false;
    }
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data)
    {
      updateQuickAlarmVisibility();
    }

    private boolean getClockVisibility() {
        return mClockLayout.getVisibility() == View.VISIBLE;
    }

    private void setClockVisibility(boolean visible) {
        mClockLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void saveClockVisibility() {
        mPrefs.edit().putBoolean(PREF_SHOW_CLOCK, getClockVisibility()).commit();
    }
    private void updateEmptyVisibility(int count) {
        View v = findViewById(R.id.alarms_list_empty);
        if (v != null) 
          v.setVisibility(count < 1 ? View.VISIBLE : View.GONE);
    }
    private void updateQuickAlarmVisibility() {
      boolean visible = mPrefs.getBoolean("quickalarm_enable", true); 

      View quick_alarm = findViewById(R.id.quick_alarm);
      if (quick_alarm != null) {
        quick_alarm.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible)
          quick_alarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              quick_alarm_dialog();
            }
          });
      }
    }

    private View.OnClickListener dismiss_snooze_listener = new View.OnClickListener() { 
      public void onClick(View clicked) {
        final View v = clicked;
        if (mCaptchaDismiss) {
          final CaptchaDialog d = new CaptchaDialog(AlarmClock.this, getString(R.string.captcha_message), 0, 3, true);
          d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dia) {
              if (!d.isComplete()) return;
              v.setVisibility(View.GONE);
              Alarms.disableSnoozeAlert(AlarmClock.this);
              Toast.makeText(AlarmClock.this, getString(R.string.snooze_dismissed), Toast.LENGTH_LONG).show();
              Alarms.setNextAlert(AlarmClock.this);
            }
          });
          d.show();
        }
        else {
          clicked.setVisibility(View.GONE);
          Alarms.disableSnoozeAlert(AlarmClock.this);
          Toast.makeText(AlarmClock.this, getString(R.string.snooze_dismissed), Toast.LENGTH_LONG).show();
          Alarms.setNextAlert(AlarmClock.this);
        }
        
      }
  };
}
