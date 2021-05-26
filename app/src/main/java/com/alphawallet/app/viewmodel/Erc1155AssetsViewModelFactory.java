package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import io.reactivex.annotations.NonNull;

public class Erc1155AssetsViewModelFactory implements ViewModelProvider.Factory {

    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public Erc1155AssetsViewModelFactory(FetchTransactionsInteract fetchTransactionsInteract,
                                         AssetDefinitionService assetDefinitionService,
                                         TokensService tokensService) {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new Erc1155AssetsViewModel(fetchTransactionsInteract, assetDefinitionService, tokensService);
    }
}
