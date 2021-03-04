package com.alphawallet.app.entity;

import com.alphawallet.app.repository.OnRampRepository;

public class OnRampContract {
    private String symbol;
    private String provider;

    public OnRampContract(String symbol)
    {
        this.symbol = symbol;
        this.provider = OnRampRepository.DEFAULT_PROVIDER;
    }

    public OnRampContract()
    {
        this.symbol = "";
        this.provider = OnRampRepository.DEFAULT_PROVIDER;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }
}
