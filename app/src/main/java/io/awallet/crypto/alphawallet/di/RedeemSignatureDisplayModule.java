package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.MemPoolInteract;
import io.awallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.viewmodel.RedeemSignatureDisplayModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 25/01/2018.
 */

@Module
public class RedeemSignatureDisplayModule {
    @Provides
    RedeemSignatureDisplayModelFactory signatureDisplayModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memPoolInteract,
            AssetDisplayRouter assetDisplayRouter) {
        return new RedeemSignatureDisplayModelFactory(
                findDefaultWalletInteract, signatureGenerateInteract, createTransactionInteract, findDefaultNetworkInteract, fetchTokensInteract, memPoolInteract, assetDisplayRouter);
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
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }

    @Provides
    MemPoolInteract provideMemPoolInteract(TokenRepositoryType tokenRepository) {
        return new MemPoolInteract(tokenRepository);
    }

    @Provides
    AssetDisplayRouter provideAssetDisplayRouter() {
        return new AssetDisplayRouter();
    }
}
