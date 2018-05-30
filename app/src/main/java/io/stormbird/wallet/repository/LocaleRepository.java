package io.stormbird.wallet.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.Locale;

import io.stormbird.wallet.entity.LocaleItem;
import io.stormbird.wallet.util.LocaleUtils;

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
