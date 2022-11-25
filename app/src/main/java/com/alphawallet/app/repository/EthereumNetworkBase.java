package com.alphawallet.app.repository;

/* Please don't add import android at this point. Later this file will be shared
 * between projects including non-Android projects */

import static com.alphawallet.app.util.Utils.isValidUrl;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_GOERLI_TESTNET_FALLBACK_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_GOERLI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_SIGMA1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_TESTNET_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GOERLI_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_MAINNET_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_TESTNET_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.KOVAN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_TEST_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISM_GOERLI_TESTNET_FALLBACK_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISM_GOERLI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_FALLBACK_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_TEST_FALLBACK_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.PALM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.PALM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROPSTEN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.SOKOL_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_RPC_URL;

import android.text.TextUtils;
import android.util.LongSparseArray;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ChainSpec;
import com.google.gson.Gson;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;

public abstract class EthereumNetworkBase implements EthereumNetworkRepositoryType
{
    public static final String COVALENT = "[COVALENT]";

    private static final String GAS_API = "module=gastracker&action=gasoracle";

    public static final String DEFAULT_INFURA_KEY = "da3717f25f824cc1baa32d812386d93f";
    /* constructing URLs from BuildConfig. In the below area you will see hardcoded key like da3717...
       These hardcoded keys are fallbacks used by AlphaWallet forks.

       Also note: If you are running your own node and wish to use that; currently it must be hardcoded here
       If you wish your node to be the primary node that AW checks then replace the relevant ..._RPC_URL below
       If you wish your node to be the fallback, tried in case the primary times out then add/replace in ..._FALLBACK_RPC_URL list
     */

    private static final KeyProvider keyProvider = KeyProviderFactory.get();
    public static final boolean usesProductionKey = !keyProvider.getInfuraKey().equals(DEFAULT_INFURA_KEY);

    public static final String FREE_MAINNET_RPC_URL = "https://rpc.ankr.com/eth";
    public static final String FREE_POLYGON_RPC_URL = "https://polygon-rpc.com";
    public static final String FREE_ARBITRUM_RPC_URL = "https://arbitrum.public-rpc.com";
    public static final String FREE_GOERLI_RPC_URL = "https://rpc.ankr.com/eth_goerli";
    public static final String FREE_MUMBAI_RPC_URL = "https://rpc-mumbai.maticvigil.com";
    public static final String FREE_ARBITRUM_TEST_RPC_URL = "https://rinkeby.arbitrum.io/rpc";
    public static final String FREE_PALM_RPC_URL = "https://palm-mainnet.infura.io/v3/3a961d6501e54add9a41aa53f15de99b";
    public static final String FREE_PALM_TEST_RPC_URL = "https://palm-testnet.infura.io/v3/3a961d6501e54add9a41aa53f15de99b";
    public static final String FREE_CRONOS_MAIN_BETA_RPC_URL = "https://evm.cronos.org";

    public static final String MAINNET_RPC_URL = usesProductionKey ? "https://mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_MAINNET_RPC_URL;
    public static final String GOERLI_RPC_URL = usesProductionKey ? "https://goerli.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_GOERLI_RPC_URL;
    public static final String POLYGON_RPC_URL = usesProductionKey ? "https://polygon-mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_POLYGON_RPC_URL;
    public static final String ARBITRUM_MAINNET_RPC = usesProductionKey ? "https://arbitrum-mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_ARBITRUM_RPC_URL;
    public static final String MUMBAI_TEST_RPC_URL = usesProductionKey ? "https://polygon-mumbai.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_MUMBAI_RPC_URL;
    public static final String OPTIMISTIC_MAIN_URL = usesProductionKey ? "https://optimism-mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : OPTIMISTIC_MAIN_FALLBACK_URL;
    public static final String PALM_RPC_URL = usesProductionKey ? "https://palm-mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_PALM_RPC_URL;
    public static final String PALM_TEST_RPC_URL = usesProductionKey ? "https://palm-testnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_PALM_TEST_RPC_URL;
    public static final String USE_KLAYTN_RPC = usesProductionKey ? "https://node-api.klaytnapi.com/v1/klaytn"
            : KLAYTN_RPC;
    public static final String USE_KLAYTN_BAOBAB_RPC = usesProductionKey ? "https://node-api.klaytnapi.com/v1/klaytn"
            : KLAYTN_BAOBAB_RPC;
    public static final String CRONOS_MAIN_RPC_URL = "https://evm.cronos.org";

    // Use the "Free" routes as backup in order to diversify node usage; to avoid single point of failure
    public static final String MAINNET_FALLBACK_RPC_URL = usesProductionKey ? FREE_MAINNET_RPC_URL : "https://mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String GOERLI_FALLBACK_RPC_URL = usesProductionKey ? FREE_GOERLI_RPC_URL : "https://goerli.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String ARBITRUM_FALLBACK_MAINNET_RPC = usesProductionKey ? FREE_ARBITRUM_RPC_URL : "https://arbitrum-mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String PALM_RPC_FALLBACK_URL = usesProductionKey ? FREE_PALM_RPC_URL : "https://palm-mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String PALM_TEST_RPC_FALLBACK_URL = usesProductionKey ? FREE_PALM_RPC_URL : "https://palm-testnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();

    //Deprecated: for now these RPCs still work
    public static final String ROPSTEN_RPC_URL = "https://rpc.ankr.com/eth_ropsten";
    public static final String RINKEBY_RPC_URL = "https://rpc.ankr.com/eth_rinkeby";
    public static final String ARBITRUM_TESTNET_RPC = FREE_ARBITRUM_TEST_RPC_URL;
    public static final String SOKOL_RPC_URL = "https://sokol.poa.network";

    //These Networks are no longer running
    public static final String KOVAN_RPC_URL = "https://kovan.poa.network";
    public static final String OPTIMISTIC_TEST_URL = OPTIMISTIC_TEST_FALLBACK_URL;

    //Note that AlphaWallet now uses a double node configuration. See class AWHttpService comment 'try primary node'.
    //If you supply a main RPC and secondary it will try the secondary if the primary node times out after 10 seconds.
    //See the declaration of NetworkInfo - it has a member backupNodeUrl. Put your secondary node here.

