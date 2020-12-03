package com.alphawallet.app.ui.widget.entity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by JB on 22/11/2020.
 */
public class GasSpeed
{
    public final String speed;
    public long seconds;
    public final BigInteger gasPrice;

    public GasSpeed(String speed, long seconds, BigInteger gasPrice)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = gasPrice;
    }
}
