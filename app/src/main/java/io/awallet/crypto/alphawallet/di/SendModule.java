package io.awallet.crypto.alphawallet.di;

import dagger.Module;
import dagger.Provides;
import io.awallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import io.awallet.crypto.alphawallet.repository.GasSettingsRepositoryType;
import io.awallet.crypto.alphawallet.router.ConfirmationRouter;
import io.awallet.crypto.alphawallet.router.MyAddressRouter;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModelFactory;

@Module
class SendModule {
    @Provides
    SendViewModelFactory provideSendViewModelFactory(ConfirmationRouter confirmationRouter,
                                                     FetchGasSettingsInteract fetchGasSettingsInteract,
                                                     MyAddressRouter myAddressRouter) {
        return new SendViewModelFactory(confirmationRouter,
                fetchGasSettingsInteract,
                myAddressRouter);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    FetchGasSettingsInteract provideFetchGasSettingsInteract(GasSettingsRepositoryType gasSettingsRepository) {
        return new FetchGasSettingsInteract(gasSettingsRepository);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }
}
