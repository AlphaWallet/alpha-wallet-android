package io.awallet.crypto.alphawallet.repository.entity;

import io.realm.RealmObject;

public class RealmTransactionContract extends RealmObject {
    private String address;
    private String name;
    private String totalSupply;
    private int decimals;
    private String symbol;

    private String balance;
    private String operation;
    private String otherParty;
    private String indices;
    private int type;
    private int contractType;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(String totalSupply) {
        this.totalSupply = totalSupply;
    }

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

    public void setBalance(String balance) { this.balance = balance; }
    public String getBalance() { return this.balance; }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public String getOtherParty()
    {
        return otherParty;
    }

    public void setOtherParty(String otherParty)
    {
        this.otherParty = otherParty;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public int getContractType()
    {
        return contractType;
    }

    public void setContractType(int contractType)
    {
        this.contractType = contractType;
    }

    public void setIndices(String indices)
    {
        this.indices = indices;
    }

    public String getIndices()
    {
        return indices;
    }
}
