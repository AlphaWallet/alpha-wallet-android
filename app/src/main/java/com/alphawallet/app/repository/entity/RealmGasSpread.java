package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.GasPriceSpread;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 19/11/2020.
 */
public class RealmGasSpread extends RealmObject
{
    @PrimaryKey
    private long chainId;

    private String rapid;
    private String fast;
    private String standard;
    private String slow;
    private String baseFee;
    private long timeStamp;

    public long getChainId()
    {
        return chainId;
    }

    public void setGasSpread(GasPriceSpread spread, long time)
    {
        rapid = spread.rapid.toString();
        fast = spread.fast.toString();
        standard = spread.standard.toString();
        slow = spread.slow.toString() + "," + (spread.lockedGas ? "0" : "1");
        baseFee = spread.baseFee.toString();
        timeStamp = time;
    }

    public GasPriceSpread getGasPrice()
    {
        boolean gasLocked = false;
        String slowGas = slow;
        if (slow.contains(","))
        {
            String[] gasBreakdown = slow.split(",");
            slowGas = gasBreakdown[0];
            gasLocked = gasBreakdown[1].charAt(0) == '0';
        }
        return new GasPriceSpread(rapid, fast, standard, slowGas, baseFee, timeStamp, gasLocked);
    }
}
