package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import com.wallet.crypto.alphawallet.repository.GasSettingsRepositoryType;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;
import com.wallet.crypto.alphawallet.viewmodel.SendViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class SendModule {
    @Provides
    SendViewModelFactory provideSendViewModelFactory(ConfirmationRouter confirmationRouter, FetchGasSettingsInteract fetchGasSettingsInteract) {
        return new SendViewModelFactory(confirmationRouter, fetchGasSettingsInteract);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    FetchGasSettingsInteract provideFetchGasSettingsInteract(GasSettingsRepositoryType gasSettingsRepository) {
        return new FetchGasSettingsInteract(gasSettingsRepository);
    }
}
