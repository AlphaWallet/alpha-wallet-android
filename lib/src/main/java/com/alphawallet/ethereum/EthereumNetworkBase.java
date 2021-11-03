package com.alphawallet.ethereum;

/* Weiwu 12 Jan 2020: This class eventually will replace the EthereumNetworkBase class in :app
 * one all interface methods are implemented.
 */

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class EthereumNetworkBase { // implements EthereumNetworkRepositoryType
    public static final long MAINNET_ID = 1;
    public static final long CLASSIC_ID = 61;
    public static final long POA_ID = 99;
    public static final long KOVAN_ID = 42;
    public static final long ROPSTEN_ID = 3;
    public static final long SOKOL_ID = 77;
    public static final long RINKEBY_ID = 4;
    public static final long XDAI_ID = 100;
    public static final long GOERLI_ID = 5;
    public static final long ARTIS_SIGMA1_ID = 246529;
    public static final long ARTIS_TAU1_ID = 246785;
    public static final long BINANCE_TEST_ID = 97;
    public static final long BINANCE_MAIN_ID = 56;
    public static final long HECO_ID = 128;
    public static final long HECO_TEST_ID = 256;
    public static final long FANTOM_ID = 250;
    public static final long FANTOM_TEST_ID = 4002;
    public static final long AVALANCHE_ID = 43114;
    public static final long FUJI_TEST_ID = 43113;
    public static final long MATIC_ID = 137;
    public static final long MATIC_TEST_ID = 80001;
    public static final long OPTIMISTIC_MAIN_ID = 10;
    public static final long OPTIMISTIC_TEST_ID = 69;
    public static final long CRONOS_TEST_ID = 338;
    public static final long ARBITRUM_MAIN_ID = 42161;
    public static final long ARBITRUM_TEST_ID = 421611;
    public static final long PALM_ID = 0x2a15c308dL; //11297108109
    public static final long PALM_TEST_ID = 0x2a15c3083L; //11297108099


    public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    public static final String XDAI_RPC_URL = "https://rpc.xdaichain.com";
    public static final String POA_RPC_URL = "https://core.poa.network/";
    public static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    public static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    public static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";
    public static final String BINANCE_TEST_RPC_URL = "https://data-seed-prebsc-1-s3.binance.org:8545";
    public static final String BINANCE_MAIN_RPC_URL = "https://bsc-dataseed.binance.org";
    public static final String HECO_RPC_URL = "https://http-mainnet-node.huobichain.com";
    public static final String HECO_TEST_RPC_URL = "https://http-testnet.hecochain.com";
    public static final String AVALANCHE_RPC_URL = "https://api.avax.network/ext/bc/C/rpc";
    public static final String FUJI_TEST_RPC_URL = "https://api.avax-test.network/ext/bc/C/rpc";
    public static final String FANTOM_RPC_URL = "https://rpcapi.fantom.network";
    public static final String FANTOM_TEST_RPC_URL = "https://rpc.testnet.fantom.network";
    public static final String MATIC_RPC_URL = "https://matic-mainnet.chainstacklabs.com";
    public static final String MUMBAI_TEST_RPC_URL = "https://matic-mumbai.chainstacklabs.com";
    public static final String OPTIMISTIC_MAIN_URL = "https://mainnet.optimism.io";
    public static final String OPTIMISTIC_TEST_URL = "https://kovan.optimism.io";
    public static final String CRONOS_TEST_URL = "http://cronos-testnet.crypto.org:8545";
    public static final String ARBITRUM_RPC_URL = "https://arbitrum-mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String ARBITRUM_TEST_RPC_URL = "https://arbitrum-rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String PALM_RPC_URL = "https://palm-mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String PALM_TEST_RPC_URL = "https://palm-testnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
  
    static Map<Long, NetworkInfo> networkMap = new LinkedHashMap<Long, NetworkInfo>() {
        {
            put(MAINNET_ID, new NetworkInfo("Ethereum", "ETH", MAINNET_RPC_URL, "https://etherscan.io/tx/",
                    MAINNET_ID));
            put(CLASSIC_ID, new NetworkInfo("Ethereum Classic", "ETC", CLASSIC_RPC_URL, "https://blockscout.com/etc/mainnet/tx/",
                    CLASSIC_ID));
            put(XDAI_ID, new NetworkInfo("xDAI", "xDAI", XDAI_RPC_URL, "https://blockscout.com/xdai/mainnet/tx/",
                    XDAI_ID));
            put(POA_ID, new NetworkInfo("POA", "POA", POA_RPC_URL, "https://blockscout.com/poa/core/tx/",
                    POA_ID));
            put(ARTIS_SIGMA1_ID, new NetworkInfo("ARTIS sigma1", "ATS", ARTIS_SIGMA1_RPC_URL, "https://explorer.sigma1.artis.network/tx/",
                    ARTIS_SIGMA1_ID));
            put(KOVAN_ID, new NetworkInfo("Kovan (Test)", "ETH", KOVAN_RPC_URL, "https://kovan.etherscan.io/tx/",
                    KOVAN_ID));
            put(ROPSTEN_ID, new NetworkInfo("Ropsten (Test)", "ETH", ROPSTEN_RPC_URL, "https://ropsten.etherscan.io/tx/",
                    ROPSTEN_ID));
            put(SOKOL_ID, new NetworkInfo("Sokol (Test)", "POA", SOKOL_RPC_URL, "https://blockscout.com/poa/sokol/tx/",
                    SOKOL_ID));
            put(RINKEBY_ID, new NetworkInfo("Rinkeby (Test)", "ETH", RINKEBY_RPC_URL, "https://rinkeby.etherscan.io/tx/",
                    RINKEBY_ID));
            put(GOERLI_ID, new NetworkInfo("Görli (Test)", "GÖETH", GOERLI_RPC_URL, "https://goerli.etherscan.io/tx/",
                    GOERLI_ID));
            put(ARTIS_TAU1_ID, new NetworkInfo("ARTIS tau1 (Test)", "ATS", ARTIS_TAU1_RPC_URL, "https://explorer.tau1.artis.network/tx/",
                    ARTIS_TAU1_ID));
            put(BINANCE_TEST_ID, new NetworkInfo("BSC TestNet (Test)", "T-BSC", BINANCE_TEST_RPC_URL, "https://explorer.binance.org/smart-testnet/tx/",
                    BINANCE_MAIN_ID));
            put(BINANCE_MAIN_ID, new NetworkInfo("Binance (BSC)", "BSC", BINANCE_MAIN_RPC_URL, "https://explorer.binance.org/smart/tx/",
                    BINANCE_TEST_ID));
            put(HECO_ID, new NetworkInfo("Heco", "HT", HECO_RPC_URL, "https://hecoinfo.com/tx/",
                    HECO_ID));
            put(HECO_TEST_ID, new NetworkInfo("Heco (Test)", "HT", HECO_TEST_RPC_URL, "https://testnet.hecoinfo.com/tx/",
                    HECO_TEST_ID));

            put(AVALANCHE_ID, new NetworkInfo("Avalanche Mainnet C-Chain", "AVAX", AVALANCHE_RPC_URL, "https://cchain.explorer.avax.network/tx/",
                    AVALANCHE_ID));
            put(FUJI_TEST_ID, new NetworkInfo("Avalanche FUJI C-Chain (Test)", "AVAX", FUJI_TEST_RPC_URL, "https://cchain.explorer.avax-test.network/tx/",
                    FUJI_TEST_ID));

            put(FANTOM_ID, new NetworkInfo("Fantom Opera", "FTM", FANTOM_RPC_URL, "https://ftmscan.com/tx/",
                    FANTOM_ID));
            put(FANTOM_TEST_ID, new NetworkInfo("Fantom (Test)", "FTM", FANTOM_TEST_RPC_URL, "https://explorer.testnet.fantom.network/tx/",
                    FANTOM_TEST_ID));

            put(MATIC_ID, new NetworkInfo("Polygon", "POLY", MATIC_RPC_URL, "https://polygonscan.com/tx/",
                    MATIC_ID));
            put(MATIC_TEST_ID, new NetworkInfo("Mumbai (Test)", "POLY", MUMBAI_TEST_RPC_URL, "https://mumbai.polygonscan.com/tx/",
                    MATIC_TEST_ID));

            put(OPTIMISTIC_MAIN_ID, new NetworkInfo("Optimistic","ETH", OPTIMISTIC_MAIN_URL, "https://optimistic.etherscan.io/tx/",
                    OPTIMISTIC_MAIN_ID));
            put(OPTIMISTIC_TEST_ID, new NetworkInfo("Optimistic (Test)", "ETH", OPTIMISTIC_TEST_URL, "https://kovan-optimistic.etherscan.io/tx/",
                    OPTIMISTIC_TEST_ID));
            put(CRONOS_TEST_ID, new NetworkInfo("Cronos (Test)", "tCRO", CRONOS_TEST_URL, "https://cronos-explorer.crypto.org/tx/",CRONOS_TEST_ID));
            put(ARBITRUM_MAIN_ID, new NetworkInfo("Arbitrum One","AETH", ARBITRUM_RPC_URL, "https://arbiscan.io/tx/",
                    ARBITRUM_MAIN_ID));
            put(ARBITRUM_TEST_ID, new NetworkInfo("Arbitrum Test", "ARETH", ARBITRUM_TEST_RPC_URL, "https://rinkeby-explorer.arbitrum.io/tx/",
                    ARBITRUM_TEST_ID));

            put(PALM_ID, new NetworkInfo("PALM","PALM", PALM_RPC_URL, "https://explorer.palm.io/tx/",
                    PALM_ID));
            put(PALM_TEST_ID, new NetworkInfo("PALM (Test)", "PALM", PALM_TEST_RPC_URL, "https://explorer.palm-uat.xyz/tx/",
                    PALM_TEST_ID));
        }
    };

    public static NetworkInfo getNetworkByChain(long chainId) {
        return networkMap.get(chainId);
    }

    public static String getShortChainName(long chainId)
    {
        NetworkInfo info = networkMap.get(chainId);
        if (info != null)
        {
            String shortName = info.name;
            int index = shortName.indexOf(" (Test)");
            if (index > 0) shortName = info.name.substring(0, index);
            if (shortName.length() > networkMap.get(CLASSIC_ID).name.length()) //shave off the last word
            {
                shortName = shortName.substring(0, shortName.lastIndexOf(" "));
            }
            return shortName;
        }
        else
        {
            return networkMap.get(MAINNET_ID).name;
        }
    }

    public static String getChainSymbol(long chainId)
    {
        NetworkInfo info = networkMap.get(chainId);
        if (info != null)
        {
            return info.symbol;
        }
        else
        {
            return networkMap.get(MAINNET_ID).symbol;
        }
    }
}
