package com.wallet.crypto.trust.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by marat on 9/26/17.
 */

/* Sample object
{
    "blockNumber": "4026694",
    "timeStamp": "1506480048",
    "hash": "0x352e8d27aca845bd231e9c618d36875c7315e8ebba4d24f85e185648a57d62a9",
    "nonce": "3",
    "blockHash": "0xbad337c86c8f6ad18fdae9914d2ddebfb0d373b5a980a94e82265134aaa305f4",
    "transactionIndex": "1",
    "from": "0x6fcea7b8786ba1fe964cbaff6595e2d03e1a51f4",
    "to": "0x5dd0b5d02cd574412ad58dd84a2f402cc25e320a",
    "value": "0",
    "gas": "250000",
    "gasPrice": "20000000000",
    "isError": "0",
    "input": "0x338b5dea000000000000000000000000c2c4045151bcb748525f968911894f92d892a84b00000000000000000000000000000000000000000000001b1ae4d6e2ef500000",
    "contractAddress": "",
    "cumulativeGasUsed": "150894",
    "gasUsed": "105222",
    "confirmations": "329"
}
*/


public class ESTransaction {

    @SerializedName("blockNumber")
    private String blockNumber;

    @SerializedName("timeStamp")
    private String timeStamp;

    @SerializedName("hash")
    private String hash;

    @SerializedName("nonce")
    private String nonce;

    @SerializedName("blockHash")
    private String blockHash;

    @SerializedName("transactionIndex")
    private String transactionIndex;

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

    @SerializedName("isError")
    private String isError;

    @SerializedName("input")
    private String input;

    @SerializedName("contractAddress")
    private String contractAddress;

    @SerializedName("cumulativeGasUsed")
    private String cumulativeGasUsed;

    @SerializedName("gasUsed")
    private String gasUsed;

    @SerializedName("confirmations")
    private String confirmations;

    public String getBlockNumber() {
        return blockNumber;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getHash() {
        return hash;
    }

    public String getNonce() {
        return nonce;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getTransactionIndex() {
        return transactionIndex;
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

    public String getIsError() {
        return isError;
    }

    public String getInput() {
        return input;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getCumulativeGasUsed() {
        return cumulativeGasUsed;
    }

    public String getGasUsed() {
        return gasUsed;
    }

    public String getConfirmations() {
        return confirmations;
    }
}
