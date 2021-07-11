package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 10/07/2021.
 */
public class RealmNFTAsset extends RealmObject
{
    @PrimaryKey
    private String tokenIdAddr; //format is addr-chainId-tokenId

    private String metaData; //store as a JSON blob

    public String getTokenId()
    {
        String[] str = tokenIdAddr.split("-");
        return str[str.length - 1];
    }

    public void setMetaData(String metaData)
    {
        this.metaData = metaData;
    }

    public String getMetaData()
    {
        return metaData;
    }

    public static String databaseKey(Token token, BigInteger tokenId)
    {
        return TokensRealmSource.databaseKey(token) + "-" + tokenId.toString();
    }
}
