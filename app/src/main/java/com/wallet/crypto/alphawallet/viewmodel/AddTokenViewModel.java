package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.TicketInfo;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.AddTokenInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.SetupTokensInteract;
import com.wallet.crypto.alphawallet.router.MyTokensRouter;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;

    private final MutableLiveData<Boolean> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> update = new MutableLiveData<>();

    AddTokenViewModel(
            AddTokenInteract addTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            MyTokensRouter myTokensRouter,
            SetupTokensInteract setupTokenInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.addTokenInteract = addTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.setupTokensInteract = setupTokenInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public void save(String address, String symbol, int decimals, String name, String venue, String date, double db) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, venue, date, db);
        addTokenInteract
                .add(tokenInfo)
                .subscribe(this::onSaved, this::onError);
    }

    //Here we decide what type of token we are dealing with
    private TokenInfo getTokenInfo(String address, String symbol, int decimals, String name, String venue, String date, double db)
    {
        TokenInfo tokenInfo = new TokenInfo(address, name, symbol, decimals, true);

        if (venue != null && venue.length() > 0)
        {
            tokenInfo = new TicketInfo(tokenInfo, venue, date, db);
        }

        return tokenInfo;
    }

    private void onSaved() {
        progress.postValue(false);
        result.postValue(true);
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
        findDefaultWalletInteract
                .find()
                .subscribe(w -> myTokensRouter.open(context, w), this::onError);
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
