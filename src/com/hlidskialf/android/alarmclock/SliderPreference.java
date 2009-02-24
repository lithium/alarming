package com.hlidskialf.android.alarmclock;

import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.Context;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;

public class SliderPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
  private SeekBar mSeekBar;
  private TextView mTextView;
  private int mValue = 0;
  private OnSliderChangedListener mListener;

  private int mZeroText, mMessageText, mUnitsText;

  public interface OnSliderChangedListener {
    public int getValue();
    public void onSliderChanged(int value);
    public int progressToValue(int progress);
    public int valueToProgress(int value);

  }

  void setOnSliderChangedListener(OnSliderChangedListener listener) { mListener = listener; }

  public SliderPreference(Context context, AttributeSet attrs) { 
    super(context,attrs); 

    mZeroText = attrs.getAttributeResourceValue(null,"zeroText",R.string.empty_string);
    mUnitsText = attrs.getAttributeResourceValue(null,"unitsText",R.string.empty_string);
    mMessageText = attrs.getAttributeResourceValue(null,"dialogMessage",R.string.empty_string);
  }

  protected void updateText()
  {
    if (mValue == 0) {
      mTextView.setText(mZeroText);
    }
    else {
      mTextView.setText(String.valueOf(mValue) + " " + getContext().getString(mUnitsText));
    }
  }


  @Override
  protected void onBindDialogView(View view) {
    TextView descr = (TextView)view.findViewById(R.id.slider_message);
    descr.setText(mMessageText);

    mTextView = (TextView)view.findViewById(R.id.slider_text);

    mSeekBar = (SeekBar)view.findViewById(R.id.slider_seekbar);
    mSeekBar.setOnSeekBarChangeListener(this);
    mSeekBar.setProgress( mListener.valueToProgress(mListener.getValue()) );

    updateText();
  }
  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
  {
    mValue = mListener.progressToValue(value);
    updateText();
  }
  public void onStartTrackingTouch(SeekBar seek) {}
  public void onStopTrackingTouch(SeekBar seek) {}


  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      mListener.onSliderChanged(mValue);
    } else {
      mValue = mListener.getValue();  
    }
  }

/*
  private int translateToSnooze(int progress) { return (int)(60.0*(progress / 100.0)); }
  private int translateToProgress(int snooze) { return (int)(100.0*(snooze/60.0)); }
  */
}
