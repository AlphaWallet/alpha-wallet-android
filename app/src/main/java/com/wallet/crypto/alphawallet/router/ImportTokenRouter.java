package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.ui.ImportTokenActivity;

import static com.wallet.crypto.alphawallet.C.IMPORT_STRING;
import static com.wallet.crypto.alphawallet.C.Key.MARKETPLACE_EVENT;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenRouter
{
    public void open(Context context, String importTxt) {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.putExtra(IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }
}
