package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.content.Context;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;

import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

    public void setSigningWallet(String account)
    {
        throw new RuntimeException("Implement setSigningWallet");
    }

    public void setIcon(String icon)
    {
        throw new RuntimeException("Implement setIcon");
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

    public void fullExpand()
    {
        FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(STATE_EXPANDED);
    }

    public void lockDragging(boolean lock)
    {
        getBehavior().setDraggable(!lock);

        //ensure view fully expanded when locking scroll. Otherwise we may not be able to see our expanded view
        if (lock)
        {
            FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(STATE_EXPANDED);
        }
    }
}
