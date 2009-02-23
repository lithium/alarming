
package com.hlidskialf.android.text.format;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.content.Context;
import android.provider.Settings;

public class DateFormat 
{
  public static String format(String f, Calendar c)
  {
    SimpleDateFormat sdf = new SimpleDateFormat(f);
    return sdf.format(c.getTime());
  }

  public static boolean is24HourFormat(Context context)
  {
    String value = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
    
    if (value == null) {
      java.text.DateFormat natural = java.text.DateFormat.getTimeInstance(java.text.DateFormat.LONG, context.getResources().getConfiguration().locale);

      if (natural instanceof SimpleDateFormat) {
        SimpleDateFormat sdf = (SimpleDateFormat) natural;
        String pattern = sdf.toPattern();

        if (pattern.indexOf('H') >= 0) {
          return true;
        } else {
          return false;
        }
      }
    }

    boolean b24 =  !(value == null || value.equals("12"));
    return b24;

  }
}
