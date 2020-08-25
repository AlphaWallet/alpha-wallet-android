package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.ContractType;

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
    private int chainId;
    private long earliestTxBlock;

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

    public long getTXUpdateTime() {
        return updatedTime;
    }
    public void setTXUpdateTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getBalance() {
        return balance;
    }

    public List<String> getTokenIdList()
    {
        String[] list = balance.split(",");
        List<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, list);
        return tokens;
    }

    public void setBalance(String balance) {
        this.balance = balance;
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

    public int getChainId() { return chainId; }
    public void setChainId(int chainId) { this.chainId = chainId; }

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
}
