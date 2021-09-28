package com.alphawallet.app.util;

import static com.alphawallet.app.repository.SharedPreferenceRepository.DEVICE_COUNTRY;
import static com.alphawallet.app.repository.SharedPreferenceRepository.DEVICE_LOCALE;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import com.alphawallet.app.repository.SharedPreferenceRepository;

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

    // Store device locale at app startup to read the device's setting
    public static void setDeviceLocale(Context ctx)
    {
        PreferenceManager
                .getDefaultSharedPreferences(ctx)
                .edit()
                .putString(DEVICE_LOCALE, getCurrentLanguage())
                .putString(DEVICE_COUNTRY, getCurrentCountry())
                .apply();
    }

    public static Locale getDeviceLocale(Context ctx)
    {
        String deviceLocaleStr = PreferenceManager.getDefaultSharedPreferences(ctx).getString(DEVICE_LOCALE, "en");
        String deviceCountryStr = PreferenceManager.getDefaultSharedPreferences(ctx).getString(DEVICE_COUNTRY, "US");
        return new Locale(deviceLocaleStr, deviceCountryStr);
    }

    /**
     * Set the language locale from any activity
     * @param context
     */
    public static String setActiveLocale(Context context)
    {
        String useLocale = getActiveLocaleName(context);
        // Change locale settings in the app.
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.setLocale(new Locale(useLocale));
        res.updateConfiguration(conf, dm);

        context.getApplicationContext().getResources().updateConfiguration(conf,
                context.getApplicationContext().getResources().getDisplayMetrics());

        return useLocale;
    }

    /**
     * Fetches a new Context which was created with the currently active locale;
     * This is required for menu text on the main 4 wallet pages due to initialisation issues
     * @param context
     * @return
     */
    public static Context getActiveLocaleContext(Context context) {
        String useLocale = getActiveLocaleName(context);
        Locale activeLocale = new Locale(useLocale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(activeLocale);
        configuration.setLayoutDirection(activeLocale);

        return context.createConfigurationContext(configuration);
    }

    private static String getActiveLocaleName(Context context)
    {
        String useLocale = PreferenceManager.getDefaultSharedPreferences(context).getString(SharedPreferenceRepository.USER_LOCALE_PREF, "");
        if (TextUtils.isEmpty(useLocale)) useLocale = getDeviceSettingsLocale(context);
        return useLocale;
    }

    /**
     * This method will check for existing Device OS Language set.
     * @param context To reference with "Resource"
     * @return String as a Language Locale
     */
    public static String getDeviceSettingsLocale(Context context) {
        String locale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            locale = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        else
        {
            locale = context.getResources().getConfiguration().locale.getLanguage();
        }
        return locale;
    }

    private static String getCurrentLanguage()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            return LocaleList.getDefault().get(0).getLanguage();
        }
        else
        {
            return Locale.getDefault().getLanguage();
        }
    }

    private static String getCurrentCountry()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            return LocaleList.getDefault().get(0).getCountry();
        }
        else
        {
            return Locale.getDefault().getCountry();
        }
    }
}
