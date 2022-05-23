package com.alphawallet.app.repository.entity;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokens.TokenInfo;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import timber.log.Timber;

public class RealmWalletToken extends RealmObject {

    @PrimaryKey
    String address;
    private long addedTime;
    private long updatedTime;
    private String balance;
    private boolean isEnabled;
    private long lastBlockRead;
    private long earliestTxBlock;
    private boolean visibilityChanged;
    private long lastTxTime;

    public String getTokenAddress() {
        String tAddress = address;
        if (tAddress.contains(".")) //base chain
        {
            return tAddress.split(".")[0];
        }
        else if (tAddress.contains("-"))
        {
            return tAddress.split("-")[0];
        }
        else
        {
            return address;
        }
    }

    public long getChainId()
    {
        long chainID = 1;
        try
        {
            String tAddress = address;
            if (tAddress.contains(".")) //base chain
            {
                chainID = Long.parseLong(tAddress.split(".")[1]);
            }
            else if (tAddress.contains("-"))
            {
                chainID = Long.parseLong(tAddress.split("-")[1]);
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "getChainID: ");
        }
        return chainID;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(long addedTime) {
        this.addedTime = addedTime;
    }

    public long getAssetUpdateTime() {
        return updatedTime;
    }
    public void setAssetUpdateTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public long getLastBlockRead() {
        return lastBlockRead;
    }

    public void setLastBlockRead(long lastBlockRead) {
        this.lastBlockRead = lastBlockRead;
    }

    public long getEarliestTxBlock() {
        return earliestTxBlock;
    }

    public void setEarliestTxBlock(long earliestTxBlock) {
        this.earliestTxBlock = earliestTxBlock;
    }

    public boolean isVisibilityChanged() {
        return visibilityChanged;
    }

    public void setVisibilityChanged(boolean visibilityChanged) {
        this.visibilityChanged = visibilityChanged;
    }

    public void updateTokenInfoIfRequired(TokenInfo tokenInfo)
    {
        if (!isEnabled && tokenInfo.isEnabled)
        {
            isEnabled = true;
            visibilityChanged = false;
        }
    }

    public void populate(RealmToken realmToken)
    {
        setAddedTime(realmToken.getUpdateTime());
        setAssetUpdateTime(realmToken.getAssetUpdateTime());
        setBalance(realmToken.getBalance());
        setEnabled(realmToken.getEnabled());
        setLastBlockRead(realmToken.getLastBlock());
        setEarliestTxBlock(realmToken.getEarliestTransactionBlock());
        setVisibilityChanged(realmToken.isVisibilityChanged());
    }
}
