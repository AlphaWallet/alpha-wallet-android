package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigInteger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class SendViewModel extends BaseViewModel {
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final GasService gasService;
    private final AssetDefinitionService assetDefinitionService;

    public SendViewModel(ConfirmationRouter confirmationRouter,
                         MyAddressRouter myAddressRouter,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         FetchTransactionsInteract fetchTransactionsInteract,
                         AddTokenInteract addTokenInteract,
                         GasService gasService,
                         AssetDefinitionService assetDefinitionService) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.addTokenInteract = addTokenInteract;
        this.gasService = gasService;
        this.assetDefinitionService = assetDefinitionService;
    }

    public MutableLiveData<Token> tokenFinalised() { return finalisedToken; }

    public void openConfirmation(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens, BigInteger gasPrice, String ensDetails, int chainId) {
        confirmationRouter.open(context, to, amount, contractAddress, decimals, symbol, sendingTokens, gasPrice, ensDetails, chainId);
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token)
    {
        myAddressRouter.open(ctx, wallet, token);
    }

    public NetworkInfo getNetworkInfo(int chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }

    public Token getToken(int chainId, String tokenAddress) { return tokensService.getToken(chainId, tokenAddress); };

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public void fetchToken(int chainId, String address, String walletAddress)
    {
        tokensService.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokenInfo -> gotTokenUpdate(tokenInfo, walletAddress), this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo, String walletAddress)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, new Wallet(walletAddress)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(finalisedToken::postValue, this::onError);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }
}
