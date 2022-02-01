package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.SetPriceAlertActivity;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TokenAlertsViewModel extends BaseViewModel {
    private final PreferenceRepositoryType preferenceRepository;
    private final TokensService tokensService;
    private final TickerService tickerService;
    private final MutableLiveData<List<PriceAlert>> priceAlerts = new MutableLiveData<>();
    private Token token;

    @Inject
    public TokenAlertsViewModel(PreferenceRepositoryType preferenceRepository,
                                TokensService tokensService, TickerService tickerService)
    {
        this.preferenceRepository = preferenceRepository;
        this.tokensService = tokensService;
        this.tickerService = tickerService;
    }

    public LiveData<List<PriceAlert>> priceAlerts()
    {
        return priceAlerts;
    }

    public TokensService getTokensService() { return tokensService; }

    public void fetchStoredPriceAlerts(Token token)
    {
        this.token = token;

        List<PriceAlert> list = getPriceAlerts();
        priceAlerts.postValue(filterByToken(list));
    }

    private List<PriceAlert> filterByToken(List<PriceAlert> source)
    {
        List<PriceAlert> filteredList = new ArrayList<>();
        for (PriceAlert p : source)
        {
            if (p.getToken().equals(token.tokenInfo.name))
            {
                filteredList.add(p);
            }
        }
        return filteredList;
    }

    public void saveAlert(PriceAlert priceAlert)
    {
        tickerService.convertPair(TickerService.getCurrencySymbolTxt(), priceAlert.getCurrency())
                .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe((rate) -> {
                // check if current price is higher than in alert, mark as 'drops to' or 'rises above' otherwise
                double currentTokenPrice = Double.parseDouble(tokensService.getTokenTicker(token).price) * rate;
                double alertPrice = Double.parseDouble(priceAlert.getValue());
                priceAlert.setAbove(alertPrice > currentTokenPrice);

                List<PriceAlert> list = getPriceAlerts();
                list.add(priceAlert);

                updateStoredAlerts(list);
            }, Throwable::printStackTrace).isDisposed();
    }

    private List<PriceAlert> getPriceAlerts()
    {
        Type listType = new TypeToken<List<PriceAlert>>()
        {
        }.getType();
        String json = preferenceRepository.getPriceAlerts();
        return json.isEmpty() ? new ArrayList<>() : new Gson().fromJson(json, listType);
    }

    public void updateStoredAlerts(List<PriceAlert> items)
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();
        String updatedJson = items.isEmpty() ? "" : new Gson().toJson(items, listType);
        preferenceRepository.setPriceAlerts(updatedJson);
        priceAlerts.postValue(filterByToken(items));
    }
}
