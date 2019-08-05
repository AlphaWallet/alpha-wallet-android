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

    public static Sign.SignatureData sigFromByteArray(byte[] sig)
    {
        byte subv = (byte)(sig[64] + 27);

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        BigInteger r = new BigInteger(1, subrRev);
        BigInteger s = new BigInteger(1, subsRev);

        return new Sign.SignatureData(subv, subrRev, subsRev);
    }
}
