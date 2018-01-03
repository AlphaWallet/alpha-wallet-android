package com.wallet.crypto.trustapp.di;


import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class GasSettingsModule {

    @Provides
    public GasSettingsViewModelFactory provideGasSettingsViewModelFactory() {
        return new GasSettingsViewModelFactory();
    }
}
