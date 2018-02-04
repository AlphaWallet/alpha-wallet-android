package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ExternalBrowserRouter {

    public void open(Context context, Uri uri) {
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(launchBrowser);
    }
}
