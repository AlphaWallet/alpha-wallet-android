package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;
import com.wallet.crypto.trustapp.util.BallanceUtils;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.wallet.crypto.trustapp.C.USD_SYMBOL;
import static com.wallet.crypto.trustapp.util.BallanceUtils.weiToEth;

public class GetDefaultWalletBalance {

    private final WalletRepositoryType walletRepository;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public GetDefaultWalletBalance(
            WalletRepositoryType walletRepository,
            EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.walletRepository = walletRepository;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public Single<Map<String, String>> get(Wallet wallet) {
        return walletRepository.ballanceInWei(wallet)
                .flatMap(ethBallance -> {
                    Map<String, String> ballances = new HashMap<>();
                    ballances.put(ethereumNetworkRepository.getDefaultNetwork().symbol, weiToEth(ethBallance, 5));
                    return Single.just(ballances);
                })
                .flatMap(ballances -> ethereumNetworkRepository
                        .getTicker()
                        .observeOn(Schedulers.io())
                        .flatMap(ticker -> {
                            String ethBallance = ballances.get(ethereumNetworkRepository.getDefaultNetwork().symbol);
                            ballances.put(USD_SYMBOL, BallanceUtils.ethToUsd(ticker.priceUsd, ethBallance));
                            return Single.just(ballances);
                        })
                        .onErrorResumeNext(throwable -> Single.just(ballances)))
                .observeOn(AndroidSchedulers.mainThread());
    }


}