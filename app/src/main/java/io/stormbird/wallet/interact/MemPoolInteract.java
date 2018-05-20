package io.stormbird.wallet.interact;

import org.web3j.protocol.core.methods.response.Transaction;

import io.stormbird.wallet.entity.SubscribeWrapper;
import io.stormbird.wallet.entity.TransferFromEventResponse;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.reactivex.disposables.Disposable;
import rx.functions.Action1;

/**
 * Created by James on 1/02/2018.
 */

public class MemPoolInteract
{
    private final TokenRepositoryType tokenRepository;

    public MemPoolInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    //create an observable
    public rx.Subscription poolListener(SubscribeWrapper processTx) {
        return tokenRepository.memPoolListener(processTx);
    }

    //create an observable
    public rx.Observable<TransferFromEventResponse> burnListener(String contractAddress) {
        return tokenRepository.burnListenerObservable(contractAddress);
    }
}
