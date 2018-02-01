package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.MessagePair;
import com.wallet.crypto.trustapp.entity.SubscribeWrapper;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;
import rx.Subscription;
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
    public void poolListener(SubscribeWrapper wrapper) {
        tokenRepository.memPoolListener(wrapper);
    }
}
