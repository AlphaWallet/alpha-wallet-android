package com.alphawallet.app.interact;

import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.walletconnect.web3.wallet.client.Wallet;
import com.walletconnect.web3.wallet.client.Web3Wallet;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class WalletConnectInteract
{
    @Inject
    public WalletConnectInteract()
    {

    }

    public int getSessionsCount()
    {
        return getSessions().size();
    }

    public List<WalletConnectSessionItem> getSessions()
    {
        List<WalletConnectSessionItem> result = new ArrayList<>(getWalletConnectV2SessionItems());

        //now sort for active/newness
        result.sort((l, r) -> Long.compare(r.expiryTime, l.expiryTime));

        return result;
    }

    public void fetchSessions(SessionFetchCallback sessionFetchCallback)
    {
        fetch(sessionFetchCallback);
    }

    private void fetch(SessionFetchCallback sessionFetchCallback)
    {
        List<WalletConnectSessionItem> result = new ArrayList<>(getWalletConnectV2SessionItems());
        sessionFetchCallback.onFetched(result);
    }

    private List<WalletConnectSessionItem> getWalletConnectV2SessionItems()
    {
        List<WalletConnectSessionItem> result = new ArrayList<>();
        try
        {
            List<Wallet.Model.Session> listOfSettledSessions = Web3Wallet.INSTANCE.getListOfActiveSessions();
            for (Wallet.Model.Session session : listOfSettledSessions)
            {
                result.add(new WalletConnectV2SessionItem(session));
            }
        }
        catch (IllegalStateException e)
        {
            Timber.e(e);
        }
        return result;
    }

    public interface SessionFetchCallback
    {
        void onFetched(List<WalletConnectSessionItem> sessions);
    }
}
