package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.PurchaseTicketsActivity;

import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.MARKET_INSTANCE;


/**
 * Created by James on 23/02/2018.
 */

public class PurchaseTicketRouter
{
    public void open(Context context, Token token, MagicLinkParcel instance, int chainId) {
        Intent intent = new Intent(context, PurchaseTicketsActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(MARKET_INSTANCE, instance);
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
