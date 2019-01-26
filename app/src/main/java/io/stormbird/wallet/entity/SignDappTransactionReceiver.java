package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import io.stormbird.wallet.C;
import io.stormbird.wallet.web3.entity.Web3Transaction;

import static io.stormbird.wallet.C.SIGN_DAPP_TRANSACTION;

/**
 * Created by James on 26/01/2019.
 * Stormbird in Singapore
 */
public class SignDappTransactionReceiver extends BroadcastReceiver
{
    private final SignTransactionInterface signInterface;
    public SignDappTransactionReceiver(Activity ctx, SignTransactionInterface signInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(SIGN_DAPP_TRANSACTION));
        this.signInterface = signInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.getAction() != null)
        {
            //Bundle bundle = intent.getExtras();
            switch (intent.getAction())
            {
                case SIGN_DAPP_TRANSACTION:
                    Web3Transaction tx = intent.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
                    boolean success = intent.getBooleanExtra(C.EXTRA_SUCCESS, false);
                    String txHex = intent.getStringExtra(C.EXTRA_HEXDATA);
                    signInterface.signTransaction(tx, txHex, success);
                    break;
                default:
                    break;
            }
        }
    }
}
