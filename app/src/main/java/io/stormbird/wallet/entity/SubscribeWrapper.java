package io.stormbird.wallet.entity;

import android.os.Handler;

import io.reactivex.Flowable;
import org.web3j.protocol.core.methods.response.Transaction;

/**
 * Created by James on 1/02/2018.
 */

public interface SubscribeWrapper
{
    void scanReturn(Transaction tx);
}
