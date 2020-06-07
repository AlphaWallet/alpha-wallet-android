package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.TokensService;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModelFactory implements ViewModelProvider.Factory
{
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private final OpenseaService openseaService;
    private final FetchTransactionsInteract fetchTransactionsInteract;

    public TokenFunctionViewModelFactory(
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract,
            OpenseaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract) {
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.keyService = keyService;
        this.genericWalletInteract = genericWalletInteract;
        this.openseaService = openseaService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TokenFunctionViewModel(assetDefinitionService, createTransactionInteract, gasService, tokensService, ethereumNetworkRepository, keyService, genericWalletInteract, openseaService, fetchTransactionsInteract);
    }
}
