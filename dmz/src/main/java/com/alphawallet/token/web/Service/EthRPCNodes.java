package com.alphawallet.token.web.Service;

import com.alphawallet.ethereum.EthereumNetworkBase;

import static com.alphawallet.token.web.AppSiteController.getInfuraKey;

public class EthRPCNodes
{
    private static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/" + getInfuraKey();
    private static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    private static final String XDAI_RPC_URL = EthereumNetworkBase.XDAI_RPC_URL;
    private static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/" + getInfuraKey();


    public static String getNodeURLByNetworkId(long networkId) {
        switch ((int)networkId) {
            case (int)EthereumNetworkBase.MAINNET_ID:
                return MAINNET_RPC_URL;
            case (int)EthereumNetworkBase.CLASSIC_ID:
                return CLASSIC_RPC_URL;
            case (int)EthereumNetworkBase.GNOSIS_ID:
                return XDAI_RPC_URL;
            case (int)EthereumNetworkBase.GOERLI_ID:
                return GOERLI_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }
}
