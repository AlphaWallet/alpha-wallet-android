package io.stormbird.wallet.interact;

import android.util.Log;

import io.stormbird.wallet.entity.MessagePair;
import io.stormbird.wallet.entity.SignaturePair;
import io.stormbird.wallet.repository.WalletRepositoryType;

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
    public Single<MessagePair> getMessage(List<Integer> indexList, String contract) {
        return Single.fromCallable(() -> {
            String selectionStr = SignaturePair.generateSelection(indexList);
            long currentTime = System.currentTimeMillis();
            long minsT = currentTime / (30 * 1000);
            int minsTime = (int) minsT;
            String plainMessage = selectionStr + "," + String.valueOf(minsTime) + "," + contract.toLowerCase();  //This is the plain text message that gets signed
            Log.d("SIG", plainMessage);
            return new MessagePair(selectionStr, plainMessage);
        });
    }
}
