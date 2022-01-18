package com.alphawallet.app.viewmodel;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
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

public class TokenAlertsViewModel extends BaseViewModel {
    private final PreferenceRepositoryType preferenceRepository;
    private final TokensService tokensService;
    private final TickerService tickerService;
    private final MutableLiveData<List<PriceAlert>> priceAlerts = new MutableLiveData<>();
    private Token token;

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

        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        String json = preferenceRepository.getPriceAlerts();

        List<PriceAlert> list = json.isEmpty() ? new ArrayList<>() : new Gson().fromJson(json, listType);

        priceAlerts.postValue(getFilteredList(list));
    }

    private List<PriceAlert> getFilteredList(List<PriceAlert> source)
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

    public void openAddPriceAlertMenu(Fragment fragment, int requestCode)
    {
        Intent intent = new Intent(fragment.getContext(), SetPriceAlertActivity.class);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        fragment.startActivityForResult(intent, requestCode); //Samoa TODO: de-deprecate (see ActivityResultLauncher paradigm in eg DappBrowserFragment)
    }

    public void saveAlert(PriceAlert priceAlert)
    {
        tickerService.convertPair(priceAlert.getCurrency(), TickerService.getCurrencySymbolTxt())
                .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe((rate) -> {
                // check if current price is higher than in alert, mark as 'drops to' or 'rises above' otherwise
                double currentTokenPrice = Double.parseDouble(tokensService.getTokenTicker(token).price);
                double alertPrice = Double.parseDouble(priceAlert.getValue()) * rate;
                priceAlert.setAbove(currentTokenPrice < alertPrice);
                Type listType = new TypeToken<List<PriceAlert>>() {}.getType();
                String json = preferenceRepository.getPriceAlerts();
                ArrayList<PriceAlert> list = json.isEmpty() ? new ArrayList<>() : new Gson().fromJson(json, listType);
                list.add(priceAlert);
                String updatedJson = new Gson().toJson(list, listType);
                preferenceRepository.setPriceAlerts(updatedJson);
                priceAlerts.postValue(getFilteredList(list));
            }, Throwable::printStackTrace).isDisposed();
    }

    public void updateStoredAlerts(List<PriceAlert> items)
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        String updatedJson = items.isEmpty() ? "" : new Gson().toJson(items, listType);

        preferenceRepository.setPriceAlerts(updatedJson);

        priceAlerts.postValue(getFilteredList(items));
    }
}
