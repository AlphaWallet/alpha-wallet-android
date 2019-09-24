package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.SellDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by James on 16/02/2018.
 */

public class SellTicketModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final SellDetailRouter sellDetailRouter;
    private final AssetDefinitionService assetDefinitionService;

    public SellTicketModelFactory(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SellDetailRouter sellDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.sellDetailRouter = sellDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SellTicketModel(fetchTokensInteract, genericWalletInteract, findDefaultNetworkInteract, sellDetailRouter, assetDefinitionService);
    }
}
