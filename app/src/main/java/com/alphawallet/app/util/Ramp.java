package com.alphawallet.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.alphawallet.app.ui.HomeActivity;

public final class Ramp {
    private static Ramp INSTANCE;
    private String address;

    private Ramp() {
    }

    public static Ramp getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Ramp();
        }
        return INSTANCE;
    }

    public void init(String address) {
        this.address = address;
    }

    public void start(Context context) {
        String rampApiKey = "j5wr7oqktym7z69yyf84bb8a6cqb7qfu5ynmeyvn";
        Uri.Builder builder = new Uri.Builder();
        Uri uri = builder.scheme("https")
                .authority("buy.ramp.network")
//                .authority("ri-widget-staging-ropsten.firebaseapp.com")
//                .authority("ri-widget-staging.firebaseapp.com")
                .appendQueryParameter("hostApiKey", rampApiKey)
                .appendQueryParameter("hostLogoUrl", "https://alphawallet.com/wp-content/themes/alphawallet/img/alphawallet-logo.svg")
                .appendQueryParameter("hostAppName", "AlphaWallet")
                .appendQueryParameter("swapAsset", "xDai")
                .appendQueryParameter("userAddress", address)
                .build();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("url", uri.toString());
        intent.putExtra("showNavBar", true);
        context.startActivity(intent);
    }
}
