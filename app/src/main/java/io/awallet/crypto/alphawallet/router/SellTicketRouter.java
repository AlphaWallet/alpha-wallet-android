package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.ui.SellTicketActivity;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;

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
