package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.OrderContractAddressPair;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;
import com.wallet.crypto.alphawallet.ui.widget.entity.SalesOrderSortedItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
    private final MutableLiveData<OrderContractAddressPair> tokenBalance = new MutableLiveData<>();
    private final MutableLiveData<String> selection = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> startUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> endUpdate = new MutableLiveData<>();

    private List<SalesOrder> orders = new ArrayList<>();
    private List<OrderContractAddressPair> checkingPairs = new ArrayList<>();

    private List<Token> tokens = new ArrayList<>();
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();

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
    public LiveData<OrderContractAddressPair> updateBalance() {
        return tokenBalance;
    }
    public LiveData<Boolean> startUpdate() {
        return startUpdate;
    }
    public LiveData<Boolean> endUpdate() {
        return endUpdate;
    }

    public void prepare() {
        tokens.clear();
        checkingPairs.clear();
        //1. Get the wallet address (used to get all cached tokens)
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    //2. Now fetch the list of cached tokens for this address
    private void onDefaultWallet(Wallet wallet)
    {
        orders.clear();
        defaultWallet.setValue(wallet);
        //fetch tokens then go to step 3
        disposable = fetchTokensInteract
                .fetchStored(defaultWallet.getValue())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokens, this::onError, this::fetchMarketOrders);
    }

    //2a. Gather the tokens
    private void onTokens(Token[] tokens) {
        this.tokens = new ArrayList<>(Arrays.asList(tokens));
        for (Token t : tokens)
        {
            tokenMap.put(t.getAddress(), t);
        }
    }

    //3. consume the tokens list and get orders for each token
    private void fetchMarketOrders() {
        if (tokens.size() == 0)
        {
            postOrdersToUI(); //once list has been consumed, go to step 4.
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

    //3a. Add each set of orders, make a note of contract address and post to UI
    private void onSalesOrders(SalesOrder[] tradeInstances)
    {
        if (tradeInstances.length > 0) {
            orders.addAll(Arrays.asList(tradeInstances));
            for (SalesOrder so : tradeInstances)
            {
                Token orderToken = tokenMap.get(so.contractAddress);
                //get the token info for display
                so.tokenInfo = orderToken.tokenInfo;
                //ecrecover owner key
                so.ownerAddress = SalesOrder.getOwnerKey(so);

                //add pair if we haven't already
                OrderContractAddressPair.addPair(checkingPairs, so);
            }
        }
    }

    //4. Post the orders list to the UI and continue with fetching the balances
    private void postOrdersToUI()
    {
        SalesOrder[] compiledOrders = orders.toArray(new SalesOrder[orders.size()]);
        //We can add to the list here, start displaying order details
        market.postValue(compiledOrders);
        checkOrderBalances();
    }

    //5. Now consume the generated token/address pairs we found in step 3 to see if the balance is still valid
    private void checkOrderBalances()
    {
        if (checkingPairs.size() == 0)
        {
            finishOrders();
        }
        else
        {
            OrderContractAddressPair pair = checkingPairs.remove(0);
            Token t = tokenMap.get(pair.order.contractAddress);
            //String orderAddress = defaultWallet.getValue().address;
            //get owner balance
            fetchTokensInteract.updateBalancePair(t, pair.order)
                    .subscribe(this::onBalance, this::onError, this::checkOrderBalances);
        }
    }

    private void finishOrders()
    {
        //now update UI again
        SalesOrder[] compiledOrders = orders.toArray(new SalesOrder[orders.size()]);
        //We can add to the list here, start displaying order details
        market.postValue(compiledOrders);
        System.out.println("Finished displaying orders");
    }

    private void onBalance(OrderContractAddressPair pair)
    {
        //now update the orders
        for (SalesOrder order : orders)
        {
            //updating this item?
            if (    order.contractAddress.equals(pair.order.contractAddress) //order address matches
                    &&  order.ownerAddress.equals(pair.order.ownerAddress))
            {
                order.balanceInfo = pair.balance;
            }
        }
    }

    //Context context, Token token, SalesOrder instance
    public void showPurchaseTicket(Context context, SalesOrder instance)
    {
        marketBuyRouter.open(context, instance);
    }
}
