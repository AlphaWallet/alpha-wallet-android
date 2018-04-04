package io.awallet.crypto.alphawallet.interact;

import io.awallet.crypto.alphawallet.entity.MessagePair;
import io.awallet.crypto.alphawallet.entity.SignaturePair;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;

import java.math.BigInteger;
import java.util.List;

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
    public Single<MessagePair> getMessage(List<Integer> indexList) {
        return Single.fromCallable(() -> {
            String selectionStr = SignaturePair.generateSelection(indexList);
            long currentTime = System.currentTimeMillis();
            long minsT = currentTime / (30 * 1000);
            int minsTime = (int) minsT;
            String plainMessage = selectionStr + "," + String.valueOf(minsTime);
            return new MessagePair(selectionStr, plainMessage);
        });
    }
}
