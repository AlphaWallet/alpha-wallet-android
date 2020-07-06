package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.SendTokenRouter;
import com.alphawallet.app.router.TransactionDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.tools.TokenDefinition;

import static com.alphawallet.app.ui.Erc20DetailActivity.HISTORY_LENGTH;

public class Erc20DetailViewModel extends BaseViewModel {

    private final MutableLiveData<ActivityMeta[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public Erc20DetailViewModel(MyAddressRouter myAddressRouter,
                                FetchTransactionsInteract fetchTransactionsInteract,
                                TransactionDetailRouter transactionDetailRouter,
                                AssetDefinitionService assetDefinitionService,
                                TokensService tokensService) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<ActivityMeta[]> transactions() {
        return transactions;
    }

    public LiveData<XMLDsigDescriptor> sig() { return sig; }

    public LiveData<Boolean> newScriptFound() { return newScriptFound; }

    public void showMyAddress(Context context, Wallet wallet, Token token) {
        myAddressRouter.open(context, wallet, token);
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token) {
        myAddressRouter.open(ctx, wallet, token);
    }

    public TokensService getTokensService() {
        return tokensService;
    }

    public FetchTransactionsInteract getTransactionsInteract() {
        return fetchTransactionsInteract;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return this.assetDefinitionService;
    }

    public void showDetails(Context context, Wallet wallet, Transaction transaction) {
        transactionDetailRouter.open(context, transaction, wallet);
    }

    public void prepare(Token token, Wallet wallet)
    {
        progress.postValue(true);
        disposable =
                fetchTransactionsInteract.fetchTransactionMetas(wallet, token.tokenInfo.chainId, token.getAddress(), HISTORY_LENGTH)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTransactions, this::onError);
    }

    private void onTransactions(ActivityMeta[] activityMetas)
    {
        transactions.postValue(activityMetas);
    }

    public void showSendToken(Context ctx, Wallet wallet, Token token)
    {
        if (token != null)
        {
            new SendTokenRouter().open(ctx, wallet.address, token.getSymbol(), token.tokenInfo.decimals,
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

    private void handleFilename(String newFile)
    {
        if (!TextUtils.isEmpty(newFile)) newScriptFound.postValue(true);
    }
}
