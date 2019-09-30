package com.alphawallet.app.repository;

import android.content.Context;

import com.alphawallet.app.util.LocaleUtils;

import java.util.ArrayList;
import java.util.Locale;

import com.alphawallet.app.entity.LocaleItem;

public class LocaleRepository implements LocaleRepositoryType {
    private static final String[] LOCALES = {
            "en",
            "zh",
            "es"
    };

    private final PreferenceRepositoryType preferences;

    public LocaleRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setDefaultLocale(Context context, String locale) {
        preferences.setDefaultLocale(locale);
        LocaleUtils.setLocale(context, locale);
    }

    public String getDefaultLocale() {
        return preferences.getDefaultLocale();
    }

    @Override
    public ArrayList<LocaleItem> getLocaleList(Context context) {
        ArrayList<LocaleItem> list = new ArrayList<>();
        for (String locale : LOCALES) {
            Locale l = new Locale(locale);
            list.add(new LocaleItem(LocaleUtils.getDisplayLanguage(locale, getDefaultLocale()), locale));
        }
        return list;
    }
}
