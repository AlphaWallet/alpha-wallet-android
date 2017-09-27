package com.example.marat.wal.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;

/**
 * Created by marat on 9/26/17.
 */

public class ESTransaction {
    @SerializedName("blockNumber")
    private BigInteger blockNumber;

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

}
