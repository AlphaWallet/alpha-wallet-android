package com.alphawallet.app.entity;

import androidx.activity.result.ActivityResult;

import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigInteger;

/**
 * Created by JB on 16/01/2021.
 */
public interface ActionSheetInterface
{
    void lockDragging(boolean shouldLock);
    void fullExpand();

    default void success()
    {
    }

    default void setURL(String url)
    {
    }

    default void setGasEstimate(BigInteger estimate)
    {
    }

    default void completeSignRequest(Boolean gotAuth)
    {
    }

    default void setSigningWallet(String account)
    {
    }

    default void setIcon(String icon)
    {
    }

    default void transactionWritten(String hash)
    {
    }

    default void updateChain(long chainId)
    {
    }

    default Web3Transaction getTransaction()
    {
        throw new RuntimeException("Implement getTransaction");
    }

    default void setSignOnly()
    {
    }

    default void setCurrentGasIndex(ActivityResult result)
    {
    }
}
