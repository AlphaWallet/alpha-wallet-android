package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.SendActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();
    private final MutableLiveData<Integer> switchNetwork = new MutableLiveData<>();
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();
    private final MutableLiveData<Token> tokentype = new MutableLiveData<>();
    private final MutableLiveData<Token> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noContract = new MutableLiveData<>();
    private final MutableLiveData<Integer> scanCount = new MutableLiveData<>();

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final PreferenceRepositoryType sharedPreference;

    private boolean foundNetwork;
    private int networkCount;
    private int primaryChainId = 1;

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }
    public MutableLiveData<Token> tokenFinalised() { return finalisedToken; }
    public MutableLiveData<Token> tokenType() { return tokentype; }
    public MutableLiveData<Boolean> noContract() { return noContract; }
    public LiveData<Token> result() { return result; }
    public LiveData<Integer> switchNetwork() { return switchNetwork; }
    public LiveData<TokenInfo> tokenInfo() {
        return tokenInfo;
    }
    public LiveData<Integer> chainScanCount() { return scanCount; }

    @Nullable
    Disposable scanNetworksDisposable;

    private final List<Disposable> scanThreads = new ArrayList<>();

    AddTokenViewModel(
            GenericWalletInteract genericWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            PreferenceRepositoryType sharedPreference) {
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.sharedPreference = sharedPreference;
    }

    public void save(int chainId, String address, String name, String symbol, int decimals, ContractType contractType)
    {
        //update token details as entered
        TokenInfo info = new TokenInfo(address, name, symbol, decimals, true, chainId);
        disposable = tokensService.addToken(info, wallet.getValue().address)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::setVisibilityChanged, this::onError);
    }

    private void setVisibilityChanged(Token t)
    {
        tokensService.lockTokenVisibility(t)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> result.postValue(t))
                .isDisposed();
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (scanNetworksDisposable != null && !scanNetworksDisposable.isDisposed()) scanNetworksDisposable.dispose();
    }

    public void setPrimaryChain(int chainId)
    {
        primaryChainId = chainId;
    }

    public int getSelectedChain()
    {
        return primaryChainId;
    }

    private void setupToken(int chainId, String addr, ContractType type) {
        disposable = tokensService
                .update(addr, chainId)
                .subscribe(info -> onTokensSetup(info, type), error -> checkType(error, chainId, addr, type));
    }

    private void checkType(Throwable throwable, int chainId, String address, ContractType type)
    {
        if (type == ContractType.ERC1155)
        {
            onTokensSetup(new TokenInfo(address, "Holding Contract", "", 0, true, chainId), type);
        }
        else
        {
            onError(throwable);
        }
    }

    public void fetchToken(int chainId, String addr)
    {
        tokensService.update(addr, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotTokenUpdate, this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo)
    {
        disposable = tokensService.addToken(tokenInfo, wallet.getValue().address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::resumeSend, this::onError);
    }

    private void resumeSend(Token token)
    {
        finalisedToken.postValue(token);
    }

    public NetworkInfo getNetworkInfo(int chainId) { return ethereumNetworkRepository.getNetworkByChain(chainId); }

    private void findWallet()
    {
        disposable = genericWalletInteract.find()
                .subscribe(wallet::setValue, this::onError);
    }

    private void onTokensSetup(TokenInfo info, ContractType type) {
        tokenInfo.postValue(info);
        disposable = tokensService.addToken(info, wallet.getValue().address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokentype::postValue, error -> tokenTypeError(error, info));
    }

    private void tokenTypeError(Throwable throwable, TokenInfo data)
    {
        Token badToken = new Token(data, BigDecimal.ZERO, 0, "", ContractType.NOT_SET);
        tokentype.postValue(badToken);
    }

    public void prepare()
    {
        findWallet();
    }

    public void showSend(Context ctx, QRResult result, Token token)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        boolean sendingTokens = (result.getFunction() != null && result.getFunction().length() > 0);
        String address = wallet.getValue().address;
        int decimals = 18;

        if (sendingTokens)
        {
            address = result.getAddress();
            decimals = token.tokenInfo.decimals;
        }

        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_NETWORKID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet.getValue());
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    private List<Integer> getNetworkIds()
    {
        List<Integer> networkIds = new ArrayList<>();
        networkIds.add(primaryChainId); //test selected chain first
        for (int chainId : tokensService.getNetworkFilters())
        {
            if (!networkIds.contains(chainId)) networkIds.add(chainId);
        }

        //Now scan unselected networks
        for (NetworkInfo networkInfo : ethereumNetworkRepository.getAvailableNetworkList())
        {
            if (!networkIds.contains(networkInfo.chainId)) networkIds.add(networkInfo.chainId);
        }
        return networkIds;
    }

    public void testNetworks(String address, NetworkInfo networkInfo)
    {
        foundNetwork = false;
        networkCount = ethereumNetworkRepository.getAvailableNetworkList().length;
        scanCount.postValue(networkCount);
        //String address, String name, String symbol, int decimals, boolean isEnabled, int chainId
        TokenInfo tokenInfo = new TokenInfo(address, "", "", 0, true, networkInfo.chainId);
        //first test the network selected, then do all the
        //try to determine what kind of contract this is. Note if we get invalid response there's no contract there
        scanNetworksDisposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(type -> onContract(tokenInfo, type), this::onError);
    }

    private void onContract(@NonNull TokenInfo info, ContractType contractType)
    {
        //did it detect anything?
        if (contractType != ContractType.OTHER)
        {
            //found the contract
            foundNetwork = true;
            switchNetwork.postValue(info.chainId);
            setupToken(info.chainId, info.address, contractType);
        }
        else
        {
            //try the other networks
            ethereumNetworkRepository.getAllActiveNetworks();
            networkCount--;
            scanCount.postValue(networkCount);

            for (int networkId : getNetworkIds())
            {
                if (foundNetwork) break;
                if (networkId == info.chainId) continue;
                TokenInfo tokenInfo = new TokenInfo(info.address, "", "", 0, true, networkId);
                Disposable d = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(type -> testNetworkResult(tokenInfo, type), this::onTestError);

                scanThreads.add(d);
            }
        }
    }

    private void testNetworkResult(TokenInfo info, ContractType type)
    {
        if (!foundNetwork && type != ContractType.OTHER)
        {
            foundNetwork = true;
            switchNetwork.postValue(info.chainId);
            setupToken(info.chainId, info.address, type);
            stopScan();
        }
        else
        {
            checkNetworkCount();
        }
    }

    public void stopScan()
    {
        for (Disposable d : scanThreads)
        {
            if (!d.isDisposed()) d.dispose();
        }
        scanThreads.clear();
    }

    private void onTestError(Throwable throwable)
    {
        checkNetworkCount();
        onError(throwable);
    }

    private void checkNetworkCount()
    {
        networkCount--;
        scanCount.postValue(networkCount);
        if (networkCount == 0 && !foundNetwork)
        {
            noContract.postValue(true);
        }
    }

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public Token getToken(int chainId, String address)
    {
        return tokensService.getToken(chainId, address);
    }

    public Token getChainToken(int chainId)
    {
        return tokensService.getServiceToken(chainId);
    }

    public boolean shouldHideZeroBalanceTokens()
    {
        return sharedPreference.shouldShowZeroBalanceTokens();
    }

    public void hideZeroBalanceTokens()
    {
        sharedPreference.setShowZeroBalanceTokens(false);
    }
}
