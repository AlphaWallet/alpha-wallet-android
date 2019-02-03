package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.ui.ImportTokenActivity;

import static io.stormbird.wallet.C.IMPORT_STRING;

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
