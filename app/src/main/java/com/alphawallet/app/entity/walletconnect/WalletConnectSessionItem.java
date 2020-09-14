package com.alphawallet.app.entity.walletconnect;

import com.alphawallet.app.repository.entity.RealmWCSession;

/**
 * Created by JB on 9/09/2020.
 */
public class WalletConnectSessionItem
{
    public final String name;
    public final String url;
    public final String icon;
    public final String sessionId;
    public final String localSessionId;

    public WalletConnectSessionItem(RealmWCSession s)
    {
        name = s.getRemotePeerData().getName();
        url = s.getRemotePeerData().getUrl();
        icon = s.getRemotePeerData().getIcons().get(0);
        sessionId = s.getSession().getTopic();
        localSessionId = s.getSessionId();
    }
}