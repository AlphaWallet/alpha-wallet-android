package io.stormbird.wallet.service;


import io.reactivex.Single;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import okhttp3.OkHttpClient;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenService {

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private MarketQueueService.ApiMarketQueue marketQueueConnector;

    public ImportTokenService(OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
    }

//    public Single<byte[]> sign(Wallet wallet, byte[] importMessage, int chainId)
//    {
//        return transactionRepository.getSignature(wallet, importMessage, chainId);
//                //.map(sig -> new SignaturePair(messagePair.selection, sig, messagePair.message));
//    }

    //sign the ticket data
    public Single<byte[]> sign(Wallet wallet, byte[] data, int chainId) {
        return transactionRepository.getSignature(wallet, data, chainId);
    }
}
