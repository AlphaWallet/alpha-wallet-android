package com.alphawallet.app.repository.entity;

import android.text.TextUtils;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.TokenInfo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmToken extends RealmObject {
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private int decimals;
    private long addedTime;
    private long updatedTime;
    private long lastTxTime;
    private String balance;
    private boolean isEnabled;
    private int tokenId;
    private int interfaceSpec;
    private String auxData;
    private long lastBlockRead;
    private long chainId;
    private long earliestTxBlock;
    private boolean visibilityChanged;
    private String erc1155BlockRead;

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
}

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public long getUpdateTime() {
        return addedTime;
    }

    public void setUpdateTime(long addedTime) {
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
        addedTime = System.currentTimeMillis();
    }

    public boolean getEnabled() {
        return isEnabled;
    }

    public boolean isEnabled() { return isEnabled; }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public int getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(int tokenId)
    {
        this.tokenId = tokenId;
    }

    public int getInterfaceSpec()
    {
        return interfaceSpec;
    }

    public ContractType getContractType()
    {
        int typeOrdinal = interfaceSpec;
        if (typeOrdinal > ContractType.CREATION.ordinal()) typeOrdinal = ContractType.NOT_SET.ordinal();
        return ContractType.values()[typeOrdinal];
    }

    public void setInterfaceSpec(int interfaceSpec)
    {
        this.interfaceSpec = interfaceSpec;
    }

    public String getAuxData()
    {
        return auxData;
    }

    public void setAuxData(String auxData)
    {
        this.auxData = auxData;
    }

    public void setLastBlock(long lastBlockCheck)
    {
        this.lastBlockRead = lastBlockCheck;
    }
    public long getLastBlock() { return lastBlockRead; }

    public long getChainId() { return chainId; }
    public void setChainId(long chainId) { this.chainId = chainId; }

    public long getLastTxTime()
    {
        return lastTxTime;
    }

    public void setLastTxTime(long lastTxTime)
    {
        this.lastTxTime = lastTxTime;
    }

    public long getEarliestTransactionBlock()
    {
        return earliestTxBlock;
    }

    public void setEarliestTransactionBlock(long earliestTransactionBlock)
    {
        this.earliestTxBlock = earliestTransactionBlock;
    }

    public boolean isVisibilityChanged()
    {
        return visibilityChanged;
    }

    public void setVisibilityChanged(boolean visibilityChanged)
    {
        this.visibilityChanged = visibilityChanged;
    }

    public void updateTokenInfoIfRequired(TokenInfo tokenInfo)
    {
        //check decimal integrity, if received a non-18 decimals, this is most likely an update correction from etherscan
        if (tokenInfo.decimals != decimals && (tokenInfo.decimals > 0 && (decimals == 0 || decimals == 18))
                || (!TextUtils.isEmpty(tokenInfo.name) && !tokenInfo.name.equals(name))
                || (!TextUtils.isEmpty(tokenInfo.symbol) && !tokenInfo.symbol.equals(symbol)))
        {
            setName(tokenInfo.name);
            setSymbol(tokenInfo.symbol);
            setDecimals(tokenInfo.decimals);
        }

        if (!isEnabled && tokenInfo.isEnabled)
        {
            isEnabled = true;
            visibilityChanged = false;
        }
    }

    public BigInteger getErc1155BlockRead()
    {
        if (erc1155BlockRead != null && erc1155BlockRead.length() > 0)
        {
            return new BigInteger(erc1155BlockRead, Character.MAX_RADIX);
        }
        else
        {
            return BigInteger.ZERO;
        }
    }

    public void setErc1155BlockRead(BigInteger erc1155BlockRead)
    {
        this.erc1155BlockRead = erc1155BlockRead.toString(Character.MAX_RADIX);
    }
}
