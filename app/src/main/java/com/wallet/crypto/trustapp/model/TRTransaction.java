package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by msubkhankulov on 11/30/2017.
 */

/* Based on the following JSON
{
	"_id": "0xd3efa659f5968c0b16e038e00add96eeb10dc1e2baaf3d493d3b23df63985ffa",
	"error": "",
	"blockNumber": 4596025,
	"timeStamp": "1511288856",
	"nonce": 23,
	"from": "0x0122374ddd61ebdbe487f27225c8d55a96688714",
	"to": "0xab95e915c123fded5bdfb6325e35ef5515f1ea69",
	"value": "0",
	"gas": "90000",
	"gasPrice": "1000000000",
	"gasUsed": "36573",
	"input": "0xa9059cbb000000000000000000000000aa3cc54d7f10fa3a1737e4997ba27c34f330ce160000000000000000000000000000000000000000000000000de0b6b3a7640000",
	"operations": [{
		"_id": "5a14702affc4db9cf17c17ee",
		"transactionId": "0xd3efa659f5968c0b16e038e00add96eeb10dc1e2baaf3d493d3b23df63985ffa",
		"type": "token_transfer",
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
	}],
	"addresses": ["0x0122374ddd61ebdbe487f27225c8d55a96688714", "0xaa3cc54d7f10fa3a1737e4997ba27c34f330ce16"],
	"operations_localized": [{
		"decimals": 18,
		"symbol": "XNN",
		"new_value": "1000000000000000000",
		"value": "1",
		"contract": "0xab95e915c123fded5bdfb6325e35ef5515f1ea69",
		"to": "0xaa3cc54d7f10fa3a1737e4997ba27c34f330ce16",
		"from": "0x0122374ddd61ebdbe487f27225c8d55a96688714",
		"type": "token_transfer",
		"title": "Transfer XNN"
	}],
	"id": "0xd3efa659f5968c0b16e038e00add96eeb10dc1e2baaf3d493d3b23df63985ffa"
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

    @SerializedName("operations")
    private List<TROperation> operations;

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

    public List<TROperation> getOperations() { return operations; }

    public String getGasUsed() {
        return gasUsed;
    }

    public String getHash() {
        return hash;
    }
}
