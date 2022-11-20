package com.alphawallet.app.widget;

import android.content.Context;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;

import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.math.BigInteger;

/**
 * Created by JB on 20/11/2022.
 */
public abstract class ActionSheet extends BottomSheetDialog
{
    public ActionSheet(@NonNull Context context)
    {
        super(context);
    }

    public void setCurrentGasIndex(ActivityResult result)
    {
        throw new RuntimeException("Implement setCurrentGasIndex");
    }

    public void fullExpand()
    {
        throw new RuntimeException("Implement fullExpand");
    }

    public void success()
    {
        throw new RuntimeException("Implement success");
    }

    public void setURL(String url)
    {
        throw new RuntimeException("Implement setURL");
    }

    public void setGasEstimate(BigInteger estimate)
    {
        throw new RuntimeException("Implement setGasEstimate");
    }

    public void completeSignRequest(Boolean gotAuth)
    {
        throw new RuntimeException("Implement completeSignRequest");
    }

    public void transactionWritten(String hash)
    {
        throw new RuntimeException("Implement transactionWritten");
    }

    public void updateChain(long chainId)
    {
        throw new RuntimeException("Implement updateChain");
    }

    public Web3Transaction getTransaction()
    {
        throw new RuntimeException("Implement getTransaction");
    }

    public void setSignOnly()
    {
        throw new RuntimeException("Implement setSignOnly");
    }

    public void forceDismiss()
    {
        setOnDismissListener(v -> {
            // Do nothing
        });
        dismiss();
    }
}
