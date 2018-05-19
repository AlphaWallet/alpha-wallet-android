package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.ui.SellTicketActivity;
import io.stormbird.wallet.ui.TransferTicketActivity;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class TransferTicketRouter
{
    public void open(Context context, Ticket ticket) {
        Intent intent = new Intent(context, TransferTicketActivity.class);
        intent.putExtra(TICKET, ticket);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
