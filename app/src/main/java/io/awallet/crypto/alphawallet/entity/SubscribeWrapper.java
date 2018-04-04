package io.awallet.crypto.alphawallet.entity;

import android.os.Handler;

import rx.functions.Action1;
import org.web3j.protocol.core.methods.response.Transaction;

/**
 * Created by James on 1/02/2018.
 */

public class SubscribeWrapper
{
    public Action1<Transaction> scanReturn;
    public Handler wrapperInteraction;

    public SubscribeWrapper(Action1<Transaction> s)
    {
        scanReturn = s;
    }

    public Action1<Throwable> onError = (e) -> {
        e.printStackTrace();
        System.out.println("TH: " + e.getMessage());
    };
}
