package com.alphawallet.token.web.Service;

import static com.alphawallet.token.entity.MagicLinkInfo.ARTIS_SIGMA1_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.ARTIS_TAU1_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.CLASSIC_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.GOERLI_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.KOVAN_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.MAINNET_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.POA_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.RINKEBY_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.ROPSTEN_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.SOKOL_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.XDAI_NETWORK_ID;
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
            case MAINNET_NETWORK_ID:
                return MAINNET_RPC_URL;
            case KOVAN_NETWORK_ID:
                return KOVAN_RPC_URL;
            case ROPSTEN_NETWORK_ID:
                return ROPSTEN_RPC_URL;
            case RINKEBY_NETWORK_ID:
                return RINKEBY_RPC_URL;
            case POA_NETWORK_ID:
                return POA_RPC_URL;
            case SOKOL_NETWORK_ID:
                return SOKOL_RPC_URL;
            case CLASSIC_NETWORK_ID:
                return CLASSIC_RPC_URL;
            case XDAI_NETWORK_ID:
                return XDAI_RPC_URL;
            case GOERLI_NETWORK_ID:
                return GOERLI_RPC_URL;
            case ARTIS_SIGMA1_NETWORK_ID:
                return ARTIS_SIGMA1_RPC_URL;
            case ARTIS_TAU1_NETWORK_ID:
                return ARTIS_TAU1_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }
}
