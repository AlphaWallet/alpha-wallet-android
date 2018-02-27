package com.wallet.crypto.alphawallet.interact;

import com.wallet.crypto.alphawallet.entity.MessagePair;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;

/**
 * Created by James on 25/01/2018.
 */

public class SignatureGenerateInteract {

    private final WalletRepositoryType walletRepository;

    public SignatureGenerateInteract(WalletRepositoryType walletRepository) {
        this.walletRepository = walletRepository;
    }

    //TODO: Sign message here not in the additional field
    public Single<MessagePair> getMessage(BigInteger bitField) {
        return Single.fromCallable(() -> {
            if (bitField != null) {
                //convert biginteger to hex
                String hexField = bitField.toString(16);
                long currentTime = System.currentTimeMillis();
                long minsT = currentTime / (30 * 1000);
                int minsTime = (int) minsT;
                String plainMessage = hexField + "," + String.valueOf(minsTime);
                return new MessagePair(hexField, plainMessage);
            }
            else
            {
                return null;
            }
        });
    }
}
