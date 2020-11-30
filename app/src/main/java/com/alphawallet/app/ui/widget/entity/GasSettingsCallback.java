package com.alphawallet.app.ui.widget.entity;

import java.math.BigInteger;

/**
 * Created by JB on 26/11/2020.
 */
public interface GasSettingsCallback
{
    void gasSettingsUpdate(BigInteger gasPrice, BigInteger gasLimit);
}
