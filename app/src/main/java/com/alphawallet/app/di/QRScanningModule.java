package com.alphawallet.app.di;

import com.alphawallet.app.viewmodel.QRScanningViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class QRScanningModule {
    @Provides
    QRScanningViewModelFactory provideQRScanningViewModelFactory()
    {
        return new QRScanningViewModelFactory();
    }
}
