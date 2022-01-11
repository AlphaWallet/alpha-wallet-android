package com.alphawallet.app.repository.entity;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTokenMapping extends RealmObject {

    @PrimaryKey
    public String address;  // ContractAddress String (addr-chainId) for derivative
    public String base;     // Base contract, usually main net (eg DAI)
    public int group;       // Ordinal for enum TokenGroup

    public ContractAddress getBase()
    {
        if (!TextUtils.isEmpty(base))
        {
            return new ContractAddress(base);
        }
        else
        {
            return null;
        }
    }

    public TokenGroup getGroup()
    {
        if (group >= 0 && group < TokenGroup.values().length)
        {
            return TokenGroup.values()[group];
        }
        else
        {
            return TokenGroup.ASSET;
        }
    }
}
