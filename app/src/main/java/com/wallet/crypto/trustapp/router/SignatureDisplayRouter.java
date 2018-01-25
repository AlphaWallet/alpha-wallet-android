package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.SignatureDisplayActivity;

import static com.wallet.crypto.trustapp.C.Key.WALLET;

/**
 * Created by James on 25/01/2018.
 */

public class SignatureDisplayRouter {
    public void open(Context context, Wallet wallet) {
        Intent intent = new Intent(context, SignatureDisplayActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}