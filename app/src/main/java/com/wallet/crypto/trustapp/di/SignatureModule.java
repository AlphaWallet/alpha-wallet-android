package com.wallet.crypto.trustapp.di;



import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;
import com.wallet.crypto.trustapp.viewmodel.SignatureDisplayModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 25/01/2018.
 */

@Module
public class SignatureModule {
    @Provides
    SignatureDisplayModelFactory signatureDisplayModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        return new SignatureDisplayModelFactory(
                findDefaultWalletInteract, signatureGenerateInteract, findDefaultNetworkInteract);
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
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }
}
