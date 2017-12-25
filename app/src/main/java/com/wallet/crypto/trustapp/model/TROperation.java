package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by msubkhankulov on 12/16/2017.
 */

/* Based on the following JSON
{
    "_id": "5a14702affc4db9cf17c17ee",
    "transactionId": "0xd3efa659f5968c0b16e038e00add96eeb10dc1e2baaf3d493d3b23df63985ffa",
    "viewType": "token_transfer",
    "from": "0x0122374ddd61ebdbe487f27225c8d55a96688714",
    "to": "0xaa3cc54d7f10fa3a1737e4997ba27c34f330ce16",
    "value": "1000000000000000000",
    "contract": {
        "_id": "5a055b617946851ff3f85327",
        "address": "0xab95e915c123fded5bdfb6325e35ef5515f1ea69",
        "name": "XENON",
        "totalSupply": "1000000000000000000000000000",
        "decimals": 18,
        "symbol": "XNN"
    }
}
 */

public class TROperation {

    @SerializedName("transactionId")
    private String transactionId;

    @SerializedName("viewType")
    private String type;

    @SerializedName("from")
    private String from;

    @SerializedName("to")
    private String to;

    @SerializedName("value")
    private String value;

    @SerializedName("contract")
    private TRContract contract;

    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getValue() {
        return value;
    }

    public TRContract getContract() {
        return contract;
    }
}
