package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.util.BalanceUtils;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static io.stormbird.wallet.C.USD_SYMBOL;
import static io.stormbird.wallet.util.BalanceUtils.weiToEth;

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
        return walletRepository.balanceInWei(wallet)
                .flatMap(ethBalance -> {
                    Map<String, String> balances = new HashMap<>();
                    balances.put(
                            ethereumNetworkRepository.getDefaultNetwork().symbol,
                            weiToEth(ethBalance)
                                    .setScale(4, RoundingMode.HALF_DOWN)
                                .stripTrailingZeros().toPlainString());
                    return Single.just(balances);
                })
//                .flatMap(balances -> ethereumNetworkRepository
//                        .getTicker()
//                        .observeOn(Schedulers.io())
//                        .flatMap(ticker -> {
//                            String ethBallance = balances.get(ethereumNetworkRepository.getDefaultNetwork().symbol);
//                            balances.put(USD_SYMBOL, BalanceUtils.ethToUsd(ticker.price_usd, ethBallance));
//                            return Single.just(balances);
//                        })
//                        .onErrorResumeNext(throwable -> Single.just(balances)))
                .observeOn(AndroidSchedulers.mainThread());
    }


}