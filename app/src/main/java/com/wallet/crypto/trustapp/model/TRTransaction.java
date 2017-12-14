package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by msubkhankulov on 11/30/2017.
 */

/* Based on the following JSON
    {
      "_id": "0xa65e8541bc78479d5d2d62ebbcc1454f106dd9824cde3bcb66c01097e43ca39e",
      "blockNumber": 4815300,
      "timeStamp": "1511432228",
      "nonce": 9,
      "from": "0xaa3cc54d7f10fa3a1737e4997ba27c34f330ce16",
      "to": "0x7d788fc8df7165b11a19f201558fcc3590fd8d97",
      "value": "100000000000000",
      "gas": "90000",
      "gasPrice": "1000000000",
      "input": "0x",
      "gasUsed": "21000",
      "operations": [],
      "addresses": [
        "0xaa3cc54d7f10fa3a1737e4997ba27c34f330ce16",
        "0x7d788fc8df7165b11a19f201558fcc3590fd8d97"
      ],
      "operations_localized": [],
      "id": "0xa65e8541bc78479d5d2d62ebbcc1454f106dd9824cde3bcb66c01097e43ca39e"
    }
 */

public class TRTransaction {

    @SerializedName("blockNumber")
    private String blockNumber;

    @SerializedName("timeStamp")
    private String timeStamp;

    @SerializedName("nonce")
    private String nonce;

    @SerializedName("from")
    private String from;

    @SerializedName("to")
    private String to;

    @SerializedName("value")
    private String value;

    @SerializedName("gas")
    private String gas;

    @SerializedName("gasPrice")
    private String gasPrice;

    @SerializedName("input")
    private String input;

    @SerializedName("gasUsed")
    private String gasUsed;

    @SerializedName("id")
    private String hash;

    public String getBlockNumber() {
        return blockNumber;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getNonce() {
        return nonce;
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

    public String getGas() {
        return gas;
    }

    public String getGasPrice() {
        return gasPrice;
    }

    public String getInput() {
        return input;
    }

    public String getGasUsed() {
        return gasUsed;
    }

    public String getHash() {
        return hash;
    }
}
