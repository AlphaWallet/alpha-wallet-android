package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.SellDetailActivity;
import io.awallet.crypto.alphawallet.ui.TransferTicketDetailActivity;

import static io.awallet.crypto.alphawallet.C.EXTRA_TOKENID_LIST;
import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 22/02/2018.
 */

public class TransferTicketDetailRouter {

    public void open(Context context, Token token, String ticketIDs, Wallet wallet) {
        Intent intent = new Intent(context, TransferTicketDetailActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, ticketIDs);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
