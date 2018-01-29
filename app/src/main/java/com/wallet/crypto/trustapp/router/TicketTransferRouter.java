package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.entity.Ticket;
import com.wallet.crypto.trustapp.ui.TicketTransferActivity;
import com.wallet.crypto.trustapp.ui.UseTokenActivity;

import static com.wallet.crypto.trustapp.C.Key.TICKET;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferRouter {
    public void open(Context context, Ticket ticket) {
        Intent intent = new Intent(context, TicketTransferActivity.class);
        intent.putExtra(TICKET, ticket);
        context.startActivity(intent);
    }
}
