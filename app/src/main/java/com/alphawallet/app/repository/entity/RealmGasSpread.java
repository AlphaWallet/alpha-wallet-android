package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.TXSpeed;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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
    private String baseFee; //TODO: Remove in next DB Migration
    private long timeStamp;

    public long getChainId()
    {
        return chainId;
    }

    public void setGasSpread(GasPriceSpread spread, long time)
    {
        rapid = addGasPrice(TXSpeed.RAPID, spread);
        fast = addGasPrice(TXSpeed.FAST, spread);
        standard = addGasPrice(TXSpeed.STANDARD, spread);
        slow = addGasPrice(TXSpeed.SLOW, spread);
        timeStamp = time;
    }

    private String addGasPrice(TXSpeed speed, GasPriceSpread spread)
    {
        if (spread.getSelectedGasFee(speed) != null)
        {
            return spread.getSelectedGasFee(speed).gasPrice.maxFeePerGas.toString();
        }
        else
        {
            return "0";
        }
    }

    public BigInteger getGasFee(TXSpeed speed)
    {
        try
        {
            switch (speed)
            {
                case RAPID:
                    return new BigInteger(rapid);
                case FAST:
                    return new BigInteger(fast);
                case STANDARD:
                    return new BigInteger(standard);
                case SLOW:
                    return new BigInteger(slow);
                default:
                case CUSTOM:
                    return BigInteger.ZERO;
            }
        }
        catch (Exception e)
        {
            return BigInteger.ZERO;
        }
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public Map<TXSpeed, BigInteger> getGasFees()
    {
        String slowGas = slow;
        if (slow.contains(","))
        {
            String[] gasBreakdown = slow.split(",");
            slowGas = gasBreakdown[0];
        }

        Map<TXSpeed, BigInteger> fees = new HashMap<>();
        fees.put(TXSpeed.RAPID, new BigInteger(rapid));
        fees.put(TXSpeed.FAST, new BigInteger(fast));
        fees.put(TXSpeed.STANDARD, new BigInteger(standard));
        fees.put(TXSpeed.SLOW, new BigInteger(slowGas));
        return fees;
    }

    public BigInteger getGasPrice()
    {
        //try standard
        BigInteger gasPrice = getGasFee(TXSpeed.STANDARD);
        if (gasPrice.compareTo(BigInteger.ZERO) > 0) return gasPrice;

        //now try any
        for (TXSpeed txs : TXSpeed.values())
        {
            gasPrice = getGasFee(txs);
            if (gasPrice.compareTo(BigInteger.ZERO) > 0) return gasPrice;
        }

        return BigInteger.ZERO;
    }

    public boolean isLocked()
    {
        boolean gasLocked = false;
        if (slow.contains(","))
        {
            String[] gasBreakdown = slow.split(",");
            gasLocked = gasBreakdown[1].charAt(0) == '0';
        }

        return gasLocked;
    }
}
