package com.alphawallet.app.entity.tokens;

public class TokenPortfolio {
    private String balance;
    private String returns;
    private String profit24Hrs;
    private String profitTotal;
    private String share;
    private String averageCost;
    private String fees;

    public String getBalance()
    {
        return balance;
    }

    public void setBalance(String balance)
    {
        this.balance = balance;
    }

    public String getReturns()
    {
        return returns;
    }

    public void setReturns(String returns)
    {
        this.returns = returns;
    }

    public String getProfit24Hrs()
    {
        return profit24Hrs;
    }

    public void setProfit24Hrs(String profit24Hrs)
    {
        this.profit24Hrs = profit24Hrs;
    }

    public String getProfitTotal()
    {
        return profitTotal;
    }

    public void setProfitTotal(String profitTotal)
    {
        this.profitTotal = profitTotal;
    }

    public String getShare()
    {
        return share;
    }

    public void setShare(String share)
    {
        this.share = share;
    }

    public String getAverageCost()
    {
        return averageCost;
    }

    public void setAverageCost(String averageCost)
    {
        this.averageCost = averageCost;
    }

    public String getFees()
    {
        return fees;
    }

    public void setFees(String fees)
    {
        this.fees = fees;
    }
}
