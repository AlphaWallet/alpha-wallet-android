package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.interact.ImportWalletInteract;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;

public class ImportWalletViewModelFactory implements ViewModelProvider.Factory {

    private final ImportWalletInteract importWalletInteract;
    private final KeyService keyService;
    private final GasService gasService;
    private final AnalyticsServiceType analyticsService;

    public ImportWalletViewModelFactory(ImportWalletInteract importWalletInteract, KeyService keyService,
                                        GasService gasService, AnalyticsServiceType analyticsService) {
        this.importWalletInteract = importWalletInteract;
        this.keyService = keyService;
        this.gasService = gasService;
        this.analyticsService = analyticsService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportWalletViewModel(importWalletInteract, keyService, gasService, analyticsService);
    }
}
