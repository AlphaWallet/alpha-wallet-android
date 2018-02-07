package com.wallet.crypto.alphawallet.service;

import android.content.Context;

import io.reactivex.disposables.Disposable;

/**
 * Created by James on 7/02/2018.
 */

public class MarketQueueService
{
    private Disposable marketQueueProcessing;
    private Context context;

    public MarketQueueService(Context ctx) {
        this.context = ctx;
    }

    public void setMarketQueue(Disposable disposable)
    {
        marketQueueProcessing = disposable;
    }

    public Disposable getMarketQueue()
    {
        return marketQueueProcessing;
    }
}
