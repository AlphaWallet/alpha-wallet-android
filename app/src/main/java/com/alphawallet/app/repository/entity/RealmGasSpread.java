package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.GasPriceSpread;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 19/11/2020.
 */
public class RealmGasSpread extends RealmObject
{
    @PrimaryKey
    private int chainId;

    private String rapid;
    private String fast;
    private String standard;
    private String slow;
    private String baseFee;
    private long timeStamp;

    public int getChainId()
    {
        return chainId;
    }

    public void setGasSpread(GasPriceSpread spread, long time)
    {
        rapid = spread.rapid.toString();
        fast = spread.fast.toString();
        standard = spread.standard.toString();
        slow = spread.slow.toString();
        baseFee = spread.baseFee.toString();
        timeStamp = time;
    }

    // All chains except main net - gas price isn't important
    public void setGasPrice(BigInteger gasPrice, int chain)
    {
        rapid = "0";
        fast = "0";
        standard = gasPrice.toString();
        slow = "0";
        baseFee = "0";
        chainId = chain;
    }

    public GasPriceSpread getGasPrice()
    {
        return new GasPriceSpread(rapid, fast, standard, slow, baseFee, timeStamp);
    }
}
