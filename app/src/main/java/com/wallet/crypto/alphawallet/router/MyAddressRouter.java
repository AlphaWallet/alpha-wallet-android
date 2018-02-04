package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.MyAddressActivity;

import static com.wallet.crypto.alphawallet.C.Key.WALLET;

public class MyAddressRouter {

    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, MyAddressActivity.class);
        intent.putExtra(WALLET, wallet);
        context.startActivity(intent);
    }
}
