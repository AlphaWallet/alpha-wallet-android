package com.alphawallet.app.di;

import com.alphawallet.app.di.mock.KeyProviderMockImpl;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderJNIImpl;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

@Module
@TestInstallIn(
        components = SingletonComponent.class,
        replaces = ProviderModule.class
)
public class FakeRepositoryModule
{
    @Singleton
    @Provides
    KeyProvider provideKeyProvider() {
        return new KeyProviderMockImpl();
    }}
