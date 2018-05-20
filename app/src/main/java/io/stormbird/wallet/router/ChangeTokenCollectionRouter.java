package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.TokenChangeCollectionActivity;

import static io.stormbird.wallet.C.Key.WALLET;

public class ChangeTokenCollectionRouter {

    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, TokenChangeCollectionActivity.class);
        intent.putExtra(WALLET, wallet);
        context.startActivity(intent);
    }
}
