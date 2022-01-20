package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;

import java.math.BigInteger;

/**
 * Created by JB on 20/01/2022.
 */
public class GasSpeed2
{
    public final String speed;
    public long seconds;
    public final EIP1559FeeOracleResult gasPrice;
    public final boolean isCustom;

    public GasSpeed2(String speed, long seconds, EIP1559FeeOracleResult gasPrice)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = gasPrice;
        this.isCustom = false;
    }

    public GasSpeed2(String speed, long seconds, EIP1559FeeOracleResult gasPrice, boolean isCustom)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = gasPrice;
        this.isCustom = isCustom;
    }
}
