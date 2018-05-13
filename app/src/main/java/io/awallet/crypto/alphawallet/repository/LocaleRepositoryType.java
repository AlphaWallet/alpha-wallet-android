package io.awallet.crypto.alphawallet.repository;

import android.content.Context;

import java.util.ArrayList;

import io.awallet.crypto.alphawallet.entity.LocaleItem;

public interface LocaleRepositoryType {
    String getDefaultLocale();

    String getDefaultLocaleCode();

    void setDefaultLocale(Context context, String locale, String localeCode);

    ArrayList<LocaleItem> getLocaleList(Context context);
}
