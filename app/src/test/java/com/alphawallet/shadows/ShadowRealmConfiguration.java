package com.alphawallet.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.RealmConfiguration;

@Implements(RealmConfiguration.class)
public class ShadowRealmConfiguration
{
    @Implementation
    public RealmConfiguration build()
    {
        return
                new RealmConfiguration.Builder().
                        inMemory().
                        name("xxxxx").build();
    }
}

