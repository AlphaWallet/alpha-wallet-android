package com.alphawallet.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PriceAlertsService extends Service {

    private static final String TAG = PriceAlertsService.class.getSimpleName();

    @Inject
    TokenLocalSource localSource;
    @Inject
    PreferenceRepositoryType preferenceRepository;
    @Inject
    TokensService tokensService;
    @Inject
    TickerService tickerService;
    @Inject
    NotificationService notificationService;

    @Inject
    TokenDetailRouter tokenDetailRouter;

    @Inject
    GenericWalletInteract genericWalletInteract;

    // @Inject
    private Wallet defaultWallet;
    @Inject
    AssetDefinitionService assetDefinitionService;

    public PriceAlertsService() {
    }

    @Nullable
    private Disposable heartBeatTimer;

    public class LocalBinder extends Binder {
        public PriceAlertsService getService() {
            return PriceAlertsService.this;
        }
    }

    private final IBinder mBinder = new PriceAlertsService.LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);

        genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, Throwable::printStackTrace).isDisposed();

    }

    private void onDefaultWallet(Wallet wallet) {
        tokensService.setCurrentAddress(wallet.address);
        assetDefinitionService.startEventListener();
        tokensService.startUpdateCycle();
        defaultWallet = wallet;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSessionPinger();
        return START_STICKY;
    }

    private void startSessionPinger() {
        if (heartBeatTimer == null || heartBeatTimer.isDisposed()) {
            heartBeatTimer = Observable.interval(0, 30, TimeUnit.SECONDS)
                    .doOnNext(l -> heartBeat()).subscribe();
        }
    }

    private void heartBeat() {

        // get list of the alerts
        Type listType = new TypeToken<List<PriceAlert>>() {
        }.getType();

        ArrayList<PriceAlert> list = null;
        String json = preferenceRepository.getPriceAlerts();
        if (!json.isEmpty()) {
            list = new Gson().fromJson(json, listType);
        } else {
            return ;
        }
        ArrayList<PriceAlert> finalList = list;
        for (PriceAlert priceAlert : finalList) {
            if (!priceAlert.isEnabled() || defaultWallet == null) {
                continue;
            }
            tickerService.convertPair(priceAlert.getCurrency(), TickerService.getCurrencySymbolTxt())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((rate) -> {
                        Token token = tokensService.getToken(priceAlert.getChainId(), priceAlert.getAddress());
                        if (token == null) {
                            return;
                        }
                        double currentTokenPrice = Double.parseDouble(tokensService.getTokenTicker(token).price);
                        double alertPrice = Double.parseDouble(priceAlert.getValue()) * rate;

                        if ((priceAlert.getIndicator() && currentTokenPrice < alertPrice) ||
                                (!priceAlert.getIndicator() && currentTokenPrice > alertPrice)) {
                            // raise alert
                            boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, token.getAddress());
                            Intent intent = tokenDetailRouter.makeERC20DetailsIntent(this, token.getAddress(), token.tokenInfo.symbol, token.tokenInfo.decimals,
                                    !token.isEthereum(), defaultWallet, token, hasDefinition);

                            String content = "";
                            if (priceAlert.getIndicator()) {
                                content = "Above ";
                            } else {
                                content = "Below ";
                            }

                            CurrencyItem currencyItem = CurrencyRepository.getCurrencyByISO(priceAlert.getCurrency());
                            content += currencyItem.getSymbol() + TickerService.getCurrencyWithoutSymbol(new Double(priceAlert.getValue()));

                            notificationService.displayPriceAlertNotification(priceAlert.getToken(), content, 0, intent);
                            // disable notification to avoid run multiple times
                            priceAlert.setEnabled(false);

                            String updatedJson = new Gson().toJson(finalList, listType);
                            preferenceRepository.setPriceAlerts(updatedJson);
                        }
                    }, Throwable::printStackTrace).isDisposed();
        }
    }
}