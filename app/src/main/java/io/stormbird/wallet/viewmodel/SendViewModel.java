package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.ENSInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.ImportTokenActivity;
import io.stormbird.wallet.ui.SendActivity;

import java.math.BigInteger;

import static io.stormbird.wallet.C.IMPORT_STRING;

public class SendViewModel extends BaseViewModel {
    private final MutableLiveData<String> ensResolve = new MutableLiveData<>();
    private final MutableLiveData<String> ensFail = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final ENSInteract ensInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;

    public SendViewModel(ConfirmationRouter confirmationRouter,
                         MyAddressRouter myAddressRouter,
                         ENSInteract ensInteract,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         FetchGasSettingsInteract fetchGasSettingsInteract) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.ensInteract = ensInteract;
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    }

    public LiveData<String> ensResolve() { return ensResolve; }
    public LiveData<String> ensFail() { return ensFail; }

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
        intent.putExtra(IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public void startGasPriceChecker(int chainId)
    {
        fetchGasSettingsInteract.startGasSettingsFetch(chainId);
    }

    public void stopGasPriceChecker()
    {
        fetchGasSettingsInteract.stopGasSettingsFetch();
    }
}
