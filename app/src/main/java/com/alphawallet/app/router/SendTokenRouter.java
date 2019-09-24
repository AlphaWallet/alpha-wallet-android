package com.alphawallet.app.router;


import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;

public class SendTokenRouter {
    public void open(Context context, String address, String symbol, int decimals, Wallet wallet, Token token, int chainId) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.putExtra(C.EXTRA_AMOUNT, (QrUrlResult)null);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
