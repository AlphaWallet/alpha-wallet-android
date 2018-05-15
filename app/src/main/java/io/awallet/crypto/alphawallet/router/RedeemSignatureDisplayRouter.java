package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.RedeemSignatureDisplayActivity;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRangeParcel;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.TICKET_RANGE;
import static io.awallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 25/01/2018.
 */

public class RedeemSignatureDisplayRouter {
    public void open(Context context, Wallet wallet, Token token, TicketRangeParcel range) {
        Intent intent = new Intent(context, RedeemSignatureDisplayActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(TICKET_RANGE, range);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}