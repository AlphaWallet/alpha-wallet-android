package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.SellTicketActivity;
import com.alphawallet.app.entity.Token;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketRouter
{
    public void open(Context context, Token token) {
        Intent intent = new Intent(context, SellTicketActivity.class);
        intent.putExtra(C.Key.TICKET, token);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        //intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
