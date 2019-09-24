package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.TransferTicketActivity;
import com.alphawallet.app.entity.Token;

/**
 * Created by James on 13/02/2018.
 */

public class TransferTicketRouter
{
    public void open(Context context, Token ticket) {
        Intent intent = new Intent(context, TransferTicketActivity.class);
        intent.putExtra(C.Key.TICKET, ticket);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
