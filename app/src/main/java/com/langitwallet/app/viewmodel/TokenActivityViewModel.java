package com.langitwallet.app.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.langitwallet.app.entity.ActivityMeta;
import com.langitwallet.app.entity.Wallet;
import com.langitwallet.app.interact.FetchTransactionsInteract;
import com.langitwallet.app.service.AssetDefinitionService;
import com.langitwallet.app.service.TokensService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.realm.Realm;

@HiltViewModel
public class TokenActivityViewModel extends BaseViewModel {
    private final MutableLiveData<ActivityMeta[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;

    @Inject
    public TokenActivityViewModel(AssetDefinitionService assetDefinitionService,
                                  FetchTransactionsInteract fetchTransactionsInteract,
                                  TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.tokensService = tokensService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public FetchTransactionsInteract getTransactionsInteract()
    {
        return fetchTransactionsInteract;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return this.assetDefinitionService;
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokensService.getRealmInstance(wallet);
    }
}
