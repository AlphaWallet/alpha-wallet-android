package com.alphawallet.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.internal.RealmProxyMediator;

@Implements(RealmProxyMediator.class)
public class ShadowRealmProxyMediator
{
    @Implementation
    public boolean transformerApplied() {
        return true;
    }
}
