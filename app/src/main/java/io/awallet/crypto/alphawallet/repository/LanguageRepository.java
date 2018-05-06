package io.awallet.crypto.alphawallet.repository;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Locale;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Language;

public class LanguageRepository implements LanguageRepositoryType {
    private final PreferenceRepositoryType preferences;

    public LanguageRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setDefaultLanguage(Context context, String language, String languageCode) {
        preferences.setDefaultLanguage(language);
        preferences.setDefaultLanguageCode(languageCode);
        setLocale(context, languageCode);
    }

    public String getDefaultLanguageCode() {
        return preferences.getDefaultLanguageCode();
    }

    public String getDefaultLanguage() {
        return preferences.getDefaultLanguage();
    }

    public void setLocale(Context context, String languageCode) {
        Locale myLocale = new Locale(languageCode);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    @Override
    public ArrayList<Language> getLanguageList(Context context) {
        ArrayList<Language> list = new ArrayList<>();
        list.add(new Language(context.getString(R.string.lang_en), "en"));
        list.add(new Language(context.getString(R.string.lang_zh), "zh"));
        list.add(new Language(context.getString(R.string.lang_es), "es"));
        return list;
    }
}
