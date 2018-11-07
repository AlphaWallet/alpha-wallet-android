package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetupTokensInteract;
import io.stormbird.wallet.router.HomeRouter;

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

    private final MutableLiveData<Boolean> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> update = new MutableLiveData<>();

    AddTokenViewModel(
            AddTokenInteract addTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            HomeRouter homeRouter,
            SetupTokensInteract setupTokenInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.addTokenInteract = addTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.homeRouter = homeRouter;
        this.setupTokensInteract = setupTokenInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public void save(String address, String symbol, int decimals, String name, boolean isStormBird) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, isStormBird);
        disposable = addTokenInteract
                .add(tokenInfo, 0)
                .subscribe(this::onSaved, this::onError);
    }

    private void onSaved(Token token)
    {
        progress.postValue(false);
        result.postValue(true);
    }

    private TokenInfo getTokenInfo(String address, String symbol, int decimals, String name, boolean isStormBird)
    {
        TokenInfo tokenInfo = new TokenInfo(address, name, symbol, decimals, true, isStormBird);
        return tokenInfo;
    }

    public void setupTokens(String addr) {
        progress.postValue(true);
        if (defaultNetwork.getValue() == null) {
            findDefaultNetwork();
        }
        disposable = setupTokensInteract
                .update(addr)
                .subscribe(this::onTokensSetup, this::onError, this::onFetchTokensCompletable);
    }

    private void findDefaultNetwork() {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
    }

    public LiveData<Boolean> result() {
        return result;
    }

    public LiveData<Boolean> update() {
        return update;
    }

    public void showTokens(Context context) {
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(w -> homeRouter.open(context, true), this::onError);
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
}
