package com.alphawallet.app.repository.entity;

import android.text.TextUtils;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.TokenInfo;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmStaticToken extends RealmObject {

    @PrimaryKey
    public String address;
    public String name;
    public String symbol;
    private int decimals;
    private int interfaceSpec;
    private String auxData;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public ContractType getContractType()
    {
        int typeOrdinal = interfaceSpec;
        if (typeOrdinal > ContractType.CREATION.ordinal()) typeOrdinal = ContractType.NOT_SET.ordinal();
        return ContractType.values()[typeOrdinal];
    }

    public int getInterfaceSpec() {
        return interfaceSpec;
    }

    public void setInterfaceSpec(int interfaceSpec) {
        this.interfaceSpec = interfaceSpec;
    }

    public String getAuxData() {
        return auxData;
    }

    public void setAuxData(String auxData) {
        this.auxData = auxData;
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
    }

    public void populate(RealmToken realmToken)
    {
        setName(realmToken.getName());
        setSymbol(realmToken.getSymbol());
        setDecimals(realmToken.getDecimals());
        setInterfaceSpec(realmToken.getInterfaceSpec());
        setAuxData(realmToken.getAuxData());
    }
}
