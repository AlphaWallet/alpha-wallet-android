package com.alphawallet.shadows;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.RealmManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import io.realm.Realm;
import io.realm.RealmQuery;

@Implements(RealmManager.class)
public class ShadowRealmManager
{
    @Implementation
    public Realm getRealmInstance(String walletAddress)
    {
        Realm realm = mock(Realm.class);
        RealmAuxData auxData = mock(RealmAuxData.class);
        RealmQuery realmQuery = mock(RealmQuery.class);
        doReturn(realmQuery).when(realmQuery).equalTo(anyString(), anyString());
        doReturn(auxData).when(realmQuery).findFirst();
        doReturn(realmQuery).when(realm).where(RealmAuxData.class);
        return realm;
    }
}
