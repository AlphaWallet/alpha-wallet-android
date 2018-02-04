package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.SignatureDisplayActivity;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 25/01/2018.
 */

public class SignatureDisplayRouter {
    public void open(Context context, Wallet wallet, Ticket token) {
        Intent intent = new Intent(context, SignatureDisplayActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(TICKET, token);
        context.startActivity(intent);
    }
}