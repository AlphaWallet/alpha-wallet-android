package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.MagicLinkParcel;
import io.awallet.crypto.alphawallet.ui.PurchaseTicketsActivity;

import static io.awallet.crypto.alphawallet.C.MARKET_INSTANCE;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBuyRouter
{
    public void open(Context context, MagicLinkParcel instance) {
        Intent intent = new Intent(context, PurchaseTicketsActivity.class);
        intent.putExtra(MARKET_INSTANCE, instance);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
