package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.ui.SettingsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public interface SettingsModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = {SettingsFragmentModule.class})
    SettingsFragment settingsFragment();
}
