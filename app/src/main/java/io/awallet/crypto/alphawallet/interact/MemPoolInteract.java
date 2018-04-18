package io.awallet.crypto.alphawallet.interact;

import org.web3j.protocol.core.methods.response.Transaction;

import io.awallet.crypto.alphawallet.entity.SubscribeWrapper;
import io.awallet.crypto.alphawallet.entity.TransferFromEventResponse;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
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
