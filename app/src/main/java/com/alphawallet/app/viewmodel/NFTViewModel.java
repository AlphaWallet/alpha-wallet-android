package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.router.SendTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.TokenDefinition;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class NFTViewModel extends BaseViewModel {
    private final MutableLiveData<ActivityMeta[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    @Inject
    public NFTViewModel(FetchTransactionsInteract fetchTransactionsInteract,
                        AssetDefinitionService assetDefinitionService,
                        TokensService tokensService)
    {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<XMLDsigDescriptor> sig()
    {
        return sig;
    }

    public LiveData<Boolean> newScriptFound()
    {
        return newScriptFound;
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
        assetDefinitionService.checkServerForScript(token.tokenInfo.chainId, token.getAddress())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.single())
                .subscribe(this::handleFilename, this::onError)
                .isDisposed();
    }

    private void handleFilename(TokenDefinition td)
    {
        if (!TextUtils.isEmpty(td.holdingToken)) newScriptFound.postValue(true);
    }

    public void restartServices()
    {
        fetchTransactionsInteract.restartTransactionService();
    }

    public Intent openSelectionModeIntent(Context context, Token token, Wallet wallet)
    {
        Intent intent = new Intent(context, Erc1155AssetSelectActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.Key.WALLET, wallet);
        return intent;
    }
}
