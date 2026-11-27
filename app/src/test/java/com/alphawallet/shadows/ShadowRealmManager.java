package com.alphawallet.shadows;

import static org.mockito.Mockito.mock;

import com.alphawallet.app.service.RealmManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.kotlin.Realm;

@Implements(RealmManager.class)
public class ShadowRealmManager
{
    @Implementation
    public Realm getRealmInstance(String walletAddress)
    {
        return mock(Realm.class);
    }
}
