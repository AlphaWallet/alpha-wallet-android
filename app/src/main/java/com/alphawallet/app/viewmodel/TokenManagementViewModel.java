package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.AddTokenRouter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TokenManagementViewModel extends BaseViewModel {
    private final TokenRepositoryType tokenRepository;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final AddTokenRouter addTokenRouter;

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();

    private Disposable fetchTokensDisposable;

    public TokenManagementViewModel(TokenRepositoryType tokenRepository,
                                    ChangeTokenEnableInteract changeTokenEnableInteract,
                                    AddTokenRouter addTokenRouter) {
        this.tokenRepository = tokenRepository;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.addTokenRouter = addTokenRouter;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }

    public void fetchTokens(String walletAddr) {
        fetchTokensDisposable = tokenRepository.fetchStored(walletAddr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokensFetched, this::onError);
    }

    private void onTokensFetched(Token[] tokenArray) {
        tokens.postValue(tokenArray);
        fetchTokensDisposable.dispose();
    }

    public void setTokenEnabled(Wallet wallet, Token token, boolean enabled) {
        changeTokenEnableInteract.setEnable(wallet, token, enabled);
    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context, null);
    }
}
