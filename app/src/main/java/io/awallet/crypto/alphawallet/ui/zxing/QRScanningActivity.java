package io.awallet.crypto.alphawallet.ui.zxing;


import android.os.Bundle;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.ui.BaseActivity;

public class QRScanningActivity extends BaseActivity
{
    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        setContentView(R.layout.activity_full_screen_scanner_fragment);
    }
}
