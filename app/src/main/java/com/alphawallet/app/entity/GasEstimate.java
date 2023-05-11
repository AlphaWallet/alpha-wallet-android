package com.alphawallet.app.entity;

import android.text.TextUtils;

import java.math.BigInteger;

public class GasEstimate
{
    private BigInteger value;
    private String error;

    public GasEstimate(BigInteger value)
    {
        this.value = value;
        this.error = "";
    }

    public GasEstimate(BigInteger value, String error)
    {
        this.value = value;
        this.error = error;
    }

    public BigInteger getValue()
    {
        return value;
    }

    public String getError()
    {
        return error;
    }

    public void setValue(BigInteger value)
    {
        this.value = value;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public boolean hasError()
    {
        return !TextUtils.isEmpty(error);
    }
}
