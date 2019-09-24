package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.router.TransactionDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

public class TransactionsViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public TransactionsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransactionsViewModel(
                findDefaultNetworkInteract,
                genericWalletInteract,
                fetchTransactionsInteract,
                setupTokensInteract,
                addTokenInteract,
                transactionDetailRouter,
                assetDefinitionService,
                tokensService);
    }
}
