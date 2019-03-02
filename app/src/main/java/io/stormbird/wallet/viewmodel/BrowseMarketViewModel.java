package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.entity.OrderContractAddressPair;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.MarketBuyRouter;
import io.stormbird.wallet.service.MarketQueueService;

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
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.tools.ParseMagicLink;

/**
 * Created by James on 19/02/2018.
 */

public class BrowseMarketViewModel extends BaseViewModel
{
    private static final long CHECK_MARKET_INTERVAL = 30;

    private static ParseMagicLink parser;
    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;

    private final MutableLiveData<MagicLinkData[]> market = new MutableLiveData<>();
    private final MutableLiveData<OrderContractAddressPair> tokenBalance = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> startUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> endUpdate = new MutableLiveData<>();

    private List<MagicLinkData> orders = new ArrayList<>();
    private List<OrderContractAddressPair> checkingPairs = new ArrayList<>();
    private List<OrderContractAddressPair> staticPairList = new ArrayList<>();

    private List<Token> tokens = new ArrayList<>();
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private boolean refreshUINeeded = false;

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

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions());
        }
    }

    public LiveData<MagicLinkData[]> updateMarket() {
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

    @Override
    public void onCleared()
    {
        super.onCleared();
        if (checkMarketDisposable != null && !checkMarketDisposable.isDisposed())
        {
            checkMarketDisposable.dispose();
        }
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onSalesOrders, this::onError, this::fetchMarketOrders);
        }
    }

    //3a. Add each set of orders, make a note of contract address and post to UI
    private void onSalesOrders(MagicLinkData[] tradeInstances)
    {
        initParser();
        if (tradeInstances.length > 0) {
            orders.addAll(Arrays.asList(tradeInstances));
            for (MagicLinkData so : tradeInstances)
            {
                Token orderToken = tokenMap.get(so.contractAddress);
                //get the token info for display
                so.contractName = orderToken.getFullName();// .tokenInfo = orderToken.tokenInfo;

                so.ownerAddress = parser.getOwnerKey(so);

                //add pair if we haven't already
                OrderContractAddressPair.addPair(checkingPairs, so);
            }
        }
    }


    //4. Post the orders list to the UI and continue with fetching the balances
    private void postOrdersToUI()
    {
        staticPairList.addAll(checkingPairs);
        MagicLinkData[] compiledOrders = orders.toArray(new MagicLinkData[orders.size()]);
        //We can add to the list here, start displaying order details
        market.postValue(compiledOrders);
        checkingBalanceCycle();
    }

    //5. start the cycle to check balances
    private void checkingBalanceCycle()
    {
        checkMarketDisposable = Observable.interval(0, CHECK_MARKET_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(this::checkOrderBalances)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private void checkOrderBalances(Long dummy)
    {
        checkingPairs.clear();
        checkingPairs.addAll(staticPairList);
        checkOrderBalances();
    }

    //6. Now consume the generated token/address pairs we found in step 3 to see if the balance is still valid
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
            if (pair.order.isValidOrder())
            {
                disposable = fetchTokensInteract.updateBalancePair(t, pair.order)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onBalance, this::onError, this::updateUI);
            }
            else
            {
                //leave balance blank
                pair.balance = new ArrayList<>();
                onBalance(pair);
                updateUI();
            }
        }
    }

    private void updateUI()
    {
        if (refreshUINeeded)
        {
            MagicLinkData[] compiledOrders = orders.toArray(new MagicLinkData[orders.size()]);
            //We can add to the list here, start displaying order details
            market.postValue(compiledOrders);
        }
        checkOrderBalances();
    }

    private void finishOrders()
    {

        //System.out.println("Finished displaying orders");
    }

    private void onBalance(OrderContractAddressPair pair)
    {
        refreshUINeeded = true;
        //now update the orders
        for (MagicLinkData order : orders)
        {
            //updating this item?
            if (    order.contractAddress.equals(pair.order.contractAddress) //order address matches
                    && order.ownerAddress.equals(pair.order.ownerAddress)
                    && pair.order.balanceChange(order.balanceInfo)) {
                order.balanceInfo = pair.balance;
                refreshUINeeded = true;
            }
        }
    }

    //Context context, Token token, SalesOrder instance
    public void showPurchaseTicket(Context context, MagicLinkParcel instance)
    {
        marketBuyRouter.open(context, instance);
    }
}
