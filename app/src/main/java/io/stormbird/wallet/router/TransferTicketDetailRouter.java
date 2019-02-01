package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.TransferTicketDetailActivity;

import static io.stormbird.wallet.C.EXTRA_STATE;
import static io.stormbird.wallet.C.EXTRA_TOKENID_LIST;
import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.WALLET;

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

    public void openTransfer(Context context, Token token, String ticketIDs, Wallet wallet, int state) {
        Intent intent = new Intent(context, TransferTicketDetailActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(EXTRA_STATE, state);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
