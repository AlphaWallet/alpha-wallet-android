package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.SellTicketActivity;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketRouter
{
    public void open(Context context, Ticket ticket) {
        Intent intent = new Intent(context, SellTicketActivity.class);
        intent.putExtra(TICKET, ticket);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
