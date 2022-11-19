package com.alphawallet.app.entity.walletconnect;

import com.alphawallet.app.repository.entity.RealmWCSession;

/**
 * Created by JB on 9/09/2020.
 */
public class WalletConnectSessionItem
{
    public String name = "";
    public String url = "";
    public String icon = "";
    public String sessionId;
    public String localSessionId;
    public long chainId;

    public WalletConnectSessionItem(RealmWCSession s)
    {
        if (s.getRemotePeerData() != null)
        {
            name = s.getRemotePeerData().getName();
            url = s.getRemotePeerData().getUrl();
            icon = s.getRemotePeerData().getIcons().size() > 0 ? s.getRemotePeerData().getIcons().get(0) : null;
        }
        sessionId = s.getSession().getTopic();
        localSessionId = s.getSessionId();
        chainId = s.getChainId() == 0 ? 1 : s.getChainId(); //older sessions without chainId set must be mainnet
    }

    public WalletConnectSessionItem()
    {

    }
}
