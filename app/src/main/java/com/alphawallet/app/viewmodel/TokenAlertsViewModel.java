package com.alphawallet.app.viewmodel;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.SetPriceAlertActivity;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TokenAlertsViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final PreferenceRepositoryType preferenceRepository;
    private final TokensService tokensService;

    private final MutableLiveData<ArrayList<PriceAlert>> priceAlerts = new MutableLiveData<>();

    public TokenAlertsViewModel(AssetDefinitionService assetDefinitionService,
                                PreferenceRepositoryType preferenceRepository,
                                TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.preferenceRepository = preferenceRepository;
        this.tokensService = tokensService;
    }

    public LiveData<ArrayList<PriceAlert>> priceAlerts()
    {
        return priceAlerts;
    }

    public void fetchStoredPriceAlerts()
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        ArrayList<PriceAlert> list;
        String json = preferenceRepository.getPriceAlerts();
        if (!json.isEmpty())
        {
            list = new Gson().fromJson(json, listType);
        }
        else
        {
            list = new ArrayList<>();
        }
        priceAlerts.postValue(list);
    }

    public void saveAlert(PriceAlert priceAlert)
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        String json = preferenceRepository.getPriceAlerts();

        ArrayList<PriceAlert> list = json.isEmpty()? new ArrayList<>() : new Gson().fromJson(json, listType);;

        list.add(priceAlert);

        String updatedJson = new Gson().toJson(list, listType);

        preferenceRepository.setPriceAlerts(updatedJson);
    }

    public void openAddPriceAlertMenu(Fragment fragment, Token token, int requestCode)
    {
        Intent intent = new Intent(fragment.getContext(), SetPriceAlertActivity.class);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        fragment.startActivityForResult(intent, requestCode);
    }
}
