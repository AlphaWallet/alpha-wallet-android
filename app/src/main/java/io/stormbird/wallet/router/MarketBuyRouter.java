package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.ui.PurchaseTicketsActivity;

import static io.stormbird.wallet.C.MARKET_INSTANCE;

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
