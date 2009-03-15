package com.hlidskialf.android.captcha;

import com.hlidskialf.android.alarmclock.R;

import android.app.Dialog;
import android.widget.ImageView;
import android.widget.Button;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View;
import android.view.KeyEvent;
import java.util.Random;
import android.view.Menu;
import android.view.MenuItem;

public class CaptchaDialog extends Dialog
{
    public static final int MENUITEM_IGIVEUP = 1;
    ImageView[] mDots;
    int mSize;
    long mCompleteMask,mDotState;
    boolean mCancellable;

    private final Random mRandom = new Random();
    private final View.OnClickListener mImageClicker = new View.OnClickListener() {
      public void onClick(View v) { select_dot(v); }
    };
    public CaptchaDialog(Context context) { 
      super(context); init(context, "Tap the yellow dots to continue", 0, 4, true); 
    }
    public CaptchaDialog(Context context, String title) { 
      super(context); init(context, title, 0, 4, true); 
    }
    public CaptchaDialog(Context context, String title, long complete_mask) {
      super(context); init(context, title, complete_mask, 4, true);
    }
    public CaptchaDialog(Context context, String title, long complete_mask, int size) {
      super(context); init(context, title, complete_mask, size, true);
    }
    public CaptchaDialog(Context context, String title, long complete_mask, int size, boolean cancellable) {
      super(context); init(context, title, complete_mask, size, cancellable);
    }
    private void init(Context context, String title, long complete_mask, int size, boolean cancellable) {
      setTitle(title);
      setContentView(R.layout.captcha);

      Button b = (Button)findViewById(R.id.captcha_dismiss);
      if (cancellable) {
        b.setOnClickListener(new Button.OnClickListener() {
          public void onClick(View v) {
            CaptchaDialog.this.dismiss();
          }
        });
      }
      else {
        b.setVisibility(View.GONE);
      }
  
      mCancellable = cancellable;
      mSize = Math.max(Math.min(size,4),2);
      mDots = new ImageView[mSize*mSize];
      mCompleteMask = (complete_mask != 0) ? complete_mask : random_complete_mask();
      mDotState = 0;
      get_views();
    }
    @Override
    public boolean onKeyDown(int code, KeyEvent evt)
    {
      if (!mCancellable && code == KeyEvent.KEYCODE_BACK) return true;

      return super.onKeyDown(code,evt);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
      MenuItem mi = menu.add(0,0,0,"I Give Up!");

      mi.setIcon(android.R.drawable.ic_dialog_alert);
      mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          public boolean onMenuItemClick(MenuItem item) { dismiss(); return true; }
      });
      return super.onCreateOptionsMenu(menu);
    }

    private long random_complete_mask() {
      return mRandom.nextLong() & (long)(Math.pow(2,mSize*mSize)-1);
    }
    private void get_views()
    {
      ViewGroup holder = (ViewGroup)findViewById(R.id.captcha_dots);
      int i;
      int d=0;
      int r=0;
      int m = holder.getChildCount();
      for (i=0; (r < mSize) && i < m; i++, r++) {
        View v = holder.getChildAt(i);
        if (!(v instanceof ViewGroup)) continue; 
        int m2 = ((ViewGroup)v).getChildCount();
        int i2;
        int r2 = 0;
        for (i2=0; (r2 < mSize) && i2<m2; i2++, r2++) {
          View v2 = ((ViewGroup)v).getChildAt(i2);
          if (!(v2 instanceof ImageView)) continue; 
          mDots[d]  = (ImageView)v2;
          mDots[d].setVisibility(View.VISIBLE);
          mDots[d].setSelected(false);
          mDots[d].setImageResource(is_dot_needed(d) ? R.drawable.captcha_dot_needed_off : R.drawable.captcha_dot_off);
          mDots[d].setOnClickListener(mImageClicker);
          d++;
        }
      }
    }
    private void update_dots()
    {
      mDotState = 0;
      int i;
      for (i=0; i < mSize*mSize; i++) {
        boolean selected = mDots[i].isSelected();
        set_dot(i, selected);
        mDots[i].setImageResource(is_dot_needed(i) ?
            (selected ? R.drawable.captcha_dot_needed_on : R.drawable.captcha_dot_needed_off) :
            (selected ? R.drawable.captcha_dot_on : R.drawable.captcha_dot_off)
        );
      }

      if (mDotState == mCompleteMask) {
        dismiss();
      }
    }
    public boolean isComplete() 
    {
      return (mDotState == mCompleteMask);
    }
    private boolean is_dot_needed(int i)
    {
      return (mCompleteMask & (1<<i)) == (1<<i);
    }
    private void set_dot(int i, boolean val)
    {
      mDotState = val ? mDotState|(1<<i) : mDotState&~(1<<i);
    }

    private void select_dot(View v) {
        if (!(v instanceof ImageView)) return;
        ImageView iv = (ImageView)v;
        boolean selected = !iv.isSelected();
        iv.setSelected( selected );

        update_dots(); 
    }

}
