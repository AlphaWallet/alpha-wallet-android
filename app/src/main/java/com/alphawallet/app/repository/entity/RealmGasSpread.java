package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.GasPriceSpread;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 19/11/2020.
 */
public class RealmGasSpread extends RealmObject
{
    @PrimaryKey
    private long timeStamp;
    private int chainId;

    private String rapid;
    private String fast;
    private String standard;
    private String slow;

    public int getChainId()
    {
        return chainId;
    }

    public void setChainId(int chainId)
    {
        this.chainId = chainId;
    }

    public void setGasSpread(GasPriceSpread spread, int chain)
    {
        rapid = spread.rapid.toString();
        fast = spread.fast.toString();
        standard = spread.standard.toString();
        slow = spread.slow.toString();
        chainId = chain;
    }

    // All chains except main net - gas price isn't important
    public void setGasPrice(BigInteger gasPrice, int chain)
    {
        rapid = "0";
        fast = "0";
        standard = gasPrice.toString();
        slow = "0";
        chainId = chain;
    }

    public GasPriceSpread getGasPrice()
    {
        return new GasPriceSpread(rapid, fast, standard, slow, timeStamp);
    }
}
