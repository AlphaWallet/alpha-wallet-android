package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import android.content.Intent;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.AddTokenActivity;
import io.stormbird.wallet.ui.SendActivity;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final HomeRouter homeRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final MutableLiveData<Boolean> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> update = new MutableLiveData<>();

    AddTokenViewModel(
            AddTokenInteract addTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            HomeRouter homeRouter,
            SetupTokensInteract setupTokenInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.addTokenInteract = addTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.homeRouter = homeRouter;
        this.setupTokensInteract = setupTokenInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public void save(String address, String symbol, int decimals, String name, int chainId) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, chainId);
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, wallet.getValue()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSaved, error -> onInterfaceCheckError(error, tokenInfo));
    }

    //fallback in case interface spec check throws an error.
    //If any token data was picked up then default to ERC20 token.
    private void onInterfaceCheckError(Throwable throwable, TokenInfo tokenInfo)
    {
        if ((tokenInfo.name != null && tokenInfo.name.length() > 0)
            || (tokenInfo.symbol != null && tokenInfo.symbol.length() > 0))
        disposable = addTokenInteract.add(tokenInfo, ContractType.ERC20, wallet.getValue())
                .subscribe(this::onSaved, this::onError);
    }

    private void onSaved(Token token)
    {
        assetDefinitionService.getAssetDefinition(token.getAddress());
        tokensService.addToken(token);
        progress.postValue(false);
        result.postValue(true);
    }

    private TokenInfo getTokenInfo(String address, String symbol, int decimals, String name, int chainId)
    {
        return new TokenInfo(address, name, symbol, decimals, true, chainId);
    }

    public void setupTokens(String addr) {
        progress.postValue(true);
        if (defaultNetwork.getValue() == null) {
            findDefaultNetwork();
        }
        else if (wallet.getValue() == null) {
            findWallet();
        }

        disposable = setupTokensInteract
                .update(addr, getNetworkInfo().chainId)
                .subscribe(this::onTokensSetup, this::onError, this::onFetchTokensCompletable);
    }

    private void findDefaultNetwork() {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        findWallet();
    }

    public LiveData<Boolean> result() {
        return result;
    }

    public LiveData<Boolean> update() {
        return update;
    }

    public NetworkInfo getNetworkInfo() { return defaultNetwork.getValue(); }
    public NetworkInfo getNetwork()
    {
        return defaultNetwork.getValue();
    }

    public void showTokens(Context context)
    {
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(w -> homeRouter.open(context, true), this::onError);
    }

    private void findWallet()
    {
        disposable = findDefaultWalletInteract.find()
                .subscribe(wallet::setValue, this::onError);
    }

    private void onTokensSetup(TokenInfo tokenData) {
        tokenInfo.setValue(tokenData);
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        update.postValue(true);
    }

    public LiveData<TokenInfo> tokenInfo() {
        return tokenInfo;
    }

    public void prepare()
    {
        findDefaultNetwork();
    }

    public void showSend(Context ctx, QrUrlResult result)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, wallet.getValue().address);
        intent.putExtra(C.EXTRA_SYMBOL, findDefaultNetworkInteract.getNetworkInfo(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, 18);
        intent.putExtra(C.Key.WALLET, wallet.getValue());
        intent.putExtra(C.EXTRA_TOKEN_ID, tokensService.getToken(result.chainId, wallet.getValue().address));
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }
}
