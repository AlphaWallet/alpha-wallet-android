package com.alphawallet.shadows;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.Realm;

@Implements(Realm.class)
public class ShadowRealm
{

    @Implementation
    public static synchronized void init(Context context) {
    }
}