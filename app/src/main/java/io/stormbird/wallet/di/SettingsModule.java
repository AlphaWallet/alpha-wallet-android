package io.stormbird.wallet.di;

import io.stormbird.wallet.ui.SettingsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public interface SettingsModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = {SettingsFragmentModule.class})
    SettingsFragment settingsFragment();
}
