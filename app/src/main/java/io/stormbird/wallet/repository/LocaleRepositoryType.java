package io.stormbird.wallet.repository;

import android.content.Context;

import java.util.ArrayList;

import io.stormbird.wallet.entity.LocaleItem;

public interface LocaleRepositoryType {
    String getDefaultLocale();

    void setDefaultLocale(Context context, String locale);

    ArrayList<LocaleItem> getLocaleList(Context context);
}
