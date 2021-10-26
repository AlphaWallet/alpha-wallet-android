package com.alphawallet.app.viewmodel;



import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.ethereum.NetworkInfo;

public class CustomNetworkViewModel extends BaseViewModel
{
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    CustomNetworkViewModel(
            EthereumNetworkRepositoryType ethereumNetworkRepository)
    {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public void addNetwork(String name, String rpcUrl, long chainId, String symbol, String blockExplorerUrl, String explorerApiUrl, boolean isTestnet, Long oldChainId) {
        this.ethereumNetworkRepository.addCustomRPCNetwork(name, rpcUrl, chainId, symbol, blockExplorerUrl, explorerApiUrl, isTestnet, oldChainId);
    }

    public EthereumNetworkRepositoryType.NetworkInfoExt getNetworkInfo(long chainId) {
        return this.ethereumNetworkRepository.getNetworkInfoExt(chainId);
    }
}
