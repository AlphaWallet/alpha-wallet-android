package com.alphawallet.app.entity;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Created by JB on 14/07/2021.
 */

public class ERC1155TransferEvent implements Comparable<ERC1155TransferEvent>
{
    public final BigInteger blockNumber;
    public final String to;
    public final String from;
    public final BigInteger tokenId;
    public final BigInteger value;
    public final boolean isReceive;

    public ERC1155TransferEvent(BigInteger blockNumber, String to, String from, BigInteger tokenId, BigInteger value, boolean isReceive)
    {
        this.blockNumber = blockNumber;
        this.to = to;
        this.from = from;
        this.tokenId = tokenId;
        this.value = isReceive ? value : value.negate();
        this.isReceive = isReceive;
    }

    @Override
    public int compareTo(@NotNull ERC1155TransferEvent other)
    {
        return blockNumber.compareTo(other.blockNumber);
    }
}