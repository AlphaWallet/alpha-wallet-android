package com.alphawallet.app.repository;

import android.content.Context;

import java.util.ArrayList;

import com.alphawallet.app.entity.LocaleItem;

public interface LocaleRepositoryType {
    String getDefaultLocale();

    void setDefaultLocale(Context context, String locale);

    ArrayList<LocaleItem> getLocaleList(Context context);
}
