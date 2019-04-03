package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.SellTicketActivity;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketRouter
{
    public void open(Context context, Token token) {
        Intent intent = new Intent(context, SellTicketActivity.class);
        intent.putExtra(TICKET, token);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
