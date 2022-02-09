package com.alphawallet.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.Nullable;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@AndroidEntryPoint
public class PriceAlertsService extends Service
{

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

    private Wallet defaultWallet;
    @Inject
    AssetDefinitionService assetDefinitionService;

    @Nullable
    private Disposable heartBeatTimer;

    public class LocalBinder extends Binder
    {
        public PriceAlertsService getService()
        {
            return PriceAlertsService.this;
        }
    }

    private final IBinder mBinder = new PriceAlertsService.LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, Throwable::printStackTrace).isDisposed();

    }

    private void onDefaultWallet(Wallet wallet)
    {
        tokensService.setCurrentAddress(wallet.address);
        assetDefinitionService.startEventListener();
        tokensService.startUpdateCycle();
        defaultWallet = wallet;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        startHearBeatTimer();
        return START_STICKY;
    }

    private void startHearBeatTimer()
    {
        if (heartBeatTimer == null || heartBeatTimer.isDisposed())
        {
            heartBeatTimer = Observable.interval(0, 30, TimeUnit.SECONDS)
                    .doOnNext(l -> heartBeat()).subscribe();
        }
    }

    private void heartBeat()
    {
        if (defaultWallet == null)
        {
            return;
        }

        List<PriceAlert> enabledPriceAlerts = getEnabledPriceAlerts();
        for (PriceAlert priceAlert : enabledPriceAlerts)
        {
            tickerService.convertPair(TickerService.getCurrencySymbolTxt(), priceAlert.getCurrency())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((rate) ->
                    {
                        Token token = tokensService.getToken(priceAlert.getChainId(), priceAlert.getAddress());
                        if (token == null)
                        {
                            return;
                        }
                        TokenTicker tokenTicker = tokensService.getTokenTicker(token);
                        if (tokenTicker == null)
                        {
                            return;
                        }
                        double currentTokenPrice = Double.parseDouble(tokenTicker.price);

                        if (priceAlert.match(rate, currentTokenPrice))
                        {
                            String content = constructContent(priceAlert, Objects.requireNonNull(CurrencyRepository.getCurrencyByISO(priceAlert.getCurrency())));
                            notificationService.displayPriceAlertNotification(priceAlert.getToken(), content, 0, constructIntent(token));
                            // disable notification to avoid run multiple times
                            priceAlert.setEnabled(false);
                            updatePriceAlerts(enabledPriceAlerts);
                        }
                    }, Throwable::printStackTrace).isDisposed();
        }
    }

    private List<PriceAlert> getEnabledPriceAlerts()
    {
        List<PriceAlert> enabledPriceAlerts = new ArrayList<>();
        for (PriceAlert pa : getPriceAlerts())
        {
            if (pa.isEnabled())
            {
                enabledPriceAlerts.add(pa);
            }
        }
        return enabledPriceAlerts;
    }

    private List<PriceAlert> getPriceAlerts()
    {
        String json = preferenceRepository.getPriceAlerts();
        if (json.isEmpty())
        {
            return new ArrayList<>();
        }
        return new Gson().fromJson(json, new TypeToken<List<PriceAlert>>()
        {
        }.getType());
    }

    private void updatePriceAlerts(List<PriceAlert> priceAlerts)
    {
        String json = new Gson().toJson(priceAlerts, new TypeToken<List<PriceAlert>>()
        {
        }.getType());
        preferenceRepository.setPriceAlerts(json);
    }

    private Intent constructIntent(Token token)
    {
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, token.getAddress());
        return tokenDetailRouter.makeERC20DetailsIntent(this, token.getAddress(), token.tokenInfo.symbol, token.tokenInfo.decimals,
                !token.isEthereum(), defaultWallet, token, hasDefinition);
    }

    private String constructContent(PriceAlert priceAlert, CurrencyItem currencyItem)
    {
        return getIndicatorText(priceAlert.getAbove()) + " " + currencyItem.getSymbol() + TickerService.getCurrencyWithoutSymbol(Double.parseDouble(priceAlert.getValue()));
    }

    private String getIndicatorText(boolean isAbove)
    {
        if (isAbove)
        {
            return getString(R.string.price_alert_indicator_above);
        }
        return getString(R.string.price_alert_indicator_below);
    }
}