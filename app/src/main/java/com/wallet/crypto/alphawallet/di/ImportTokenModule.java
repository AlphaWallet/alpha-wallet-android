package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.service.ImportTokenService;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 9/03/2018.
 */

@Module
public class ImportTokenModule {

    @Provides
    ImportTokenViewModelFactory importTokenViewModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            ImportTokenService importTokenService) {
        return new ImportTokenViewModelFactory(
                findDefaultWalletInteract, importTokenService);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }
}
