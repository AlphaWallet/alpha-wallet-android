package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.TokenChangeCollectionActivity;

import static io.awallet.crypto.alphawallet.C.Key.WALLET;

public class ChangeTokenCollectionRouter {

    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, TokenChangeCollectionActivity.class);
        intent.putExtra(WALLET, wallet);
        context.startActivity(intent);
    }
}
