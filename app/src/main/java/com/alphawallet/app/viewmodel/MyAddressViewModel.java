package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.TokensService;

public class MyAddressViewModel extends BaseViewModel {
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokenService;

    MyAddressViewModel(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenService = tokensService;
    }

    public TokensService getTokenService() {
        return tokenService;
    }

    public EthereumNetworkRepositoryType getEthereumNetworkRepository() {
        return ethereumNetworkRepository;
    }

    public NetworkInfo getNetworkByChain(int chainId) {
        return ethereumNetworkRepository.getNetworkByChain(chainId);
    }
}
