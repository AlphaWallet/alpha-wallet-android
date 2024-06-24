package com.langitwallet.app.interact;

import io.reactivex.Observable;

import com.langitwallet.app.entity.TransferFromEventResponse;
import com.langitwallet.app.repository.TokenRepositoryType;

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
