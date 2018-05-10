package io.awallet.crypto.alphawallet.repository;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Locale;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.LocaleItem;

public class LocaleRepository implements LocaleRepositoryType {
    private final PreferenceRepositoryType preferences;

    public LocaleRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setDefaultLocale(Context context, String locale, String localeCode) {
        preferences.setDefaultLocale(locale);
        preferences.setDefaultLocaleCode(localeCode);
        setLocale(context, localeCode);
    }

    public String getDefaultLocaleCode() {
        return preferences.getDefaultLocaleCode();
    }

    public String getDefaultLocale() {
        return preferences.getDefaultLocale();
    }

    public void setLocale(Context context, String localeCode) {
        Locale myLocale = new Locale(localeCode);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    @Override
    public ArrayList<LocaleItem> getLocaleList(Context context) {
        ArrayList<LocaleItem> list = new ArrayList<>();
        list.add(new LocaleItem(context.getString(R.string.lang_en), "en"));
        list.add(new LocaleItem(context.getString(R.string.lang_zh), "zh"));
        list.add(new LocaleItem(context.getString(R.string.lang_es), "es"));
        return list;
    }
}
