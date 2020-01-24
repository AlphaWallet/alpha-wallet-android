package com.alphawallet.ethereum;

/* Weiwu 12 Jan 2020: This class eventually will replace the EthereumNetworkBase class in :app
 * one all inteface methods are implemented.
 */

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class EthereumNetworkBase { // implements EthereumNetworkRepositoryType
    private static final int MAINNET_ID = 1;
    private static final int CLASSIC_ID = 61;
    private static final int POA_ID = 99;
    private static final int KOVAN_ID = 42;
    private static final int ROPSTEN_ID = 3;
    private static final int SOKOL_ID = 77;
    private static final int RINKEBY_ID = 4;
    private static final int XDAI_ID = 100;
    private static final int GOERLI_ID = 5;
    private static final int ARTIS_SIGMA1_ID = 246529;
    private static final int ARTIS_TAU1_ID = 246785;

    private static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String CLASSIC_RPC_URL = "https://ethereumclassic.network";
    private static final String XDAI_RPC_URL = "https://dai.poa.network";
    private static final String POA_RPC_URL = "https://core.poa.network/";
    private static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    private static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    private static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";

    private static final String MAINNET_BLOCKSCOUT = "eth/mainnet";
    private static final String CLASSIC_BLOCKSCOUT = "etc/mainnet";
    private static final String XDAI_BLOCKSCOUT = "poa/dai";
    private static final String POA_BLOCKSCOUT = "poa/core";
    private static final String ROPSTEN_BLOCKSCOUT = "eth/ropsten";
    private static final String RINKEBY_BLOCKSCOUT = "eth/rinkeby";
    private static final String SOKOL_BLOCKSCOUT = "poa/sokol";
    private static final String KOVAN_BLOCKSCOUT = "eth/kovan";
    private static final String GOERLI_BLOCKSCOUT = "eth/goerli";

    private static final Map<Integer, NetworkInfo> networkMap = new LinkedHashMap<Integer, NetworkInfo>() {
        {
            put(MAINNET_ID, new NetworkInfo("Ethereum", "ETH", MAINNET_RPC_URL, "https://etherscan.io/tx/",
                    MAINNET_ID, true, "ethereum", MAINNET_BLOCKSCOUT));
            put(CLASSIC_ID, new NetworkInfo("Ethereum Classic", "ETC", CLASSIC_RPC_URL, "https://gastracker.io/tx/",
                    CLASSIC_ID, true, "ethereum-classic", CLASSIC_BLOCKSCOUT));
            put(XDAI_ID, new NetworkInfo("xDAI", "xDAI", XDAI_RPC_URL, "https://blockscout.com/poa/dai/tx/",
                    XDAI_ID, false, "dai", XDAI_BLOCKSCOUT));
            put(POA_ID, new NetworkInfo("POA", "POA", POA_RPC_URL, "https://poaexplorer.com/txid/search/",
                    POA_ID, false, "ethereum", POA_BLOCKSCOUT));
            put(ARTIS_SIGMA1_ID, new NetworkInfo("ARTIS sigma1", "ATS", ARTIS_SIGMA1_RPC_URL, "https://explorer.sigma1.artis.network/tx/",
                    ARTIS_SIGMA1_ID, false, "artis", ""));
            put(KOVAN_ID, new NetworkInfo("Kovan (Test)", "ETH", KOVAN_RPC_URL, "https://kovan.etherscan.io/tx/",
                    KOVAN_ID, false, "ethereum", KOVAN_BLOCKSCOUT));
            put(ROPSTEN_ID, new NetworkInfo("Ropsten (Test)", "ETH", ROPSTEN_RPC_URL, "https://ropsten.etherscan.io/tx/",
                    ROPSTEN_ID, false, "ethereum", ROPSTEN_BLOCKSCOUT));
            put(SOKOL_ID, new NetworkInfo("Sokol (Test)", "POA", SOKOL_RPC_URL, "https://sokol-explorer.poa.network/account/",
                    SOKOL_ID, false, "ethereum", SOKOL_BLOCKSCOUT));
            put(RINKEBY_ID, new NetworkInfo("Rinkeby (Test)", "ETH", RINKEBY_RPC_URL, "https://rinkeby.etherscan.io/tx/",
                    RINKEBY_ID, false, "ethereum", RINKEBY_BLOCKSCOUT));
            put(GOERLI_ID, new NetworkInfo("Görli (Test)", "GÖETH", GOERLI_RPC_URL, "https://goerli.etherscan.io/tx/",
                    GOERLI_ID, false, "ethereum", GOERLI_BLOCKSCOUT));
            put(ARTIS_TAU1_ID, new NetworkInfo("ARTIS tau1 (Test)", "ATS", ARTIS_TAU1_RPC_URL, "https://explorer.tau1.artis.network/tx/",
                    ARTIS_TAU1_ID, false, "artis", ""));
        }
    };

    public static NetworkInfo getNetworkByChain(int chainId) {
        return networkMap.get(chainId);
    }

}