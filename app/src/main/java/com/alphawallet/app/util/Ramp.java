package com.alphawallet.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.alphawallet.app.ui.HomeActivity;

public class Ramp {
    public static native String getRampKey();

    public static void start(Context context, String address, String symbol) {
        Uri.Builder builder = new Uri.Builder();
        Uri uri = builder.scheme("https")
                .authority("buy.ramp.network")
                .appendQueryParameter("hostApiKey", getRampKey())
                .appendQueryParameter("hostLogoUrl", "https://alphawallet.com/wp-content/themes/alphawallet/img/alphawallet-logo.svg")
                .appendQueryParameter("hostAppName", "AlphaWallet")
                .appendQueryParameter("swapAsset", symbol)
                .appendQueryParameter("userAddress", address)
                .build();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("url", uri.toString());
        intent.putExtra("showNavBar", true);
        context.startActivity(intent);
    }
}
