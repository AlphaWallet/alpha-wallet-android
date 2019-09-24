package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.SellTicketRouter;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModelFactory implements ViewModelProvider.Factory
{
    private final AssetDefinitionService assetDefinitionService;
    private final SellTicketRouter sellTicketRouter;
    private final TransferTicketDetailRouter transferTicketRouter;
    private final CreateTransactionInteract createTransactionInteract;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;

    public TokenFunctionViewModelFactory(
            AssetDefinitionService assetDefinitionService,
            SellTicketRouter sellTicketRouter,
            TransferTicketDetailRouter transferTicketRouter,
            CreateTransactionInteract createTransactionInteract,
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract) {
        this.assetDefinitionService = assetDefinitionService;
        this.sellTicketRouter = sellTicketRouter;
        this.transferTicketRouter = transferTicketRouter;
        this.createTransactionInteract = createTransactionInteract;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.keyService = keyService;
        this.genericWalletInteract = genericWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TokenFunctionViewModel(assetDefinitionService, sellTicketRouter, transferTicketRouter, createTransactionInteract, gasService, tokensService, ethereumNetworkRepository, keyService, genericWalletInteract);
    }
}
