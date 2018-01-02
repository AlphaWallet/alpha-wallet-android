package com.wallet.crypto.trustapp.router;


import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.ui.ConfirmationActivity;

public class ConfirmationRouter {
    public void open(Context context, String to, String amount) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_AMOUNT, amount);
        context.startActivity(intent);
    }
}
