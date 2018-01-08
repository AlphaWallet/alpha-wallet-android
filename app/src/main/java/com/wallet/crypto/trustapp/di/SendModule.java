package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.FetchGasSettingsInteract;
import com.wallet.crypto.trustapp.repository.GasSettingsRepositoryType;
import com.wallet.crypto.trustapp.router.ConfirmationRouter;
import com.wallet.crypto.trustapp.viewmodel.SendViewModelFactory;

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
