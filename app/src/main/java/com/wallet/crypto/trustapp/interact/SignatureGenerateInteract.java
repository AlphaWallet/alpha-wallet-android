package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.TransactionRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepository;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

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

    public Single<String> getMessage(Wallet wallet) {
        return Single.fromCallable(() -> {
            long currentTime = System.currentTimeMillis();
            long minsT = currentTime / (60 * 1000);
            int minsTime = (int) minsT;
            String plainMessage = new String(String.valueOf(minsTime));
            //now sign this message with the wallet address

            return plainMessage;
        });

//        return walletRepository
//                .fetchTransaction(wallet)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());
    }
}