    public static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    public static final String POA_RPC_URL = "https://core.poa.network/";
    public static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    public static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";
    public static final String BINANCE_TEST_RPC_URL = "https://data-seed-prebsc-1-s3.binance.org:8545";
    public static final String BINANCE_TEST_FALLBACK_RPC_URL = "https://data-seed-prebsc-2-s1.binance.org:8545";
    public static final String BINANCE_MAIN_RPC_URL = "https://bsc-dataseed.binance.org";
    public static final String BINANCE_MAIN_FALLBACK_RPC_URL = "https://bsc-dataseed2.ninicoin.io:443";
    public static final String POLYGON_FALLBACK_RPC_URL = "https://matic-mainnet.chainstacklabs.com";
    public static final String MUMBAI_FALLBACK_RPC_URL = "https://matic-mumbai.chainstacklabs.com";
    public static final String CRONOS_TEST_URL = "https://evm-t3.cronos.org";
    public static final String ARBITRUM_FALLBACK_TESTNET_RPC = "https://rinkeby.arbitrum.io/rpc";

    public static final String IOTEX_MAINNET_RPC_FALLBACK_URL = "https://rpc.ankr.com/iotex";
    public static final String OPTIMISM_GOERLI_TESTNET_RPC_URL = "https://optimism-goerli.infura.io/v3/" + keyProvider.getInfuraKey();
    public static final String ARBITRUM_GOERLI_TESTNET_RPC_URL = "https://arbitrum-goerli.infura.io/v3/" + keyProvider.getInfuraKey();

    //All chains that have fiat/real value (not testnet) must be put here
    //Note: This list also determines the order of display for main net chains in the wallet.
    //If your wallet prioritises xDai for example, you may want to move the XDAI_ID to the front of this list,
    //Then xDai would appear as the first token at the top of the wallet
    private static final List<Long> hasValue = new ArrayList<>(Arrays.asList(
            MAINNET_ID, GNOSIS_ID, POLYGON_ID, CLASSIC_ID, POA_ID, ARTIS_SIGMA1_ID, BINANCE_MAIN_ID, HECO_ID, AVALANCHE_ID,
            FANTOM_ID, OPTIMISTIC_MAIN_ID, CRONOS_MAIN_ID, ARBITRUM_MAIN_ID, PALM_ID, KLAYTN_ID, IOTEX_MAINNET_ID, AURORA_MAINNET_ID, MILKOMEDA_C1_ID));

    private static final List<Long> testnetList = new ArrayList<>(Arrays.asList(
            GOERLI_ID, BINANCE_TEST_ID, HECO_TEST_ID, CRONOS_TEST_ID, OPTIMISM_GOERLI_TEST_ID, ARBITRUM_GOERLI_TEST_ID, KLAYTN_BAOBAB_ID,
            FANTOM_TEST_ID, IOTEX_TESTNET_ID, FUJI_TEST_ID, POLYGON_TEST_ID, MILKOMEDA_C1_TEST_ID, ARTIS_TAU1_ID,
            SEPOLIA_TESTNET_ID, AURORA_TESTNET_ID, PALM_TEST_ID,
            //Deprecated networks
            ROPSTEN_ID, RINKEBY_ID, KOVAN_ID, OPTIMISTIC_TEST_ID, SOKOL_ID, ARBITRUM_TEST_ID));

    private static final List<Long> deprecatedNetworkList = new ArrayList<>(Arrays.asList(
            ROPSTEN_ID, RINKEBY_ID, KOVAN_ID, OPTIMISTIC_TEST_ID, SOKOL_ID, ARBITRUM_TEST_ID));

