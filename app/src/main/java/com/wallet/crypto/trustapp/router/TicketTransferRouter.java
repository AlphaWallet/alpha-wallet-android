package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.ui.TicketTransferActivity;
import com.wallet.crypto.trustapp.ui.UseTokenActivity;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferRouter {
    public void open(Context context, String address) {
        Intent intent = new Intent(context, TicketTransferActivity.class);
        intent.putExtra(C.EXTRA_ADDRESS, address);
        context.startActivity(intent);
    }
}
