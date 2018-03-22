package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.ui.SettingsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public interface SettingsModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = {SettingsFragmentModule.class})
    SettingsFragment settingsFragment();
}
