package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Wallet;

/**
 * Created by James on 22/02/2018.
 */

public class SellDetailRouter {

    public void openMarketPlace(Context context, Token token, String ticketIDs, Wallet wallet) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_MARKET_SALE);
        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        context.startActivity(intent);
    }

    public void openUniversalLink(Context context, Token token, String ticketIDs, Wallet wallet, int state, double price) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(C.EXTRA_STATE, state);
        intent.putExtra(C.EXTRA_PRICE, price);
        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        context.startActivity(intent);
    }
}
