package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.ui.ImportTokenActivity;

import static io.awallet.crypto.alphawallet.C.IMPORT_STRING;
import static io.awallet.crypto.alphawallet.C.Key.MARKETPLACE_EVENT;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenRouter
{
    public void open(Context context, String importTxt) {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }
}