    // for reset built-in network
    private static final LongSparseArray<NetworkInfo> builtinNetworkMap = new LongSparseArray<NetworkInfo>()
    {
        {
            put(MAINNET_ID, new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://cn.etherscan.com/tx/", MAINNET_ID,
                    MAINNET_FALLBACK_RPC_URL, "https://api-cn.etherscan.com/api?"));
            put(CLASSIC_ID, new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.ETC_SYMBOL,
                    CLASSIC_RPC_URL,
                    "https://blockscout.com/etc/mainnet/tx/", CLASSIC_ID, CLASSIC_RPC_URL,
                    "https://blockscout.com/etc/mainnet/api?"));
            put(GNOSIS_ID, new NetworkInfo(C.XDAI_NETWORK_NAME, C.xDAI_SYMBOL,
                    XDAI_RPC_URL,
                    "https://blockscout.com/xdai/mainnet/tx/", GNOSIS_ID,
                    "https://gnosis.public-rpc.com", "https://blockscout.com/xdai/mainnet/api?"));
            put(POA_ID, new NetworkInfo(C.POA_NETWORK_NAME, C.POA_SYMBOL,
                    POA_RPC_URL,
                    "https://blockscout.com/poa/core/tx/", POA_ID, POA_RPC_URL,
                    "https://blockscout.com/poa/core/api?"));
            put(ARTIS_SIGMA1_ID, new NetworkInfo(C.ARTIS_SIGMA1_NETWORK, C.ARTIS_SIGMA1_SYMBOL,
                    ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/tx/", ARTIS_SIGMA1_ID,
                    ARTIS_SIGMA1_RPC_URL, "https://explorer.sigma1.artis.network/api?"));
            put(GOERLI_ID, new NetworkInfo(C.GOERLI_NETWORK_NAME, C.GOERLI_SYMBOL,
                    GOERLI_RPC_URL,
                    "https://goerli.etherscan.io/tx/", GOERLI_ID,
                    GOERLI_FALLBACK_RPC_URL, "https://api-goerli.etherscan.io/api?"));
            put(ARTIS_TAU1_ID, new NetworkInfo(C.ARTIS_TAU1_NETWORK, C.ARTIS_TAU1_SYMBOL,
                    ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/tx/", ARTIS_TAU1_ID,
                    ARTIS_TAU1_RPC_URL, "https://explorer.tau1.artis.network/api?"));
            put(BINANCE_TEST_ID, new NetworkInfo(C.BINANCE_TEST_NETWORK, C.BINANCE_SYMBOL,
                    BINANCE_TEST_RPC_URL,
                    "https://testnet.bscscan.com/tx/", BINANCE_TEST_ID,
                    BINANCE_TEST_FALLBACK_RPC_URL, "https://api-testnet.bscscan.com/api?"));
            put(BINANCE_MAIN_ID, new NetworkInfo(C.BINANCE_MAIN_NETWORK, C.BINANCE_SYMBOL,
                    BINANCE_MAIN_RPC_URL,
                    "https://bscscan.com/tx/", BINANCE_MAIN_ID,
                    BINANCE_MAIN_FALLBACK_RPC_URL, "https://api.bscscan.com/api?"));
            put(HECO_ID, new NetworkInfo(C.HECO_MAIN_NETWORK, C.HECO_SYMBOL,
                    HECO_RPC_URL,
                    "https://hecoinfo.com/tx/", HECO_ID,
                    HECO_RPC_URL, "https://api.hecoinfo.com/api?"));
            put(HECO_TEST_ID, new NetworkInfo(C.HECO_TEST_NETWORK, C.HECO_SYMBOL,
                    HECO_TEST_RPC_URL,
                    "https://testnet.hecoinfo.com/tx/", HECO_TEST_ID,
                    HECO_TEST_RPC_URL, "https://testnet.hecoinfo.com/api?"));
            put(AVALANCHE_ID, new NetworkInfo(C.AVALANCHE_NETWORK, C.AVALANCHE_SYMBOL,
                    AVALANCHE_RPC_URL,
                    "https://cchain.explorer.avax.network/tx/", AVALANCHE_ID,
                    AVALANCHE_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT));
            put(FUJI_TEST_ID, new NetworkInfo(C.FUJI_TEST_NETWORK, C.AVALANCHE_SYMBOL,
                    FUJI_TEST_RPC_URL,
                    "https://cchain.explorer.avax-test.network/tx/", FUJI_TEST_ID,
                    FUJI_TEST_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT));
            put(FANTOM_ID, new NetworkInfo(C.FANTOM_NETWORK, C.FANTOM_SYMBOL,
                    FANTOM_RPC_URL,
                    "https://ftmscan.com/tx/", FANTOM_ID,
                    FANTOM_RPC_URL, "https://api.ftmscan.com/api?"));
            put(FANTOM_TEST_ID, new NetworkInfo(C.FANTOM_TEST_NETWORK, C.FANTOM_SYMBOL,
                    FANTOM_TEST_RPC_URL,
                    "https://explorer.testnet.fantom.network/tx/", FANTOM_TEST_ID,
                    FANTOM_TEST_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT)); //NB: Fantom testnet not yet supported by Covalent
            put(POLYGON_ID, new NetworkInfo(C.POLYGON_NETWORK, C.POLYGON_SYMBOL, POLYGON_RPC_URL,
                    "https://polygonscan.com/tx/", POLYGON_ID,
                    POLYGON_FALLBACK_RPC_URL, "https://api.polygonscan.com/api?"));
            put(POLYGON_TEST_ID, new NetworkInfo(C.POLYGON_TEST_NETWORK, C.POLYGON_SYMBOL,
                    MUMBAI_TEST_RPC_URL,
                    "https://mumbai.polygonscan.com/tx/", POLYGON_TEST_ID,
                    MUMBAI_FALLBACK_RPC_URL, " https://api-testnet.polygonscan.com/api?"));
            put(OPTIMISTIC_MAIN_ID, new NetworkInfo(C.OPTIMISTIC_NETWORK, C.ETH_SYMBOL,
                    OPTIMISTIC_MAIN_URL,
                    "https://optimistic.etherscan.io/tx/", OPTIMISTIC_MAIN_ID, OPTIMISTIC_MAIN_FALLBACK_URL,
                    "https://api-optimistic.etherscan.io/api?"));
            put(CRONOS_MAIN_ID, new NetworkInfo(C.CRONOS_MAIN_NETWORK, C.CRONOS_SYMBOL,
                    CRONOS_MAIN_RPC_URL,
                    "https://cronoscan.com/tx/", CRONOS_MAIN_ID, CRONOS_MAIN_RPC_URL,
                    "https://api.cronoscan.com/api?"));
            put(CRONOS_TEST_ID, new NetworkInfo(C.CRONOS_TEST_NETWORK, C.CRONOS_TEST_SYMBOL,
                    CRONOS_TEST_URL,
                    "https://testnet.cronoscan.com/tx/", CRONOS_TEST_ID, CRONOS_TEST_URL,
                    "https://testnet.cronoscan.com/api?"));
            put(ARBITRUM_MAIN_ID, new NetworkInfo(C.ARBITRUM_ONE_NETWORK, C.ARBITRUM_SYMBOL,
                    ARBITRUM_MAINNET_RPC,
                    "https://arbiscan.io/tx/", ARBITRUM_MAIN_ID, ARBITRUM_FALLBACK_MAINNET_RPC,
                    "https://api.arbiscan.io/api?"));
            put(PALM_ID, new NetworkInfo(C.PALM_NAME, C.PALM_SYMBOL,
                    PALM_RPC_URL,
                    "https://explorer.palm.io/tx/", PALM_ID, PALM_RPC_FALLBACK_URL,
                    "https://explorer.palm.io/api?"));
            put(PALM_TEST_ID, new NetworkInfo(C.PALM_TEST_NAME, C.PALM_SYMBOL,
                    PALM_TEST_RPC_URL,
                    "https://explorer.palm-uat.xyz/tx/", PALM_TEST_ID, PALM_TEST_RPC_FALLBACK_URL,
                    "https://explorer.palm-uat.xyz/api?"));
            put(KLAYTN_ID, new NetworkInfo(C.KLAYTN_NAME, C.KLAYTN_SYMBOL,
                    USE_KLAYTN_RPC,
                    "https://scope.klaytn.com/tx/", KLAYTN_ID, KLAYTN_RPC,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(KLAYTN_BAOBAB_ID, new NetworkInfo(C.KLAYTN_BAOBAB_NAME, C.KLAYTN_SYMBOL,
                    USE_KLAYTN_BAOBAB_RPC,
                    "https://baobab.scope.klaytn.com/tx/", KLAYTN_BAOBAB_ID, KLAYTN_BAOBAB_RPC,
                    ""));
            put(IOTEX_MAINNET_ID, new NetworkInfo(C.IOTEX_NAME, C.IOTEX_SYMBOL,
                    IOTEX_MAINNET_RPC_URL,
                    "https://iotexscan.io/tx/", IOTEX_MAINNET_ID, IOTEX_MAINNET_RPC_FALLBACK_URL,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(IOTEX_TESTNET_ID, new NetworkInfo(C.IOTEX_TESTNET_NAME, C.IOTEX_SYMBOL,
                    IOTEX_TESTNET_RPC_URL,
                    "https://testnet.iotexscan.io/tx/", IOTEX_TESTNET_ID, "",
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(AURORA_MAINNET_ID, new NetworkInfo(C.AURORA_MAINNET_NAME, C.ETH_SYMBOL, AURORA_MAINNET_RPC_URL,
                    "https://aurorascan.dev/tx/", AURORA_MAINNET_ID, "",
                    "https://api.aurorascan.dev/api?"));
            put(AURORA_TESTNET_ID, new NetworkInfo(C.AURORA_TESTNET_NAME, C.ETH_SYMBOL, AURORA_TESTNET_RPC_URL,
                    "https://testnet.aurorascan.dev/tx/", AURORA_TESTNET_ID, "",
                    "https://api-testnet.aurorascan.dev/api?"));
            put(MILKOMEDA_C1_ID, new NetworkInfo(C.MILKOMEDA_NAME, C.MILKOMEDA_SYMBOL,
                    MILKOMEDA_C1_RPC,
                    "https://explorer-mainnet-cardano-evm.c1.milkomeda.com/tx/", MILKOMEDA_C1_ID, "",
                    "https://explorer-mainnet-cardano-evm.c1.milkomeda.com/api?"));
            put(MILKOMEDA_C1_TEST_ID, new NetworkInfo(C.MILKOMEDA_TESTNET_NAME, C.MILKOMEDA_TEST_SYMBOL,
                    MILKOMEDA_C1_TEST_RPC,
                    "https://explorer-devnet-cardano-evm.c1.milkomeda.com/tx/", MILKOMEDA_C1_TEST_ID, "",
                    "https://explorer-devnet-cardano-evm.c1.milkomeda.com/api?"));
            put(SEPOLIA_TESTNET_ID, new NetworkInfo(C.SEPOLIA_TESTNET_NAME, C.SEPOLIA_SYMBOL,
                    SEPOLIA_TESTNET_RPC_URL,
                    "https://sepolia.etherscan.io/tx/", SEPOLIA_TESTNET_ID, "https://rpc2.sepolia.org",
                    "https://api-sepolia.etherscan.io/api?"));
            put(OPTIMISM_GOERLI_TEST_ID, new NetworkInfo(C.OPTIMISM_GOERLI_TESTNET_NAME, C.OPTIMISM_GOERLI_TEST_SYMBOL,
                    OPTIMISM_GOERLI_TESTNET_RPC_URL,
                    "https://blockscout.com/optimism/goerli/tx/", OPTIMISM_GOERLI_TEST_ID, OPTIMISM_GOERLI_TESTNET_FALLBACK_RPC_URL,
                    "https://blockscout.com/optimism/goerli/api?"));
            put(ARBITRUM_GOERLI_TEST_ID, new NetworkInfo(C.ARBITRUM_GOERLI_TESTNET_NAME, C.ARBITRUM_SYMBOL,
                    ARBITRUM_GOERLI_TESTNET_RPC_URL,
                    "https://goerli-rollup-explorer.arbitrum.io/tx/", ARBITRUM_GOERLI_TEST_ID, ARBITRUM_GOERLI_TESTNET_FALLBACK_RPC_URL,
                    "https://goerli-rollup-explorer.arbitrum.io/api?"));

            // Deprecated Networks
            put(ROPSTEN_ID, new NetworkInfo(C.ROPSTEN_NETWORK_NAME, C.ETH_SYMBOL,
                    ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/", ROPSTEN_ID,
                    ROPSTEN_RPC_URL, "https://api-ropsten.etherscan.io/api?"));
            put(RINKEBY_ID, new NetworkInfo(C.RINKEBY_NETWORK_NAME, C.ETH_SYMBOL,
                    RINKEBY_RPC_URL,
                    "https://rinkeby.etherscan.io/tx/", RINKEBY_ID,
                    RINKEBY_RPC_URL, "https://api-rinkeby.etherscan.io/api?"));
            put(KOVAN_ID, new NetworkInfo(C.KOVAN_NETWORK_NAME, C.ETH_SYMBOL,
                    KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", KOVAN_ID,
                    KOVAN_RPC_URL, "https://api-kovan.etherscan.io/api?"));
            put(SOKOL_ID, new NetworkInfo(C.SOKOL_NETWORK_NAME, C.POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://blockscout.com/poa/sokol/tx/", SOKOL_ID,
                    SOKOL_RPC_URL, "https://blockscout.com/poa/sokol/api?"));
            put(OPTIMISTIC_TEST_ID, new NetworkInfo(C.OPTIMISTIC_TEST_NETWORK, C.ETH_SYMBOL,
                    OPTIMISTIC_TEST_URL,
                    "https://kovan-optimistic.etherscan.io/tx/", OPTIMISTIC_TEST_ID, OPTIMISTIC_TEST_FALLBACK_URL,
                    "https://api-kovan-optimistic.etherscan.io/api?"));
            put(ARBITRUM_TEST_ID, new NetworkInfo(C.ARBITRUM_TEST_NETWORK, C.ARBITRUM_TEST_SYMBOL,
                    ARBITRUM_TESTNET_RPC,
                    "https://testnet.arbiscan.io/tx/", ARBITRUM_TEST_ID, ARBITRUM_FALLBACK_TESTNET_RPC,
                    "https://testnet.arbiscan.io/api?")); //no transaction API
        }
    };

    //List of network details. Note, the advantage of using LongSparseArray is efficiency and also
    //the entries are automatically sorted into numerical order
    private static final LongSparseArray<NetworkInfo> networkMap = builtinNetworkMap.clone();

    private static final LongSparseArray<Integer> chainLogos = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.drawable.ic_token_eth);
            put(KOVAN_ID, R.drawable.ic_kovan);
            put(ROPSTEN_ID, R.drawable.ic_ropsten);
            put(RINKEBY_ID, R.drawable.ic_rinkeby);
            put(CLASSIC_ID, R.drawable.ic_icons_network_etc); //classic_logo
            put(POA_ID, R.drawable.ic_poa_logo);
            put(SOKOL_ID, R.drawable.ic_icons_tokens_sokol);
            put(GNOSIS_ID, R.drawable.ic_icons_network_gnosis);
            put(GOERLI_ID, R.drawable.ic_goerli);
            put(ARTIS_SIGMA1_ID, R.drawable.ic_artis_sigma_logo);
            put(ARTIS_TAU1_ID, R.drawable.ic_artis_tau_logo);
            put(BINANCE_MAIN_ID, R.drawable.ic_binance_logo);
            put(BINANCE_TEST_ID, R.drawable.ic_icons_tokens_bnb_testnet);
            put(HECO_ID, R.drawable.ic_heco_logo);
            put(HECO_TEST_ID, R.drawable.ic_icons_tokens_heco_testnet);
            put(FANTOM_ID, R.drawable.ic_fantom);
            put(FANTOM_TEST_ID, R.drawable.ic_icons_fantom_test);
            put(AVALANCHE_ID, R.drawable.ic_icons_tokens_avalanche);
            put(FUJI_TEST_ID, R.drawable.ic_icons_tokens_avalanche_testnet);
            put(POLYGON_ID, R.drawable.ic_icons_polygon);
            put(POLYGON_TEST_ID, R.drawable.ic_icons_tokens_mumbai);
            put(OPTIMISTIC_MAIN_ID, R.drawable.ic_optimism_logo);
            put(OPTIMISTIC_TEST_ID, R.drawable.ic_optimism_testnet_logo);
            put(CRONOS_MAIN_ID, R.drawable.ic_cronos_mainnet);
            put(CRONOS_TEST_ID, R.drawable.ic_cronos);
            put(ARBITRUM_MAIN_ID, R.drawable.ic_icons_arbitrum);
            put(ARBITRUM_TEST_ID, R.drawable.ic_icons_arbitrum_test);
            put(PALM_ID, R.drawable.ic_icons_network_palm);
            put(PALM_TEST_ID, R.drawable.palm_logo_test);
            put(KLAYTN_ID, R.drawable.ic_klaytn_network_logo);
            put(KLAYTN_BAOBAB_ID, R.drawable.ic_klaytn_test);
            put(IOTEX_MAINNET_ID, R.drawable.ic_iotex);
            put(IOTEX_TESTNET_ID, R.drawable.ic_iotex_test);
            put(AURORA_MAINNET_ID, R.drawable.ic_aurora);
            put(AURORA_TESTNET_ID, R.drawable.ic_aurora_test);
            put(MILKOMEDA_C1_ID, R.drawable.ic_milkomeda);
            put(MILKOMEDA_C1_TEST_ID, R.drawable.ic_milkomeda_test);
            put(SEPOLIA_TESTNET_ID, R.drawable.ic_sepolia_test);
            put(OPTIMISM_GOERLI_TEST_ID, R.drawable.ic_optimism_testnet_logo);
            put(ARBITRUM_GOERLI_TEST_ID, R.drawable.ic_icons_arbitrum_test);
        }
    };

    private static final LongSparseArray<Integer> smallChainLogos = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.drawable.ic_icons_network_eth);
            put(KOVAN_ID, R.drawable.ic_kovan);
            put(ROPSTEN_ID, R.drawable.ic_ropsten);
            put(RINKEBY_ID, R.drawable.ic_rinkeby);
            put(CLASSIC_ID, R.drawable.ic_icons_network_etc);
            put(POA_ID, R.drawable.ic_icons_network_poa);
            put(SOKOL_ID, R.drawable.ic_icons_tokens_sokol);
            put(GNOSIS_ID, R.drawable.ic_icons_network_gnosis);
            put(GOERLI_ID, R.drawable.ic_goerli);
            put(ARTIS_SIGMA1_ID, R.drawable.ic_icons_network_artis);
            put(ARTIS_TAU1_ID, R.drawable.ic_artis_tau_logo);
            put(BINANCE_MAIN_ID, R.drawable.ic_icons_network_bsc);
            put(BINANCE_TEST_ID, R.drawable.ic_icons_tokens_bnb_testnet);
            put(HECO_ID, R.drawable.ic_icons_network_heco);
            put(HECO_TEST_ID, R.drawable.ic_icons_tokens_heco_testnet);
            put(FANTOM_ID, R.drawable.ic_icons_network_fantom);
            put(FANTOM_TEST_ID, R.drawable.ic_icons_fantom_test);
            put(AVALANCHE_ID, R.drawable.ic_icons_network_avalanche);
            put(FUJI_TEST_ID, R.drawable.ic_icons_tokens_avalanche_testnet);
            put(POLYGON_ID, R.drawable.ic_icons_network_polygon);
            put(POLYGON_TEST_ID, R.drawable.ic_icons_tokens_mumbai);
            put(OPTIMISTIC_MAIN_ID, R.drawable.ic_icons_network_optimism);
            put(OPTIMISTIC_TEST_ID, R.drawable.ic_optimism_testnet_logo);
            put(CRONOS_MAIN_ID, R.drawable.ic_cronos_mainnet);
            put(CRONOS_TEST_ID, R.drawable.ic_cronos);
            put(ARBITRUM_MAIN_ID, R.drawable.ic_icons_network_arbitrum);
            put(ARBITRUM_TEST_ID, R.drawable.ic_icons_arbitrum_test);
            put(PALM_ID, R.drawable.ic_icons_network_palm);
            put(PALM_TEST_ID, R.drawable.palm_logo_test);
            put(KLAYTN_ID, R.drawable.ic_klaytn_network_logo);
            put(KLAYTN_BAOBAB_ID, R.drawable.ic_klaytn_test);
            put(IOTEX_MAINNET_ID, R.drawable.ic_iotex);
            put(IOTEX_TESTNET_ID, R.drawable.ic_iotex_test);
            put(AURORA_MAINNET_ID, R.drawable.ic_aurora);
            put(AURORA_TESTNET_ID, R.drawable.ic_aurora_test);
            put(MILKOMEDA_C1_ID, R.drawable.ic_milkomeda);
            put(MILKOMEDA_C1_TEST_ID, R.drawable.ic_milkomeda_test);
            put(SEPOLIA_TESTNET_ID, R.drawable.ic_sepolia_test);
            put(OPTIMISM_GOERLI_TEST_ID, R.drawable.ic_optimism_testnet_logo);
            put(ARBITRUM_GOERLI_TEST_ID, R.drawable.ic_icons_arbitrum_test);
        }
    };

    private static final LongSparseArray<Integer> chainColours = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.color.mainnet);
            put(KOVAN_ID, R.color.kovan);
            put(ROPSTEN_ID, R.color.ropsten);
            put(RINKEBY_ID, R.color.rinkeby);
            put(CLASSIC_ID, R.color.classic);
            put(POA_ID, R.color.poa);
            put(SOKOL_ID, R.color.sokol);
            put(GNOSIS_ID, R.color.xdai);
            put(GOERLI_ID, R.color.goerli);
            put(ARTIS_SIGMA1_ID, R.color.artis_sigma1);
            put(ARTIS_TAU1_ID, R.color.artis_tau1);
            put(BINANCE_MAIN_ID, R.color.binance_main);
            put(BINANCE_TEST_ID, R.color.binance_test);
            put(HECO_ID, R.color.heco_main);
            put(HECO_TEST_ID, R.color.heco_test);
            put(FANTOM_ID, R.color.fantom_main);
            put(FANTOM_TEST_ID, R.color.fantom_test);
            put(AVALANCHE_ID, R.color.avalanche_main);
            put(FUJI_TEST_ID, R.color.avalanche_test);
            put(POLYGON_ID, R.color.polygon_main);
            put(POLYGON_TEST_ID, R.color.polygon_test);
            put(OPTIMISTIC_MAIN_ID, R.color.optimistic_main);
            put(OPTIMISTIC_TEST_ID, R.color.optimistic_test);
            put(CRONOS_MAIN_ID, R.color.cronos_main);
            put(CRONOS_TEST_ID, R.color.cronos_test);
            put(ARBITRUM_MAIN_ID, R.color.arbitrum_main);
            put(ARBITRUM_TEST_ID, R.color.arbitrum_test);
            put(PALM_ID, R.color.palm_main);
            put(PALM_TEST_ID, R.color.palm_test);
            put(KLAYTN_ID, R.color.klaytn_main);
            put(KLAYTN_BAOBAB_ID, R.color.klaytn_test);
            put(IOTEX_MAINNET_ID, R.color.iotex_mainnet);
            put(IOTEX_TESTNET_ID, R.color.iotex_mainnet);
            put(AURORA_MAINNET_ID, R.color.aurora_mainnet);
            put(AURORA_TESTNET_ID, R.color.aurora_testnet);
            put(MILKOMEDA_C1_ID, R.color.milkomeda);
            put(MILKOMEDA_C1_TEST_ID, R.color.milkomeda_test);
            put(SEPOLIA_TESTNET_ID, R.color.sepolia);
            put(OPTIMISM_GOERLI_TEST_ID, R.color.optimistic_test);
            put(ARBITRUM_GOERLI_TEST_ID, R.color.arbitrum_test);
        }
    };

    //Does the chain have a gas oracle?
    //Add it to this list here if so. Note that so far, all gas oracles follow the same format:
    //  <etherscanAPI from the above list> + GAS_API
    //If the gas oracle you're adding doesn't follow this spec then you'll have to change the getGasOracle method
    private static final List<Long> hasGasOracleAPI = Arrays.asList(MAINNET_ID, HECO_ID, BINANCE_MAIN_ID, POLYGON_ID);

    //These chains don't allow custom gas
    private static final List<Long> hasLockedGas = Arrays.asList(OPTIMISTIC_MAIN_ID, OPTIMISTIC_TEST_ID, ARBITRUM_MAIN_ID, ARBITRUM_TEST_ID, KLAYTN_ID, KLAYTN_BAOBAB_ID);

    private static final List<Long> hasOpenSeaAPI = Arrays.asList(MAINNET_ID, POLYGON_ID, RINKEBY_ID);

    private static final LongSparseArray<BigInteger> blockGasLimit = new LongSparseArray<BigInteger>()
    {
        {
            put(MAINNET_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX));
            put(KLAYTN_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX_KLAYTN));
            put(AURORA_MAINNET_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX_AURORA));
        }
    };

    public static String getGasOracle(long chainId)
    {
        if (hasGasOracleAPI.contains(chainId) && networkMap.indexOfKey(chainId) >= 0)
        {
            return networkMap.get(chainId).etherscanAPI + GAS_API;
        }
        else
        {
            return "";
        }
    }

    public static int getChainOrdinal(long chainId)
    {
        if (hasValue.contains(chainId))
        {
            return hasValue.indexOf(chainId);
        }
        else if (networkMap.indexOfKey(chainId) >= 0)
        {
            return networkMap.indexOfKey(chainId);
        }
        else
        {
            return 500 + (int) chainId % 500; //fixed ID above 500
        }
    }

    //TODO: Refactor when we bump the version of java to allow switch on Long (Finally!!)
    //Also TODO: add a test to check these batch limits of each chain we support
    private static int batchProcessingLimit(long chainId)
    {
        NetworkInfo info = builtinNetworkMap.get(chainId);
        if (info.rpcServerUrl.contains("infura")) //infura supported chains can handle tx batches of 1000 and up
        {
            return 512;
        }
        else if (info.rpcServerUrl.contains("klaytn"))
        {
            return 0;
        }
        else if (info.rpcServerUrl.contains("gnosis"))
        {
            return 6; //TODO: Check limit
        }
        else if (info.rpcServerUrl.contains("cronos.org"))
        {
            return 5; //TODO: Check limit
        }
        else
        {
            return 32;
        }
    }

    private static final LongSparseArray<Integer> batchProcessingLimitMap = new LongSparseArray<>();

    //Init the batch limits
    private static void setBatchProcessingLimits()
    {
        for (int i = 0; i < builtinNetworkMap.size(); i++)
        {
            NetworkInfo info = builtinNetworkMap.valueAt(i);
            batchProcessingLimitMap.put(info.chainId, batchProcessingLimit(info.chainId));
        }
    }

    public static int getBatchProcessingLimit(long chainId)
    {
        if (batchProcessingLimitMap.size() == 0) setBatchProcessingLimits(); //If batch limits not set, init them and proceed
        return batchProcessingLimitMap.get(chainId, 0); //default to zero / no batching
    }

    @Override
    public boolean hasLockedGas(long chainId)
    {
        return hasLockedGas.contains(chainId);
    }

    static final Map<Long, String> addressOverride = new HashMap<Long, String>()
    {
        {
            put(OPTIMISTIC_MAIN_ID, "0x4200000000000000000000000000000000000006");
            put(OPTIMISTIC_TEST_ID, "0x4200000000000000000000000000000000000006");
        }
    };

    final PreferenceRepositoryType preferences;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();
    final boolean useTestNets;
    final NetworkInfo[] additionalNetworks;


    static class CustomNetworks
    {
        private ArrayList<NetworkInfo> list = new ArrayList<>();
        private Map<Long, Boolean> mapToTestNet = new HashMap<>();
        final transient private PreferenceRepositoryType preferences;

        public CustomNetworks(PreferenceRepositoryType preferences)
        {
            this.preferences = preferences;
            restore();
        }

        public void restore()
        {
            String networks = preferences.getCustomRPCNetworks();
            if (!TextUtils.isEmpty(networks))
            {
                CustomNetworks cn = new Gson().fromJson(networks, CustomNetworks.class);
                this.list = cn.list;
                this.mapToTestNet = cn.mapToTestNet;
                checkCustomNetworkSetting();

                for (NetworkInfo info : list)
                {
                    if (!isValidUrl(info.rpcServerUrl)) //ensure RPC doesn't contain malicious code
                    {
                        continue;
                    }

                    networkMap.put(info.chainId, info);
                    Boolean value = mapToTestNet.get(info.chainId);
                    boolean isTestnet = value != null && value;
                    if (!isTestnet && !hasValue.contains(info.chainId))
                    {
                        hasValue.add(info.chainId);
                    }
                    else if (isTestnet && !testnetList.contains(info.chainId))
                    {
                        testnetList.add(info.chainId);
                    }
                }
            }
        }

        private void checkCustomNetworkSetting()
        {
            if (list.size() > 0 && !list.get(0).isCustom)
            { //need to update the list
                List<NetworkInfo> copyList = new ArrayList<>(list);
                list.clear();
                for (NetworkInfo n : copyList)
                {
                    boolean isCustom = builtinNetworkMap.indexOfKey(n.chainId) == -1;
                    NetworkInfo newInfo = new NetworkInfo(n.name, n.symbol, n.rpcServerUrl, n.etherscanUrl, n.chainId, n.backupNodeUrl, n.etherscanAPI, isCustom);
                    list.add(newInfo);
                }
                //record back
                preferences.setCustomRPCNetworks(new Gson().toJson(this));
            }
        }

        public void save(NetworkInfo info, boolean isTestnet, Long oldChainId)
        {
            if (oldChainId != null)
            {
                updateNetwork(info, isTestnet, oldChainId);
            }
            else
            {
                addNetwork(info, isTestnet);
            }

            String networks = new Gson().toJson(this);
            preferences.setCustomRPCNetworks(networks);
        }

        private void updateNetwork(NetworkInfo info, boolean isTestnet, long oldChainId)
        {
            removeNetwork(oldChainId);
            list.add(info);
            if (!isTestnet)
            {
                hasValue.add(info.chainId);
            }
            else
            {
                testnetList.add(info.chainId);
            }
            mapToTestNet.put(info.chainId, isTestnet);
            networkMap.put(info.chainId, info);
        }

        private void addNetwork(NetworkInfo info, boolean isTestnet)
        {
            list.add(info);
            if (!isTestnet)
            {
                hasValue.add(info.chainId);
            }
            else
            {
                testnetList.add(info.chainId);
            }
            mapToTestNet.put(info.chainId, isTestnet);
            networkMap.put(info.chainId, info);
        }

        public void remove(long chainId)
        {
            removeNetwork(chainId);

            String networks = new Gson().toJson(this);
            preferences.setCustomRPCNetworks(networks);
        }

        private void removeNetwork(long chainId)
        {
            for (NetworkInfo in : list)
            {
                if (in.chainId == chainId)
                {
                    list.remove(in);
                    break;
                }
            }
            hasValue.remove(chainId);
            mapToTestNet.remove(chainId);
            networkMap.remove(chainId);
        }
    }

    private static CustomNetworks customNetworks;

    EthereumNetworkBase(PreferenceRepositoryType preferenceRepository, NetworkInfo[] additionalNetworks, boolean useTestNets)
    {
        this.preferences = preferenceRepository;
        this.additionalNetworks = additionalNetworks;
        this.useTestNets = useTestNets;

        customNetworks = new CustomNetworks(this.preferences);
    }

    private void addNetworks(NetworkInfo[] networks, List<NetworkInfo> result, boolean withValue)
    {
        for (NetworkInfo network : networks)
        {
            if (EthereumNetworkRepository.hasRealValue(network.chainId) == withValue
                    && !result.contains(network)) result.add(network);
        }
    }

    private void addNetworks(List<NetworkInfo> result, boolean withValue)
    {
        if (withValue)
        {
            for (long networkId : hasValue)
            {
                if (!deprecatedNetworkList.contains(networkId))
                {
                    result.add(networkMap.get(networkId));
                }
            }

            for (long networkId : hasValue)
            {
                if (deprecatedNetworkList.contains(networkId))
                {
                    result.add(networkMap.get(networkId));
                }
            }
        }
        else
        {
            for (long networkId : testnetList)
            {
                if (!deprecatedNetworkList.contains(networkId))
                {
                    result.add(networkMap.get(networkId));
                }
            }

            for (long networkId : testnetList)
            {
                if (deprecatedNetworkList.contains(networkId))
                {
                    result.add(networkMap.get(networkId));
                }
            }
        }
    }

    public static String getChainOverrideAddress(long chainId)
    {
        return addressOverride.containsKey(chainId) ? addressOverride.get(chainId) : "";
    }

    @Override
    public String getNameById(long chainId)
    {
        if (networkMap.indexOfKey(chainId) >= 0) return networkMap.get(chainId).name;
        else return "Unknown: " + chainId;
    }

    @Override
    public NetworkInfo getActiveBrowserNetwork()
    {
        long activeNetwork = preferences.getActiveBrowserNetwork();
        return networkMap.get(activeNetwork);
    }

    @Override
    public NetworkInfo getNetworkByChain(long chainId)
    {
        return networkMap.get(chainId);
    }

    // fetches the last transaction nonce; if it's identical to the last used one then increment by one
    // to ensure we don't get transaction replacement
    @Override
    public Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress)
    {
        return Single.fromCallable(() ->
        {
            try
            {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                        .send();
                return ethGetTransactionCount.getTransactionCount();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    @Override
    public List<Long> getFilterNetworkList()
    {
        return getSelectedFilters(preferences.isActiveMainnet());
    }

    @Override
    public List<Long> getSelectedFilters(boolean isMainNet)
    {
        String filterList = preferences.getNetworkFilterList();
        List<Long> storedIds = Utils.longListToArray(filterList);
        List<Long> selectedIds = new ArrayList<>();

        for (Long networkId : storedIds)
        {
            if (hasRealValue(networkId) == isMainNet)
            {
                NetworkInfo check = networkMap.get(networkId);
                if (check != null) selectedIds.add(networkId);
            }
        }

        if (selectedIds.size() == 0)
        {
            selectedIds.add(getDefaultNetwork(isMainNet));
        }

        return selectedIds;
    }

    @Override
    public Long getDefaultNetwork(boolean isMainNet)
    {
        return isMainNet ? CustomViewSettings.primaryChain : GOERLI_ID;
    }

    @Override
    public void setFilterNetworkList(Long[] networkList)
    {
        String store = Utils.longArrayToString(networkList);
        preferences.setNetworkFilterList(store);
    }

    @Override
    public void setActiveBrowserNetwork(NetworkInfo networkInfo)
    {
        if (networkInfo != null)
        {
            preferences.setActiveBrowserNetwork(networkInfo.chainId);
            for (OnNetworkChangeListener listener : onNetworkChangedListeners)
            {
                listener.onNetworkChanged(networkInfo);
            }
        }
        else
        {
            preferences.setActiveBrowserNetwork(0);
        }
    }

    @Override
    public NetworkInfo[] getAvailableNetworkList()
    {
        //construct on demand, and give in order
        /* merging static compile time network list with runtime network list */
        List<NetworkInfo> networks = new ArrayList<>();

        addNetworks(additionalNetworks, networks, true);
        addNetworks(networks, true);
        /* the order is passed to the user interface. So if a user has a token on one
         * of the additionalNetworks, the same token on DEFAULT_NETWORKS, and on a few
         * test nets, they are displayed by that order.
         */
        addNetworks(additionalNetworks, networks, false);
        if (useTestNets) addNetworks(networks, false);
        return networks.toArray(new NetworkInfo[0]);
    }

    @Override
    public NetworkInfo[] getAllActiveNetworks()
    {
        NetworkInfo[] allNetworks = getAvailableNetworkList();
        List<NetworkInfo> networks = new ArrayList<>();
        addNetworks(allNetworks, networks, preferences.isActiveMainnet());
        return networks.toArray(new NetworkInfo[0]);
    }

    @Override
    public void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged)
    {
        onNetworkChangedListeners.add(onNetworkChanged);
    }

    public static boolean hasRealValue(long chainId)
    {
        return hasValue.contains(chainId);
    }

    public static List<Long> getAllMainNetworks()
    {
        return hasValue;
    }

    public static String getSecondaryNodeURL(long networkId)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info != null)
        {
            return info.backupNodeUrl;
        }
        else
        {
            return "";
        }
    }

    //TODO: Fold this into file and add to database
    public static int getChainLogo(long networkId)
    {
        if (chainLogos.indexOfKey(networkId) >= 0)
        {
            return chainLogos.get(networkId);
        }
        else
        {
            return R.drawable.ic_ethereum_generic;
        }
    }

    public static int getSmallChainLogo(long networkId)
    {
        if (smallChainLogos.indexOfKey(networkId) >= 0)
        {
            return smallChainLogos.get(networkId);
        }
        else
        {
            return getChainLogo(networkId);
        }
    }

    public static int getChainColour(long chainId)
    {
        if (chainColours.indexOfKey(chainId) >= 0)
        {
            return chainColours.get(chainId);
        }
        else
        {
            return R.color.text_primary;
        }
    }

    public static BigInteger getMaxGasLimit(long chainId)
    {
        return blockGasLimit.get(chainId, blockGasLimit.get(MAINNET_ID));
    }

    public static String getNodeURLByNetworkId(long networkId)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info != null)
        {
            return info.rpcServerUrl;
        }
        else
        {
            return MAINNET_RPC_URL;
        }
    }

    /**
     * This is used so as not to leak API credentials to web3; XInfuraAPI is the backup API key checked into github
     *
     * @param networkId
     * @return
     */
    public static String getDefaultNodeURL(long networkId)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info != null) return info.rpcServerUrl;
        else return "";
    }

    public static long getNetworkIdFromName(String name)
    {
        if (!TextUtils.isEmpty(name))
        {
            for (int i = 0; i < networkMap.size(); i++)
            {
                if (name.equals(networkMap.valueAt(i).name))
                {
                    return networkMap.valueAt(i).chainId;
                }
            }
        }
        return 0;
    }

    //Note: this is used by chains which have a fixed, invariable gas price.
    //      it mainly only applies to private or custom chains, eg a Kaleido based chain
    //      public chains will almost never use this.
    public static boolean hasGasOverride(long chainId)
    {
        return false;
    }

    public static boolean hasOpenseaAPI(long chainId)
    {
        return hasOpenSeaAPI.contains(chainId);
    }

    public static BigInteger gasOverrideValue(long chainId)
    {
        return BigInteger.valueOf(1);
    }

    public static List<ChainSpec> extraChains()
    {
        return null;
    }

    public static List<Long> addDefaultNetworks()
    {
        return CustomViewSettings.alwaysVisibleChains;
    }

    public static ContractLocator getOverrideToken()
    {
        return new ContractLocator("", CustomViewSettings.primaryChain, ContractType.ETHEREUM);
    }

    @Override
    public boolean isChainContract(long chainId, String address)
    {
        return (addressOverride.containsKey(chainId) && address.equalsIgnoreCase(addressOverride.get(chainId)));
    }

    public static boolean isPriorityToken(Token token)
    {
        return false;
    }

    public static long getPriorityOverride(Token token)
    {
        if (token.isEthereum()) return token.tokenInfo.chainId + 1;
        else return 0;
    }

    public static int decimalOverride(String address, long chainId)
    {
        return 0;
    }

    public Token getBlankOverrideToken(NetworkInfo networkInfo)
    {
        return createCurrencyToken(networkInfo);
    }

    public Single<Token[]> getBlankOverrideTokens(Wallet wallet)
    {
        return Single.fromCallable(() ->
        {
            if (getBlankOverrideToken() == null)
            {
                return new Token[0];
            }
            else
            {
                Token[] tokens = new Token[1];
                tokens[0] = getBlankOverrideToken();
                tokens[0].setTokenWallet(wallet.address);
                return tokens;
            }
        });
    }

    private static Token createCurrencyToken(NetworkInfo network)
    {
        TokenInfo tokenInfo = new TokenInfo(Address.DEFAULT.toString(), network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(Address.DEFAULT.toString());
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }

    public Token getBlankOverrideToken()
    {
        return null;
    }

    public String getCurrentWalletAddress()
    {
        return preferences.getCurrentWalletAddress();
    }

    public boolean hasSetNetworkFilters()
    {
        return preferences.hasSetNetworkFilters();
    }

    public void setHasSetNetworkFilters()
    {
        preferences.setHasSetNetworkFilters();
    }

    public boolean isMainNetSelected()
    {
        return preferences.isActiveMainnet();
    }

    @Override
    public void setActiveMainnet(boolean isMainNet)
    {
        preferences.setActiveMainnet(isMainNet);
    }

    public void saveCustomRPCNetwork(String networkName, String rpcUrl, long chainId, String symbol, String blockExplorerUrl, String explorerApiUrl, boolean isTestnet, Long oldChainId)
    {

        NetworkInfo builtInNetwork = builtinNetworkMap.get(chainId);
        boolean isCustom = builtInNetwork == null;
        NetworkInfo info = new NetworkInfo(networkName, symbol, rpcUrl, blockExplorerUrl, chainId, isCustom ? null : builtInNetwork.backupNodeUrl, explorerApiUrl, isCustom);
        customNetworks.save(info, isTestnet, oldChainId);
    }

    public void removeCustomRPCNetwork(long chainId)
    {
        customNetworks.remove(chainId);
    }

    public static NetworkInfo getNetworkInfo(long chainId)
    {
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

    public static BigInteger getMaxEventFetch(long chainId)
    {
        if (chainId == POLYGON_ID || chainId == POLYGON_TEST_ID)
        {
            return BigInteger.valueOf(3500L);
        }
        else
        {
            return BigInteger.valueOf(10000L);
        }
    }

    @Override
    public NetworkInfo getBuiltInNetwork(long chainId)
    {
        return builtinNetworkMap.get(chainId);
    }

    public static boolean isNetworkDeprecated(long chainId)
    {
        return deprecatedNetworkList.contains(chainId);
    }
}
