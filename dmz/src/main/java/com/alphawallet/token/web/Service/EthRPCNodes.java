package com.alphawallet.token.web.Service;

import com.alphawallet.ethereum.EthereumNetworkBase;

import static com.alphawallet.token.web.AppSiteController.getInfuraKey;

public class EthRPCNodes
{
    private static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/" + getInfuraKey();
    private static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    private static final String XDAI_RPC_URL = "https://dai.poa.network";
    private static final String POA_RPC_URL = "https://core.poa.network/";
    private static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/" + getInfuraKey();
    private static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/" + getInfuraKey();
    private static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/" + getInfuraKey();
    private static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    private static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/" + getInfuraKey();
    private static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    private static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";


    public static String getNodeURLByNetworkId(int networkId) {
        switch (networkId) {
            case EthereumNetworkBase.MAINNET_ID:
                return MAINNET_RPC_URL;
            case EthereumNetworkBase.KOVAN_ID:
                return KOVAN_RPC_URL;
            case EthereumNetworkBase.ROPSTEN_ID:
                return ROPSTEN_RPC_URL;
            case EthereumNetworkBase.RINKEBY_ID:
                return RINKEBY_RPC_URL;
            case EthereumNetworkBase.POA_ID:
                return POA_RPC_URL;
            case EthereumNetworkBase.SOKOL_ID:
                return SOKOL_RPC_URL;
            case EthereumNetworkBase.CLASSIC_ID:
                return CLASSIC_RPC_URL;
            case EthereumNetworkBase.XDAI_ID:
                return XDAI_RPC_URL;
            case EthereumNetworkBase.GOERLI_ID:
                return GOERLI_RPC_URL;
            case EthereumNetworkBase.ARTIS_SIGMA1_ID:
                return ARTIS_SIGMA1_RPC_URL;
            case EthereumNetworkBase.ARTIS_TAU1_ID:
                return ARTIS_TAU1_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }
}
