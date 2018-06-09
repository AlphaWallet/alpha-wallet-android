package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.MyAddressActivity;

import static io.stormbird.wallet.C.EXTRA_CONTRACT_ADDRESS;
import static io.stormbird.wallet.C.Key.WALLET;

public class MyAddressRouter {

    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, MyAddressActivity.class);
        intent.putExtra(WALLET, wallet);
        context.startActivity(intent);
    }

    public void open(Context context, String contractAddress) {
        Intent intent = new Intent(context, MyAddressActivity.class);
        intent.putExtra(EXTRA_CONTRACT_ADDRESS, contractAddress);
        context.startActivity(intent);
    }
}
