package com.wallet.crypto.trustapp.ui.widget.entity;

import android.support.annotation.NonNull;

public class ListItem {
    public final int viewType;
    public final Object value;

    public ListItem(int viewType, Object value) {
        this.viewType = viewType;
        this.value = value;
    }

    public static ListItem[] create(@NonNull Object[] values, int type) {
        int len = values.length;
        ListItem[] items = new ListItem[len];
        for (int i = 0; i < len; i++) {
            items[i] = new ListItem(type, values[i]);
        }
        return items;
    }
}
