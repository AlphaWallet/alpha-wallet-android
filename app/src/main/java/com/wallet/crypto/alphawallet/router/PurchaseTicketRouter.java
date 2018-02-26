package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.PurchaseTicketsActivity;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.MARKET_INSTANCE;


/**
 * Created by James on 23/02/2018.
 */

public class PurchaseTicketRouter
{
    public void open(Context context, Token token, SalesOrder instance) {
        Intent intent = new Intent(context, PurchaseTicketsActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(MARKET_INSTANCE, instance);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
