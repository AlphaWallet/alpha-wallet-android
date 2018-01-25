package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.AddTokenInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SetupTokensInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;
import com.wallet.crypto.trustapp.router.MyTokensRouter;
import com.wallet.crypto.trustapp.router.SignatureDisplayRouter;
import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModelFactory;
import com.wallet.crypto.trustapp.viewmodel.UseTokenViewModelFactory;

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
            SignatureDisplayRouter signatureDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        return new UseTokenViewModelFactory(
                useTokenInteract, findDefaultWalletInteract, signatureGenerateInteract, myTokensRouter, signatureDisplayRouter, findDefaultNetworkInteract);
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
    SignatureDisplayRouter provideSignatureDisplayRouter() {
        return new SignatureDisplayRouter();
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }
}
