package com.wallet.crypto.alphawallet.router;


import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ConfirmationType;
import com.wallet.crypto.alphawallet.ui.ConfirmationActivity;

import java.math.BigInteger;

//TODO: Refactor when we add token type to token class
public class ConfirmationRouter {
    public void open(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_AMOUNT, amount.toString());
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        int tokenType = ConfirmationType.ETH.ordinal();
        if (sendingTokens) tokenType = ConfirmationType.ERC20.ordinal();
        intent.putExtra(C.TOKEN_TYPE, tokenType);
        context.startActivity(intent);
    }

    public void open(Context context, String to, String ids, String contractAddress, int decimals, String symbol, String ticketIDs) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_AMOUNT, ids);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, true);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.ERC875.ordinal());
        intent.putExtra(C.EXTRA_TOKENID_LIST, ticketIDs);
        context.startActivity(intent);
    }

    public void openMarket(Context context, String to, String ids, String contractAddress, String symbol, String ticketIDs) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_AMOUNT, ids);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_DECIMALS, 0);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, true);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.MARKET.ordinal());
        intent.putExtra(C.EXTRA_TOKENID_LIST, ticketIDs);
        context.startActivity(intent);
    }
}
