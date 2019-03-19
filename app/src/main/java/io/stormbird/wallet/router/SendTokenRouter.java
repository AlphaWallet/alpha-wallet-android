package io.stormbird.wallet.router;


import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.QrUrlResult;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.SendActivity;

import static io.stormbird.wallet.C.Key.WALLET;

public class SendTokenRouter {
    public void open(Context context, String address, String symbol, int decimals, boolean isToken, Wallet wallet, Token token) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, isToken);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.putExtra(C.EXTRA_AMOUNT, (QrUrlResult)null);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
