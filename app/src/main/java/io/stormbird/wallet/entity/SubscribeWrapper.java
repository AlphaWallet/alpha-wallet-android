package io.stormbird.wallet.entity;

import android.os.Handler;

import rx.functions.Action1;
import org.web3j.protocol.core.methods.response.Transaction;

/**
 * Created by James on 1/02/2018.
 */

public class SubscribeWrapper
{
    public Action1<Transaction> scanReturn;
    public Action1<Throwable> onError;
    public Handler wrapperInteraction;

    public SubscribeWrapper(Action1<Transaction> s, Action1<Throwable> onError)
    {
        scanReturn = s;
        this.onError = onError;
    }
}
