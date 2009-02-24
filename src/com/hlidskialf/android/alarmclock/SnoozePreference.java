
package com.hlidskialf.android.alarmclock;
import android.app.AlertDialog.Builder;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.Context;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;

public class SnoozePreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
  private SeekBar mSeekBar;
  private TextView mTextView;
  private int mSnoozeDuration = 0;
  private OnSnoozeChangedObserver mOnSnoozeChangedObserver;

  public interface OnSnoozeChangedObserver {
    public int getSnooze();
    public void onSnoozeChanged(int duration);
  }

  void setOnSnoozeChangedObserver(OnSnoozeChangedObserver onSnoozeChangedObserver) {
    mOnSnoozeChangedObserver = onSnoozeChangedObserver;
  }

  public SnoozePreference(Context context, AttributeSet attrs) { 
    super(context,attrs); 
  }

  @Override
  protected void onBindDialogView(View view) {
    mTextView = (TextView)view.findViewById(R.id.snooze_text);

    mSeekBar = (SeekBar)view.findViewById(R.id.snooze_seek);
    mSeekBar.setOnSeekBarChangeListener(this);
    mSeekBar.setProgress( translateToProgress(mOnSnoozeChangedObserver.getSnooze()) );
  }
  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
  {
    value = translateToSnooze(value);
    mSnoozeDuration = value;
    if (value == 0) {
      mTextView.setText(R.string.snooze_disabled);
    }
    else {
      mTextView.setText(String.valueOf(value) + " minutes");
    }
  }
  public void onStartTrackingTouch(SeekBar seek) {}
  public void onStopTrackingTouch(SeekBar seek) {}


  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      mOnSnoozeChangedObserver.onSnoozeChanged(mSnoozeDuration);
    } else {
      mSnoozeDuration = mOnSnoozeChangedObserver.getSnooze();  
    }
  }


  private int translateToSnooze(int progress) { return (int)(60.0*(progress / 100.0)); }
  private int translateToProgress(int snooze) { return (int)(100.0*(snooze/60.0)); }
}
