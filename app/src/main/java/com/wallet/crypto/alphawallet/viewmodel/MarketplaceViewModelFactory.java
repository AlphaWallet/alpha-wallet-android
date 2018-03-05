package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.router.MarketBrowseRouter;

public class MarketplaceViewModelFactory implements ViewModelProvider.Factory {
    private final MarketBrowseRouter marketBrowseRouter;

    public MarketplaceViewModelFactory(
            MarketBrowseRouter marketBrowseRouter) {
        this.marketBrowseRouter = marketBrowseRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MarketplaceViewModel(
                marketBrowseRouter
        );
    }
}
