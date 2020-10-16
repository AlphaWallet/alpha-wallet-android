package com.alphawallet.app.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class TokenManagementViewModel extends BaseViewModel {
    private final TokenRepositoryType tokenRepository;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final AddTokenRouter addTokenRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final MutableLiveData<TokenCardMeta[]> tokens = new MutableLiveData<>();

    private Disposable fetchTokensDisposable;

    public TokenManagementViewModel(TokenRepositoryType tokenRepository,
                                    ChangeTokenEnableInteract changeTokenEnableInteract,
                                    AddTokenRouter addTokenRouter,
                                    AssetDefinitionService assetDefinitionService,
                                    TokensService tokensService) {
        this.tokenRepository = tokenRepository;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.addTokenRouter = addTokenRouter;
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

    public void showAddToken(Context context) {
        addTokenRouter.open(context, null);
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
