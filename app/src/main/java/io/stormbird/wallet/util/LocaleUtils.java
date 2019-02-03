package io.stormbird.wallet.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

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
}
