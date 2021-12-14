package com.alphawallet.app.entity.tokens;

public class TokenStats {
    private String marketCap;
    private String totalSupply;
    private String maxSupply;
    private String yearLow;
    private String yearHigh;

    public String getMarketCap()
    {
        return marketCap;
    }

    public void setMarketCap(String marketCap)
    {
        this.marketCap = marketCap;
    }

    public String getTotalSupply()
    {
        return totalSupply;
    }

    public void setTotalSupply(String totalSupply)
    {
        this.totalSupply = totalSupply;
    }

    public String getMaxSupply()
    {
        return maxSupply;
    }

    public void setMaxSupply(String maxSupply)
    {
        this.maxSupply = maxSupply;
    }

    public String getYearLow()
    {
        return yearLow;
    }

    public void setYearLow(String yearLow)
    {
        this.yearLow = yearLow;
    }

    public String getYearHigh()
    {
        return yearHigh;
    }

    public void setYearHigh(String yearHigh)
    {
        this.yearHigh = yearHigh;
    }
}
