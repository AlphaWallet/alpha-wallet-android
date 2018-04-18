package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.router.ExternalBrowserRouter;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.MarketBrowseRouter;
import io.awallet.crypto.alphawallet.router.MarketplaceRouter;
import io.awallet.crypto.alphawallet.router.MyTokensRouter;
import io.awallet.crypto.alphawallet.router.NewSettingsRouter;
import io.awallet.crypto.alphawallet.router.SettingsRouter;
import io.awallet.crypto.alphawallet.router.TransactionDetailRouter;
import io.awallet.crypto.alphawallet.router.WalletRouter;

public class TransactionsViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final HomeRouter homeRouter;

    public TransactionsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            ExternalBrowserRouter externalBrowserRouter,
            HomeRouter homeRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransactionsViewModel(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                fetchTransactionsInteract,
                fetchTokensInteract,
                setupTokensInteract,
                addTokenInteract,
                transactionDetailRouter,
                externalBrowserRouter,
                homeRouter);
    }
}
