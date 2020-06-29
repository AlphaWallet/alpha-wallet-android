package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;

public class QRScanningViewModel extends BaseViewModel {

    QRScanningViewModel() {
    }

    public void prepare(Context context)
    {

    }

    public void handleResult(Activity activity, String qrCode)
    {
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_QR_CODE, qrCode);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    public void finishWithCancel(Activity activity)
    {
        Intent intent = new Intent();
        activity.setResult(Activity.RESULT_CANCELED, intent);
        activity.finish();
    }
}
