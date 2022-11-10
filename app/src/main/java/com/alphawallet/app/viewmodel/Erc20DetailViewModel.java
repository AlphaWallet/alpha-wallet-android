package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.OnRampRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.SendTokenRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.TokenDefinition;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class Erc20DetailViewModel extends BaseViewModel {
    private final MutableLiveData<ActivityMeta[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();
    private final MutableLiveData<Boolean> scriptUpdateInProgress = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final OnRampRepositoryType onRampRepository;

    @Nullable
    private Disposable scriptUpdate;

    @Inject
    public Erc20DetailViewModel(MyAddressRouter myAddressRouter,
                                FetchTransactionsInteract fetchTransactionsInteract,
                                AssetDefinitionService assetDefinitionService,
                                TokensService tokensService,
                                OnRampRepositoryType onRampRepository,
                                AnalyticsServiceType analyticsService)
    {
        this.myAddressRouter = myAddressRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.onRampRepository = onRampRepository;
        setAnalyticsService(analyticsService);
    }

    public LiveData<XMLDsigDescriptor> sig()
    {
        return sig;
    }

    public LiveData<Boolean> newScriptFound()
    {
        return newScriptFound;
    }

    public LiveData<Boolean> scriptUpdateInProgress() { return scriptUpdateInProgress; }

    public void showMyAddress(Context context, Wallet wallet, Token token)
    {
        myAddressRouter.open(context, wallet, token);
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token)
    {
        myAddressRouter.open(ctx, wallet, token);
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

    public void showSendToken(Activity act, Wallet wallet, Token token)
    {
        if (token != null)
        {
            new SendTokenRouter().open(act, wallet.address, token.getSymbol(), token.tokenInfo.decimals,
                    wallet, token, token.tokenInfo.chainId);
        }
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokensService.getRealmInstance(wallet);
    }

    public void checkTokenScriptValidity(Token token)
    {
        disposable = assetDefinitionService.getSignatureData(token.tokenInfo.chainId, token.tokenInfo.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig::postValue, this::onSigCheckError);
    }

    private void onSigCheckError(Throwable throwable)
    {
        XMLDsigDescriptor failSig = new XMLDsigDescriptor();
        failSig.result = "fail";
        failSig.type = SigReturnType.NO_TOKENSCRIPT;
        failSig.subject = throwable.getMessage();
        sig.postValue(failSig);
    }

    public void checkForNewScript(Token token)
    {
        //check server for new tokenscript
        scriptUpdate = assetDefinitionService.checkServerForScript(token, scriptUpdateInProgress)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.single())
                .subscribe(this::handleFilename, e -> scriptUpdateInProgress.postValue(false));
    }

    private void handleFilename(TokenDefinition td)
    {
        if (!TextUtils.isEmpty(td.holdingToken))
        {
            newScriptFound.postValue(true);
        }
        else
        {
            scriptUpdateInProgress.postValue(false);
        }
    }

    public void restartServices()
    {
        fetchTransactionsInteract.restartTransactionService();
    }

    public Intent getBuyIntent(String address, Token token)
    {
        Intent intent = new Intent();
        intent.putExtra(C.DAPP_URL_LOAD, onRampRepository.getUri(address, token));
        return intent;
    }

    public OnRampRepositoryType getOnRampRepository() {
        return onRampRepository;
    }

    @Override
    public void onCleared()
    {
        super.onCleared();
        if (scriptUpdate != null && !scriptUpdate.isDisposed())
        {
            scriptUpdate.dispose();
        }
    }
}
