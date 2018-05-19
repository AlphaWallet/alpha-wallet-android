package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.ChangeTokenEnableInteract;
import io.stormbird.wallet.interact.FetchAllTokenInfoInteract;

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;

public class TokenChangeCollectionViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();

    private final FetchAllTokenInfoInteract fetchAllTokenInfoInteract;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;

    TokenChangeCollectionViewModel(
            FetchAllTokenInfoInteract fetchAllTokenInfoInteract,
            ChangeTokenEnableInteract changeTokenEnableInteract) {
        this.fetchAllTokenInfoInteract = fetchAllTokenInfoInteract;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
    }

    public void prepare() {
        progress.postValue(true);
        fetchTokens();
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }

    public void fetchTokens() {
        progress.postValue(true);
        disposable = fetchAllTokenInfoInteract
                .fetch(wallet.getValue())
                .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
    }

    private void onTokens(Token[] tokens) {
        this.tokens.setValue(tokens);
        if (tokens != null && tokens.length > 0) {
            progress.postValue(true);
        }
    }

    public void setEnabled(Token token) {
        changeTokenEnableInteract
                .setEnable(wallet.getValue(), token)
                .subscribe(() -> {}, this::onError);
    }
}
