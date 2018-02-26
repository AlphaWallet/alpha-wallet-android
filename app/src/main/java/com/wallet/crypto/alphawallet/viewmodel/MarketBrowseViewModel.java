package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.MarketInstance;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBrowseViewModel extends BaseViewModel
{
    private static final long CHECK_MARKET_INTERVAL = 30;

    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;

    private final MutableLiveData<MarketInstance[]> market = new MutableLiveData<>();
    private final MutableLiveData<String> selection = new MutableLiveData<>();

    @Nullable
    private Disposable checkMarketDisposable;

    public MarketBrowseViewModel(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter)
    {
        this.marketQueueService = marketQueueService;
        this.marketBuyRouter = marketBuyRouter;
    }

    public LiveData<MarketInstance[]> updateMarket() {
        return market;
    }

    public void prepare()
    {
        String contractAddr = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";
        disposable = marketQueueService
                .fetchMarketOrders(contractAddr)
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::onMarketOrders, this::onError);
    }

    private void onMarketOrders(MarketInstance[] tradeInstances)
    {
        market.postValue(tradeInstances);
    }

    //Context context, Token token, MarketInstance instance
    public void showPurchaseTicket(Context context, MarketInstance instance)
    {
        marketBuyRouter.open(context, instance);
    }
}
