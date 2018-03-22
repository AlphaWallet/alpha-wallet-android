package io.awallet.crypto.alphawallet.interact;

import io.awallet.crypto.alphawallet.entity.SubscribeWrapper;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;

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
    public void poolListener(SubscribeWrapper wrapper) {
        tokenRepository.memPoolListener(wrapper);
    }
}
