package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.ui.ImportTokenActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Wallet;

import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TokensService;

import java.math.BigInteger;

public class SendViewModel extends BaseViewModel {
    private final MutableLiveData<String> ensResolve = new MutableLiveData<>();
    private final MutableLiveData<String> ensFail = new MutableLiveData<>();
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final ENSInteract ensInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final SetupTokensInteract setupTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GasService gasService;

    public SendViewModel(ConfirmationRouter confirmationRouter,
                         MyAddressRouter myAddressRouter,
                         ENSInteract ensInteract,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         SetupTokensInteract setupTokensInteract,
                         FetchTransactionsInteract fetchTransactionsInteract,
                         AddTokenInteract addTokenInteract,
                         FetchTokensInteract fetchTokensInteract,
                         GasService gasService) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.ensInteract = ensInteract;
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.setupTokensInteract = setupTokensInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.addTokenInteract = addTokenInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.gasService = gasService;
    }

    public LiveData<String> ensResolve() { return ensResolve; }
    public LiveData<String> ensFail() { return ensFail; }
    public MutableLiveData<Token> tokenFinalised() { return finalisedToken; }

    public void openConfirmation(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens, String ensDetails, int chainId) {
        confirmationRouter.open(context, to, amount, contractAddress, decimals, symbol, sendingTokens, ensDetails, chainId);
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token)
    {
        myAddressRouter.open(ctx, wallet, token);
    }

    public String getChainName(int chainId)
    {
        return networkRepository.getNameById(chainId);
    }

    public NetworkInfo getNetworkInfo(int chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }

    public Token getToken(int chainId, String tokenAddress) { return tokensService.getToken(chainId, tokenAddress); };
    public void checkENSAddress(int chainId, String name)
    {
        disposable = ensInteract.checkENSAddress(chainId, name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ensResolve::postValue, throwable -> ensFail.postValue(""));
    }

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    //TODO: these functions should be in TokensService
    public void fetchToken(int chainId, String address, String walletAddress)
    {
        setupTokensInteract.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokenInfo -> gotTokenUpdate(tokenInfo, walletAddress), this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo, String walletAddress)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, new Wallet(walletAddress)))
                .flatMap(token -> fetchTokensInteract.updateDefaultBalance(token, new Wallet(walletAddress)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(finalisedToken::postValue, this::onError);
    }

    public void startGasPriceChecker(int chainId)
    {
        gasService.startGasListener(chainId);
    }

    public void stopGasPriceChecker()
    {
        gasService.stopGasListener();
    }
}
