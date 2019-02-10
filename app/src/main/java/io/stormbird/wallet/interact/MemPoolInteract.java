package io.stormbird.wallet.interact;

import io.reactivex.Observable;
import io.stormbird.wallet.entity.SubscribeWrapper;
import io.stormbird.wallet.entity.TransferFromEventResponse;
import io.stormbird.wallet.repository.TokenRepositoryType;
import org.reactivestreams.Subscription;

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
    public Observable<TransferFromEventResponse> burnListener(String contractAddress) {
        return tokenRepository.burnListenerObservable(contractAddress);
    }
}
