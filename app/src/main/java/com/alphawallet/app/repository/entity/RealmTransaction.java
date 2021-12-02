package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTransaction extends RealmObject {
    @PrimaryKey
    private String hash;
    private String blockNumber;
    private long timeStamp;
    private int nonce;
    private String from;
    private String to;
    private String value;
    private String gas;
    private String gasPrice;
    private String gasUsed;
    private String input;
    private String error;
    private long chainId;
    private long expectedCompletion;
    private String contractAddress; // this is so we can efficiently lookup transactions relating to contracts,
                                    // if we discovered them using the Etherscan 'Transfers' API.
                                    // NB: only transactions discovered by the Transfers API will have this field.
                                    // It allows us to lookup eg AirDrop tx's or Internal tx's that otherwise wouldn't
                                    // be indexable via RealmDB.

    public String getHash() {
        return hash;
    }

    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getGas() {
        return gas;
    }

    public void setGas(String gas) {
        this.gas = gas;
    }

    public String getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(String gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(String gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getChainId()
    {
        return chainId;
    }

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }

    public boolean isPending()
    {
        return blockNumber == null || blockNumber.length() == 0 || (blockNumber.equals("0") || blockNumber.equals("-2"));
    }

    public long getExpectedCompletion()
    {
        return expectedCompletion;
    }

    public void setExpectedCompletion(long expectedCompletion)
    {
        this.expectedCompletion = expectedCompletion;
    }

    public String getContractAddress()
    {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress)
    {
        this.contractAddress = contractAddress;
    }
}
