package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.ui.MarketBrowseActivity;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBuyRouter
{
    public void open(Context context) {
        Intent intent = new Intent(context, MarketBrowseActivity.class); //TODO: Change to Market Buy Activity when classes written
        context.startActivity(intent);
    }
}
