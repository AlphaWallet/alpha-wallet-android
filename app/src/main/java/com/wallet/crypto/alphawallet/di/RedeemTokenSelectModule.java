package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.RedeemTokenRouter;
import com.wallet.crypto.alphawallet.router.RedeemTokenSelectRouter;
import com.wallet.crypto.alphawallet.router.SellDetailRouter;
import com.wallet.crypto.alphawallet.viewmodel.RedeemTokenSelectViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 27/02/2018.
 */

@Module
public class RedeemTokenSelectModule
{
    @Provides
    RedeemTokenSelectViewModelFactory redeemTokenSelectViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            RedeemTokenRouter redeemTokenRouter) {

        return new RedeemTokenSelectViewModelFactory(
                findDefaultWalletInteract, findDefaultNetworkInteract, redeemTokenRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    RedeemTokenRouter provideRedeemTokenRouter() {
        return new RedeemTokenRouter();
    }
}
