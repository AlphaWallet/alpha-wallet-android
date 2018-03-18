package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.OrderContractAddressPair;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 19/02/2018.
 */

public class BrowseMarketViewModel extends BaseViewModel
{
    private static final long CHECK_MARKET_INTERVAL = 30;

    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;

    private final MutableLiveData<SalesOrder[]> market = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenBalance = new MutableLiveData<>();
    private final MutableLiveData<String> selection = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    private List<SalesOrder> orders = new ArrayList<>();
    private List<OrderContractAddressPair> checkingPairs = new ArrayList<>();

    private List<Token> tokens = new ArrayList<>();
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private List<String> activeOrderAddresses = new ArrayList<>();

    @Nullable
    private Disposable checkMarketDisposable;

    public BrowseMarketViewModel(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract)
    {
        this.marketQueueService = marketQueueService;
        this.marketBuyRouter = marketBuyRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
    }

    public LiveData<SalesOrder[]> updateMarket() {
        return market;
    }

    public void prepare() {
        activeOrderAddresses.clear();
        tokens.clear();
        checkingPairs.clear();
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    //1. Get the wallet address (used to get all cached tokens)
    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
        //fetch tokens
        disposable = fetchTokensInteract
                .fetchStored(defaultWallet.getValue())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokens, this::onError, this::fetchMarketOrders);
    }

    private void onTokens(Token[] tokens) {
        this.tokens = new ArrayList<>(Arrays.asList(tokens));
        for (Token t : tokens)
        {
            tokenMap.put(t.getAddress(), t);
        }
    }

    //2. consume the tokens list and get orders for each token
    private void fetchMarketOrders() {
        if (tokens.size() == 0)
        {
            checkOrderBalances();
        }
        else
        {
            Token thisToken = tokens.remove(0);
            disposable = marketQueueService
                    .fetchSalesOrders(thisToken.getAddress())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(this::onSalesOrders, this::onError, this::fetchMarketOrders);
        }
    }

    //Add each set of orders, make a note of contract address and post to UI
    private void onSalesOrders(SalesOrder[] tradeInstances)
    {
        if (tradeInstances.length > 0) {
            orders.addAll(Arrays.asList(tradeInstances));
            for (SalesOrder so : tradeInstances)
            {
                Token orderToken = tokenMap.get(so.contractAddress);
                //ecrecover owner key
                String ownerAddress = SalesOrder.getOwnerKey(so);

                //add pair if we haven't already
                OrderContractAddressPair.addPair(checkingPairs, orderToken, ownerAddress);
            }
            activeOrderAddresses.add(tradeInstances[0].contractAddress);

            //We can add to the list here, start displaying order details
            market.postValue(tradeInstances);
        }
    }

    //3. Now run through each contract/address pair we found in the order system to see if the balance is still valid
    private void checkOrderBalances()
    {
        if (checkingPairs.size() == 0)
        {
            finishOrders();
        }
        else
        {
            OrderContractAddressPair pair = checkingPairs.remove(0);
            Token t = pair.orderToken;
            String orderAddress = pair.owningAddress;
            //get owner balance
            fetchTokensInteract.updateBalance(new Wallet(orderAddress), t)
                    .subscribe(this::onBalance, this::onError, this::checkOrderBalances);
        }
    }

    private void finishOrders()
    {
        System.out.println("Finished displaying orders");
    }

    private void onBalance(Token token)
    {
        //update the order info & UI, let the adapter update the value
        tokenBalance.postValue(token);
    }

    //Context context, Token token, SalesOrder instance
    public void showPurchaseTicket(Context context, SalesOrder instance)
    {
        marketBuyRouter.open(context, instance);
    }
}
