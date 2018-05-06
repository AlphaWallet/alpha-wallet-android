package io.awallet.crypto.alphawallet.repository;

import android.content.Context;

import java.util.ArrayList;

import io.awallet.crypto.alphawallet.entity.Language;

public interface LanguageRepositoryType {
    String getDefaultLanguage();

    String getDefaultLanguageCode();

    void setDefaultLanguage(Context context, String language, String languageCode);

    ArrayList<Language> getLanguageList(Context context);
}
