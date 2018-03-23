package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.ui.TicketTransferActivity;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.TICKET_RANGE;

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
