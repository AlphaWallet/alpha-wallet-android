package com.alphawallet.app.repository;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.util.LocaleUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.alphawallet.app.entity.LocaleItem;

public class LocaleRepository implements LocaleRepositoryType {
    private static final String[] LOCALES = {
            "en",
            "zh",
            "es",
            "fr",
            "vi",
            "my"
    };

    private final PreferenceRepositoryType preferences;

    public LocaleRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setLocale(Context context, String locale) {
        LocaleUtils.setLocale(context, locale);
    }

    @Override
    public String getUserPreferenceLocale()
    {
        return preferences.getUserPreferenceLocale();
    }

    @Override
    public void setUserPreferenceLocale(String locale)
    {
        preferences.setUserPreferenceLocale(locale);
    }

    @Override
    public String getActiveLocale()
    {
        String useLocale = preferences.getUserPreferenceLocale();
        if (TextUtils.isEmpty(useLocale)) useLocale = preferences.getDefaultLocale();
        return useLocale;
    }

    @Override
    public ArrayList<LocaleItem> getLocaleList(Context context) {
        ArrayList<LocaleItem> list = new ArrayList<>();
        for (String locale : LOCALES) {
            Locale l = new Locale(locale);
            list.add(new LocaleItem(LocaleUtils.getDisplayLanguage(locale, getActiveLocale()), locale));
        }
        return list;
    }

    @Override
    public boolean isLocalePresent(String locale) {
        return Arrays.asList(LOCALES).contains(locale);
    }
}
