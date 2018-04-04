package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.SellDetailActivity;

import static io.awallet.crypto.alphawallet.C.EXTRA_TOKENID_LIST;
import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.WALLET;
import static io.awallet.crypto.alphawallet.C.MAGIC_LINK;
import static io.awallet.crypto.alphawallet.C.MARKET_INSTANCE;
import static io.awallet.crypto.alphawallet.C.MARKET_SALE;

/**
 * Created by James on 22/02/2018.
 */

public class SellDetailRouter {

    public void open(Context context, Token token, String ticketIDs, Wallet wallet) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(MARKET_INSTANCE, MARKET_SALE);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public void openMagic(Context context, Token token, String ticketIDs, Wallet wallet) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(MARKET_INSTANCE, MAGIC_LINK);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
