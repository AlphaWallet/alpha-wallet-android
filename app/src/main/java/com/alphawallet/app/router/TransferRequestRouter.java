package com.alphawallet.app.router;


import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.ui.TransferRequestActivity;

public class TransferRequestRouter
{
    public void open(Activity context, QRResult result)
    {
        Intent intent = new Intent(context, TransferRequestActivity.class);
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
    }
}
