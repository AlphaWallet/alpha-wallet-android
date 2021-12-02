package com.alphawallet.app.repository.entity;

import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.google.gson.Gson;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 4/09/2020.
 */
public class RealmWCSession extends RealmObject
{
    @PrimaryKey
    private String sessionId;       // This is the sessionKey, which is the 'x' part the WalletConnect code: wc:xxxx-xxxx-xxxx-xxxx@1.......
    private String peerId;          // Randomly chosen by us when we first start the session, should stay the same for subsequent connections
    private String sessionData;     // Session data, formed from the wc: connect code when we first start a session - it will contain the sessionId key
    private String remotePeerData;  // Peer data from the other end of the connection. This will contain the icon & URL for the connection information
    private String remotePeerId;    // This is the connection key we receive from the walletconnect server after we establish the first connection.
                                    //   When we reconnect this session, we use this value in the client.connect(...) call
    private int usageCount;         // How many times we used this session
    private long lastUsageTime;     // Last time we used this session
    private String walletAccount;   // Which wallet we connected this session with (note, you can add/remove available session wallets using the update API call,
                                    //   maybe add this as an advanced option).

    private long chainId;

    public WCSession getSession()
    {
        return new Gson().fromJson(sessionData, WCSession.class);
    }

    public void setSessionData(String sessionData)
    {
        this.sessionData = sessionData;
    }

    public WCPeerMeta getRemotePeerData()
    {
        return new Gson().fromJson(remotePeerData, WCPeerMeta.class);
    }

    public void setRemotePeerData(String peerData)
    {
        this.remotePeerData = peerData;
    }

    public void setRemotePeerId(String id)
    {
        this.remotePeerId = id;
    }

    public String getRemotePeerId()
    {
        return remotePeerId;
    }

    public int getUsageCount()
    {
        return usageCount;
    }

    public void setUsageCount(int usageCount)
    {
        this.usageCount = usageCount;
    }

    public long getLastUsageTime()
    {
        return lastUsageTime;
    }

    public void setLastUsageTime(long lastUsageTime)
    {
        this.lastUsageTime = lastUsageTime;
    }

    public String getWalletAccount()
    {
        return walletAccount;
    }

    public void setWalletAccount(String walletAccount)
    {
        this.walletAccount = walletAccount;
    }

    public String getPeerId()
    {
        return peerId;
    }

    public void setPeerId(String ourConnectionId)
    {
        this.peerId = ourConnectionId;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public long getChainId()
    {
        return chainId;
    }

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }
}
