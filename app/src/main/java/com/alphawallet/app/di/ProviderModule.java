package com.alphawallet.app.di;

import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderJNIImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class ProviderModule
{
    @Singleton
    @Provides
    KeyProvider provideKeyProvider() {
        return new KeyProviderJNIImpl();
    }
}
