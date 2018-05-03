package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.ui.CreateTokenActivity;

import static io.awallet.crypto.alphawallet.C.IMPORT_STRING;

public class CreateTokenRouter
{
    public void open(Context context, String importTxt)
    {
        Intent intent = new Intent(context, CreateTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }
}
