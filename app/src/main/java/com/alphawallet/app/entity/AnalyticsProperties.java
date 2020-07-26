package com.alphawallet.app.entity;

public class AnalyticsProperties {

    private String walletAddress;

    private String fromAddress;

    private String toAddress;

    private String amount;

    private String walletType;

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String type) {
        this.walletType = type;
    }
}