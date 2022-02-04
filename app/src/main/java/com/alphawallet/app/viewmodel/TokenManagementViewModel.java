package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class TokenManagementViewModel extends BaseViewModel {
    private final TokenRepositoryType tokenRepository;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final MutableLiveData<TokenCardMeta[]> tokens = new MutableLiveData<>();

    private Disposable fetchTokensDisposable;

    @Inject
    public TokenManagementViewModel(TokenRepositoryType tokenRepository,
                                    ChangeTokenEnableInteract changeTokenEnableInteract,
                                    AssetDefinitionService assetDefinitionService,
                                    TokensService tokensService) {
        this.tokenRepository = tokenRepository;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<TokenCardMeta[]> tokens() {
        return tokens;
    }

    public void fetchTokens(Wallet wallet) {
        fetchTokensDisposable = tokenRepository.fixFullNames(wallet, assetDefinitionService) //first ensure we fix up the names in the DB to get easy filter action
                .flatMap(count -> tokenRepository.fetchAllTokenMetas(wallet, tokensService.getNetworkFilters(), ""))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokensFetched, this::onError);
    }

    private void onTokensFetched(TokenCardMeta[] tokenArray) {
        tokens.postValue(tokenArray);
        fetchTokensDisposable.dispose();
    }

    public void setTokenEnabled(Wallet wallet, Token token, boolean enabled) {
        changeTokenEnableInteract.setEnable(wallet, token, enabled);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokensService.getRealmInstance(wallet);
    }
}
