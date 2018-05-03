package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.MutableLiveData;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.service.FeeMasterService;

public class CreateTokenViewModel extends BaseViewModel {

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final FeeMasterService feeMasterService;
    private final AddTokenInteract addTokenInteract;

    private final MutableLiveData<Boolean> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> update = new MutableLiveData<>();

    CreateTokenViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                      FindDefaultWalletInteract findDefaultWalletInteract,
                      CreateTransactionInteract createTransactionInteract,
                      FetchTokensInteract fetchTokensInteract,
                      SetupTokensInteract setupTokensInteract,
                      FeeMasterService feeMasterService,
                      AddTokenInteract addTokenInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.feeMasterService = feeMasterService;
        this.addTokenInteract = addTokenInteract;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

}

