package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.TicketTransferActivity;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.TICKET_RANGE;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferRouter {
    public void open(Context context, Ticket ticket) {
        Intent intent = new Intent(context, TicketTransferActivity.class);
        intent.putExtra(TICKET, ticket);
        context.startActivity(intent);
    }

    public void openRange(Context context, Ticket ticket, TicketRange range) {
        Intent intent = new Intent(context, TicketTransferActivity.class);
        intent.putExtra(TICKET, ticket);
        intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
