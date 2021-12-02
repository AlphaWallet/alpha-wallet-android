package com.alphawallet.app.entity;

import android.util.Pair;

/**
 * Created by JB on 2/12/2021.
 */
public interface SyncCallback
{
    void syncCompleted(Wallet wallet, Pair<Double, Double> value);
    void syncStarted(Wallet wallet, Pair<Double, Double> value);
}
