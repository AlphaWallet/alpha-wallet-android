package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.PriceAlertItem;

import java.util.ArrayList;

public class TokenAlertsViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final MutableLiveData<ArrayList<PriceAlertItem>> priceAlerts = new MutableLiveData<>();

    public TokenAlertsViewModel(AssetDefinitionService assetDefinitionService,
                                TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<ArrayList<PriceAlertItem>> priceAlerts()
    {
        return priceAlerts;
    }

    public void fetchStoredPriceAlerts() {
        ArrayList<PriceAlertItem> storedPriceAlerts = new ArrayList<>();

        // TODO
        storedPriceAlerts.add(new PriceAlertItem("3000", false, true));
        storedPriceAlerts.add(new PriceAlertItem("5000", true, false));

        priceAlerts.postValue(storedPriceAlerts);
    }

    public void openAddPriceAlertMenu()
    {
        // TODO
    }
}
