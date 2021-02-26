package com.alphawallet.app.entity;

public class OnRampContract {
    private String symbol;
    private String address;
    private String provider;

    public OnRampContract(String symbol) {
        this.symbol = symbol;
        this.address = "";
        this.provider = "";
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
