package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.repository.GasSettingsRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.viewmodel.SendViewModelFactory;

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
