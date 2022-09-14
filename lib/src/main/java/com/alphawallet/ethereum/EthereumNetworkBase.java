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
    public static final long GNOSIS_ID = 100;
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
    public static final long POLYGON_ID = 137;
    public static final long POLYGON_TEST_ID = 80001;
    public static final long OPTIMISTIC_MAIN_ID = 10;
    public static final long OPTIMISTIC_TEST_ID = 69;
    public static final long CRONOS_MAIN_ID = 25;
    public static final long CRONOS_TEST_ID = 338;
    public static final long ARBITRUM_MAIN_ID = 42161;
    public static final long ARBITRUM_TEST_ID = 421611;
    public static final long PALM_ID = 0x2a15c308dL; //11297108109
    public static final long PALM_TEST_ID = 0x2a15c3083L; //11297108099
    public static final long KLAYTN_ID = 8217;
    public static final long KLAYTN_BAOBAB_ID = 1001;
    public static final long IOTEX_MAINNET_ID = 4689;
    public static final long IOTEX_TESTNET_ID = 4690;
    public static final long AURORA_MAINNET_ID = 1313161554;
    public static final long AURORA_TESTNET_ID = 1313161555;
    public static final long MILKOMEDA_C1_ID = 2001;
    public static final long MILKOMEDA_C1_TEST_ID = 200101;
    public static final long PHI_NETWORK_MAIN_ID = 4181;
    public static final long PHI_MAIN_ID = 4181;


    public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    public static final String XDAI_RPC_URL = "https://rpc.ankr.com/gnosis";
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
    public static final String CRONOS_MAIN_RPC_URL = "https://evm.cronos.org";
    public static final String CRONOS_TEST_URL = "https://evm-t3.cronos.org";
    public static final String ARBITRUM_RPC_URL = "https://arbitrum-mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String ARBITRUM_TEST_RPC_URL = "https://arbitrum-rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String PALM_RPC_URL = "https://palm-mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String PALM_TEST_RPC_URL = "https://palm-testnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String KLAYTN_RPC = "https://public-node-api.klaytnapi.com/v1/cypress";
    public static final String KLAYTN_BAOBAB_RPC = "https://api.baobab.klaytn.net:8651";
    public static final String AURORA_MAINNET_RPC_URL = "https://mainnet.aurora.dev";
    public static final String AURORA_TESTNET_RPC_URL = "https://testnet.aurora.dev";
    public static final String MILKOMEDA_C1_RPC = "https://rpc-mainnet-cardano-evm.c1.milkomeda.com";
    public static final String MILKOMEDA_C1_TEST_RPC = "https://rpc-devnet-cardano-evm.c1.milkomeda.com";
    public static final String PHI_MAIN_RPC_URL = "https://rpc1.phi.network";

    static Map<Long, NetworkInfo> networkMap = new LinkedHashMap<Long, NetworkInfo>() {
        {
            put(MAINNET_ID, new NetworkInfo("Ethereum", "ETH", MAINNET_RPC_URL, "https://etherscan.io/tx/",
                    MAINNET_ID, false));
            put(CLASSIC_ID, new NetworkInfo("Ethereum Classic", "ETC", CLASSIC_RPC_URL, "https://blockscout.com/etc/mainnet/tx/",
                    CLASSIC_ID, false));
            put(GNOSIS_ID, new NetworkInfo("Gnosis", "xDAi", XDAI_RPC_URL, "https://blockscout.com/xdai/mainnet/tx/",
                    GNOSIS_ID, false));
            put(POA_ID, new NetworkInfo("POA", "POA", POA_RPC_URL, "https://blockscout.com/poa/core/tx/",
                    POA_ID, false));
            put(ARTIS_SIGMA1_ID, new NetworkInfo("ARTIS sigma1", "ATS", ARTIS_SIGMA1_RPC_URL, "https://explorer.sigma1.artis.network/tx/",
                    ARTIS_SIGMA1_ID, false));
            put(KOVAN_ID, new NetworkInfo("Kovan (Test)", "ETH", KOVAN_RPC_URL, "https://kovan.etherscan.io/tx/",
                    KOVAN_ID, false));
            put(ROPSTEN_ID, new NetworkInfo("Ropsten (Test)", "ETH", ROPSTEN_RPC_URL, "https://ropsten.etherscan.io/tx/",
                    ROPSTEN_ID, false));
            put(SOKOL_ID, new NetworkInfo("Sokol (Test)", "POA", SOKOL_RPC_URL, "https://blockscout.com/poa/sokol/tx/",
                    SOKOL_ID, false));
            put(RINKEBY_ID, new NetworkInfo("Rinkeby (Test)", "ETH", RINKEBY_RPC_URL, "https://rinkeby.etherscan.io/tx/",
                    RINKEBY_ID, false));
            put(GOERLI_ID, new NetworkInfo("Görli (Test)", "GÖETH", GOERLI_RPC_URL, "https://goerli.etherscan.io/tx/",
                    GOERLI_ID, false));
            put(ARTIS_TAU1_ID, new NetworkInfo("ARTIS tau1 (Test)", "ATS", ARTIS_TAU1_RPC_URL, "https://explorer.tau1.artis.network/tx/",
                    ARTIS_TAU1_ID, false));
            put(BINANCE_TEST_ID, new NetworkInfo("BSC TestNet (Test)", "T-BSC", BINANCE_TEST_RPC_URL, "https://explorer.binance.org/smart-testnet/tx/",
                    BINANCE_TEST_ID, false));
            put(BINANCE_MAIN_ID, new NetworkInfo("Binance (BSC)", "BSC", BINANCE_MAIN_RPC_URL, "https://explorer.binance.org/smart/tx/",
                    BINANCE_MAIN_ID, false));
            put(HECO_ID, new NetworkInfo("Heco", "HT", HECO_RPC_URL, "https://hecoinfo.com/tx/",
                    HECO_ID, false));
            put(HECO_TEST_ID, new NetworkInfo("Heco (Test)", "HT", HECO_TEST_RPC_URL, "https://testnet.hecoinfo.com/tx/",
                    HECO_TEST_ID, false));

            put(AVALANCHE_ID, new NetworkInfo("Avalanche Mainnet C-Chain", "AVAX", AVALANCHE_RPC_URL, "https://cchain.explorer.avax.network/tx/",
                    AVALANCHE_ID, false));
            put(FUJI_TEST_ID, new NetworkInfo("Avalanche FUJI C-Chain (Test)", "AVAX", FUJI_TEST_RPC_URL, "https://cchain.explorer.avax-test.network/tx/",
                    FUJI_TEST_ID, false));

            put(FANTOM_ID, new NetworkInfo("Fantom Opera", "FTM", FANTOM_RPC_URL, "https://ftmscan.com/tx/",
                    FANTOM_ID, false));
            put(FANTOM_TEST_ID, new NetworkInfo("Fantom (Test)", "FTM", FANTOM_TEST_RPC_URL, "https://explorer.testnet.fantom.network/tx/",
                    FANTOM_TEST_ID, false));

            put(POLYGON_ID, new NetworkInfo("Polygon", "POLY", MATIC_RPC_URL, "https://polygonscan.com/tx/",
                    POLYGON_ID, false));
            put(POLYGON_TEST_ID, new NetworkInfo("Mumbai (Test)", "POLY", MUMBAI_TEST_RPC_URL, "https://mumbai.polygonscan.com/tx/",
                    POLYGON_TEST_ID, false));

            put(OPTIMISTIC_MAIN_ID, new NetworkInfo("Optimistic","ETH", OPTIMISTIC_MAIN_URL, "https://optimistic.etherscan.io/tx/",
                    OPTIMISTIC_MAIN_ID, false));
            put(OPTIMISTIC_TEST_ID, new NetworkInfo("Optimistic (Test)", "ETH", OPTIMISTIC_TEST_URL, "https://kovan-optimistic.etherscan.io/tx/",
                    OPTIMISTIC_TEST_ID, false));
            put(CRONOS_MAIN_ID, new NetworkInfo("Cronos (Beta)", "CRO", CRONOS_MAIN_RPC_URL, "https://cronoscan.com/tx", CRONOS_MAIN_ID, false));
            put(CRONOS_TEST_ID, new NetworkInfo("Cronos (Test)", "tCRO", CRONOS_TEST_URL, "https://testnet.cronoscan.com/tx/", CRONOS_TEST_ID, false));
            put(ARBITRUM_MAIN_ID, new NetworkInfo("Arbitrum One","AETH", ARBITRUM_RPC_URL, "https://arbiscan.io/tx/",
                    ARBITRUM_MAIN_ID, false));
            put(ARBITRUM_TEST_ID, new NetworkInfo("Arbitrum Test", "ARETH", ARBITRUM_TEST_RPC_URL, "https://rinkeby-explorer.arbitrum.io/tx/",
                    ARBITRUM_TEST_ID, false));

            put(PALM_ID, new NetworkInfo("PALM","PALM", PALM_RPC_URL, "https://explorer.palm.io/tx/",
                    PALM_ID, false));
            put(PALM_TEST_ID, new NetworkInfo("PALM (Test)", "PALM", PALM_TEST_RPC_URL, "https://explorer.palm-uat.xyz/tx/",
                    PALM_TEST_ID, false));

            put(KLAYTN_ID, new NetworkInfo("Klaytn Cypress","KLAY", KLAYTN_RPC, "https://scope.klaytn.com/tx/",
                    KLAYTN_ID, false));
            put(KLAYTN_BAOBAB_ID, new NetworkInfo("Klaytn Baobab (Test)","KLAY", KLAYTN_BAOBAB_RPC, "https://baobab.scope.klaytn.com/tx/",
                    KLAYTN_BAOBAB_ID, false));
            put(AURORA_MAINNET_ID, new NetworkInfo("Aurora","ETH", AURORA_MAINNET_RPC_URL, "https://aurorascan.dev/tx/",
                    AURORA_MAINNET_ID, false));
            put(AURORA_TESTNET_ID, new NetworkInfo("Aurora (Test)","ETH", AURORA_TESTNET_RPC_URL, "https://testnet.aurorascan.dev/tx/",
                    AURORA_TESTNET_ID, false));

            put(MILKOMEDA_C1_ID, new NetworkInfo("Milkomeda Cardano","milkADA", MILKOMEDA_C1_RPC, "https://explorer-mainnet-cardano-evm.c1.milkomeda.com/tx/",
                    MILKOMEDA_C1_ID, false));
            put(MILKOMEDA_C1_TEST_ID, new NetworkInfo("Milkomeda Cardano (Test)","milktADA", MILKOMEDA_C1_TEST_RPC, "https://explorer-devnet-cardano-evm.c1.milkomeda.com/tx/",
                    MILKOMEDA_C1_TEST_ID, false));
            put(PHI_NETWORK_MAIN_ID, new NetworkInfo("PHI", "\u03d5", PHI_MAIN_RPC_URL, "https://explorer.phi.network/tx/",
                    PHI_NETWORK_MAIN_ID, false));
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
