package com.alphawallet.shadows;

import com.alphawallet.app.service.RealmManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.Realm;
import io.realm.RealmConfiguration;

@Implements(RealmManager.class)
public class ShadowRealmManager
{
    @Implementation
    public Realm getRealmInstance(String walletAddress)
    {
        RealmConfiguration testConfig =
                new RealmConfiguration.Builder().
                        inMemory().
                        name(walletAddress).build();
        return Realm.getInstance(testConfig);
    }
}
