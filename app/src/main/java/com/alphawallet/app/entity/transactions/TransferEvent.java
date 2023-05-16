package com.alphawallet.app.entity.transactions;

/**
 * Created by JB on 15/05/2023.
 */
public class TransferEvent
{
    public final String valueList;
    public final String activityName;
    public final String contractAddress;
    public final String tokenValue;

    public TransferEvent(String valueList, String activityName, String contractAddress, String tokenValue)
    {
        this.activityName = activityName;
        this.valueList = valueList;
        this.contractAddress = contractAddress;
        this.tokenValue = tokenValue;
    }
}
