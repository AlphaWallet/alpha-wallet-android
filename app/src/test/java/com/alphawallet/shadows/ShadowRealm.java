package com.alphawallet.shadows;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import com.alphawallet.app.realm.RealmCompat;

@Implements(RealmCompat.class)
public class ShadowRealm
{
    @Implementation
    public static void init(Context context) {
    }
}
