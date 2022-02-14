package com.alphawallet.app.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.TokenLocator;
import com.alphawallet.app.service.AssetDefinitionService;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class TokenScriptManagementViewModel extends BaseViewModel {

    private final AssetDefinitionService assetDefinitionService;
    private final MutableLiveData<List<TokenLocator>> tokenLocatorsLiveData;

    @Inject
    public TokenScriptManagementViewModel(AssetDefinitionService assetDefinitionService) {
        this.assetDefinitionService = assetDefinitionService;
        tokenLocatorsLiveData = new MutableLiveData<>();
    }

    public void onPrepare(boolean refresh)
    {
        assetDefinitionService.getAllTokenDefinitions(refresh) //holds for loading complete then returns origin contracts
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::addUnresolvedTokens)
                .isDisposed();
    }

    private void addUnresolvedTokens(List<TokenLocator> tokenLocators) {
        tokenLocatorsLiveData.setValue(tokenLocators);
    }

    public MutableLiveData<List<TokenLocator>> getTokenLocatorsLiveData() {
        return tokenLocatorsLiveData;
    }

    public AssetDefinitionService getAssetService()
    {
        return assetDefinitionService;
    }
}
