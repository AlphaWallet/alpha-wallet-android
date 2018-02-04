package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import com.wallet.crypto.alphawallet.interact.UseTokenInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.MyTokensRouter;
import com.wallet.crypto.alphawallet.router.SignatureDisplayRouter;
import com.wallet.crypto.alphawallet.router.TicketTransferRouter;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 22/01/2018.
 */

@Module
public class UseTokenModule {
    @Provides
    UseTokenViewModelFactory useTokenViewModelFactory(
            UseTokenInteract useTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            MyTokensRouter myTokensRouter,
            TicketTransferRouter ticketTransferRouter,
            SignatureDisplayRouter signatureDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        return new UseTokenViewModelFactory(
                useTokenInteract, findDefaultWalletInteract, signatureGenerateInteract, myTokensRouter, ticketTransferRouter, signatureDisplayRouter, findDefaultNetworkInteract);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    UseTokenInteract provideUseTokenInteract(
            TokenRepositoryType tokenRepository,
            WalletRepositoryType walletRepository) {
        return new UseTokenInteract(walletRepository, tokenRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    MyTokensRouter provideMyTokensRouter() {
        return new MyTokensRouter();
    }

    @Provides
    TicketTransferRouter tiketTransferRouter() {
        return new TicketTransferRouter();
    }

    @Provides
    SignatureDisplayRouter provideSignatureDisplayRouter() {
        return new SignatureDisplayRouter();
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }
}
