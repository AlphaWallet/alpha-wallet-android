package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.MessagePair;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.TransactionRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepository;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import java.math.BigInteger;
import java.security.SecureRandom;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
            //convert biginteger to hex
            String hexField = bitField.toString(16);
            long currentTime = System.currentTimeMillis();
            long minsT = currentTime / (30 * 1000);
            int minsTime = (int) minsT;
            String plainMessage = hexField + "," + String.valueOf(minsTime);
            return new MessagePair(hexField, plainMessage);
        });
    }
}
