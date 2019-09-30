package com.alphawallet.app.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleUtils {
    public static String getDisplayLanguage(String locale, String base) {
        Locale mLocale = new Locale(locale);
        Locale intermediate = new Locale(base); //ensure we get the language name in the correct language eg 'English', 'Inglis' etc.
        String displayLanguage = mLocale.getDisplayLanguage(intermediate);
        return displayLanguage.substring(0, 1).toUpperCase() + displayLanguage.substring(1);
    }

    public static void setLocale(Context context, String locale) {
        Locale mLocale = new Locale(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = mLocale;
        res.updateConfiguration(conf, dm);
    }

    public static Date getLocalDateFromTimestamp(long timeStampInSec) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(timeStampInSec * DateUtils.SECOND_IN_MILLIS);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        return calendar.getTime();
    }
}
