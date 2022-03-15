package com.alphawallet.app.interact;

import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.repository.entity.RealmWCSession;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class WalletConnectInteract
{
    private final RealmManager realmManager;

    @Inject
    public WalletConnectInteract(RealmManager realmManager)
    {
        this.realmManager = realmManager;
    }

    public int getSessionsCount()
    {
        return getSessions().size();
    }

    public List<WalletConnectSessionItem> getSessions()
    {
        List<WalletConnectSessionItem> result = new ArrayList<>();
        result.addAll(getWalletConnectV1SessionItems());
        result.addAll(getWalletConnectV2SessionItems());
        return result;
    }

    private List<WalletConnectSessionItem> getWalletConnectV1SessionItems()
    {
        List<WalletConnectSessionItem> sessions = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(WalletConnectViewModel.WC_SESSION_DB))
        {
            RealmResults<RealmWCSession> items = realm.where(RealmWCSession.class)
                    .sort("lastUsageTime", Sort.DESCENDING)
                    .findAll();

            for (RealmWCSession r : items)
            {
                sessions.add(new WalletConnectSessionItem(r));
            }
        }

        return sessions;
    }

    private List<WalletConnectSessionItem> getWalletConnectV2SessionItems()
    {
        List<WalletConnectSessionItem> result = new ArrayList<>();
        List<WalletConnect.Model.SettledSession> listOfSettledSessions = WalletConnectClient.INSTANCE.getListOfSettledSessions();
        for (WalletConnect.Model.SettledSession session : listOfSettledSessions)
        {
            result.add(new WalletConnectV2SessionItem(session));
        }
        return result;
    }
}
