package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.TransferTicketActivity;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class TransferTicketRouter
{
    public void open(Context context, Token ticket) {
        Intent intent = new Intent(context, TransferTicketActivity.class);
        intent.putExtra(TICKET, ticket);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
