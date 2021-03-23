package com.alphawallet.app.web3.entity;
import org.web3j.protocol.core.DefaultBlockParameter;

/**
 * Created by JB on 21/07/2020.
 */
public class Web3Call
{
    public final Address to;
    public final DefaultBlockParameter blockParam;
    public final String payload;
    public final long leafPosition;

    public Web3Call(
            Address to,
            DefaultBlockParameter blockParam,
            String payload,
            long leafPosition) {
        this.to = to;
        this.blockParam = blockParam;
        this.payload = payload;
        this.leafPosition = leafPosition;
    }
}
