package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import io.awallet.crypto.alphawallet.repository.GasSettingsRepositoryType;
import io.awallet.crypto.alphawallet.router.ConfirmationRouter;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModelFactory;

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
