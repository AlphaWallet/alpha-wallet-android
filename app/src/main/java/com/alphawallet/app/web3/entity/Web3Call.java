package com.alphawallet.app.web3.entity;
import com.alphawallet.app.util.Utils;

import org.web3j.protocol.core.DefaultBlockParameter;

import java.math.BigInteger;

/**
 * Created by JB on 21/07/2020.
 */
public class Web3Call
{
    public final Address to;
    public final DefaultBlockParameter blockParam;
    public final String payload;
    public final BigInteger value;
    public final BigInteger gasLimit;
    public final long leafPosition;

    public Web3Call(
            Address to,
            DefaultBlockParameter blockParam,
            String payload,
            String value,
            String gasLimit,
            long leafPosition) {
        this.to = to;
        this.blockParam = blockParam;
        this.payload = payload;
        this.value = value != null ? Utils.stringToBigInteger(value) : null;
        this.gasLimit = gasLimit != null ? Utils.stringToBigInteger(gasLimit) : null;
        this.leafPosition = leafPosition;
    }
}
