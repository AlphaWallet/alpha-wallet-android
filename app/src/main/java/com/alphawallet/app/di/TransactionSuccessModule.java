package com.alphawallet.app.di;

import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.viewmodel.TransactionSuccessViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TransactionSuccessModule {

    @Provides
    TransactionSuccessViewModelFactory provideTransactionSuccessViewModelFactory(PreferenceRepositoryType preferenceRepository) {
        return new TransactionSuccessViewModelFactory(
                preferenceRepository);
    }
}
