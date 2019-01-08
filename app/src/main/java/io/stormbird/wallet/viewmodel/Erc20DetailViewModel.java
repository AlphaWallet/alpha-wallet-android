package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

public class Erc20DetailViewModel extends BaseViewModel {
    private static final long CHECK_ETHPRICE_INTERVAL = 10;
    private final MutableLiveData<Double> ethPrice = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final AssetDefinitionService assetDefinitionService;

    public Erc20DetailViewModel(MyAddressRouter myAddressRouter,
                                FetchTokensInteract fetchTokensInteract,
                                AssetDefinitionService assetDefinitionService) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
    }

    public LiveData<Double> ethPriceReading() {
        return ethPrice;
    }

    public void showMyAddress(Context context, Wallet wallet) {
        myAddressRouter.open(context, wallet);
    }

    public void showContractInfo(Context ctx, String contractAddress) {
        myAddressRouter.open(ctx, contractAddress);
    }

    public void startEthereumTicker() {
        disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .getEthereumTicker()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTicker, this::onError)).subscribe();
    }

    private void onTicker(Ticker ticker) {
        if (ticker != null && ticker.price_usd != null) {
            ethPrice.postValue(Double.valueOf(ticker.price_usd));
        }
    }

    public boolean hasIFrame(String address) {
        return assetDefinitionService.hasIFrame(address);
    }

    public String getTokenData(String address) {
        return assetDefinitionService.getIntroductionCode(address);
    }
}
