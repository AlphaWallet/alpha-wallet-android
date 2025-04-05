package com.alphawallet.app.repository;

/* Please don't add import android at this point. Later this file will be shared
 * between projects including non-Android projects */

import static com.alphawallet.app.entity.EventSync.BLOCK_SEARCH_INTERVAL;
import static com.alphawallet.app.entity.EventSync.OKX_BLOCK_SEARCH_INTERVAL;
import static com.alphawallet.app.entity.EventSync.POLYGON_BLOCK_SEARCH_INTERVAL;
import static com.alphawallet.app.util.Utils.isValidUrl;
import static com.alphawallet.ethereum.EthereumNetworkBase.AMOY_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_FREE_MAINNET_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_FREE_TESTNET_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GOERLI_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HOLESKY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_TEST_FREE_RPC;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MANTLE_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MANTLE_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MINT_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MINT_SEPOLIA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OKX_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_FALLBACK_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.PALM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.PALM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_AMOY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROOTSTOCK_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROOTSTOCK_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_RPC_URL;

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
import com.google.gson.reflect.TypeToken;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final String INFURA_GAS_API = "https://gas.api.infura.io/networks/CHAIN_ID/suggestedGasFees";

    public static final String FREE_MAINNET_RPC_URL = "https://rpc.ankr.com/eth";
    public static final String FREE_POLYGON_RPC_URL = "https://polygon-rpc.com";
    public static final String FREE_ARBITRUM_RPC_URL = "https://arbitrum.public-rpc.com";
    public static final String FREE_GOERLI_RPC_URL = "https://rpc.ankr.com/eth_goerli";
    public static final String FREE_MUMBAI_RPC_URL = "https://rpc-mumbai.maticvigil.com";
    public static final String FREE_PALM_RPC_URL = "https://palm-mainnet.infura.io/v3/3a961d6501e54add9a41aa53f15de99b";
    public static final String FREE_PALM_TEST_RPC_URL = "https://palm-testnet.infura.io/v3/3a961d6501e54add9a41aa53f15de99b";
    public static final String FREE_CRONOS_MAIN_BETA_RPC_URL = "https://evm.cronos.org";

    public static final String MAINNET_RPC_URL = usesProductionKey ? "https://mainnet.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_MAINNET_RPC_URL;
    public static final String GOERLI_RPC_URL = usesProductionKey ? "https://goerli.infura.io/v3/" + keyProvider.getInfuraKey()
            : FREE_GOERLI_RPC_URL;

    public static final String SEPOLIA_RPC_URL = usesProductionKey ? "https://sepolia.infura.io/v3/" + keyProvider.getInfuraKey()
            : SEPOLIA_TESTNET_RPC_URL;
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

    public static final String HOLESKY_BACKUP_RPC_URL = usesProductionKey ? "https://holesky.infura.io/v3/" + keyProvider.getInfuraKey()
            : "https://holesky.infura.io/v3/da3717f25f824cc1baa32d812386d93f";

    public static final String AMOY_RPC = usesProductionKey ? "https://polygon-amoy.infura.io/v3/" + keyProvider.getInfuraKey()
            : AMOY_TEST_RPC_URL;

    public static final String AMOY_RPC_FALLBACK = usesProductionKey ? AMOY_TEST_RPC_URL : "https://polygon-amoy-bor-rpc.publicnode.com";



    public static final String USE_KLAYTN_RPC = "https://public-en.node.kaia.io";//!TextUtils.isEmpty(keyProvider.getBlockPiCypressKey()) ? "https://klaytn.blockpi.network/v1/rpc/" + keyProvider.getBlockPiCypressKey()
    public static final String USE_KLAYTN_BAOBAB_RPC = "https://rpc.ankr.com/klaytn_testnet";

    public static final String CRONOS_MAIN_RPC_URL = "https://evm.cronos.org";

    public static final String MINT_MAINNET_FALLBACK = "https://asia.rpc.mintchain.io";

    // Use the "Free" routes as backup in order to diversify node usage; to avoid single point of failure
    public static final String MAINNET_FALLBACK_RPC_URL = usesProductionKey ? FREE_MAINNET_RPC_URL : "https://mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String GOERLI_FALLBACK_RPC_URL = usesProductionKey ? FREE_GOERLI_RPC_URL : "https://goerli.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String ARBITRUM_FALLBACK_MAINNET_RPC = usesProductionKey ? FREE_ARBITRUM_RPC_URL : "https://arbitrum-mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String PALM_RPC_FALLBACK_URL = usesProductionKey ? FREE_PALM_RPC_URL : "https://palm-mainnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String PALM_TEST_RPC_FALLBACK_URL = usesProductionKey ? FREE_PALM_RPC_URL : "https://palm-testnet.infura.io/v3/" + keyProvider.getSecondaryInfuraKey();
    public static final String LINEA_TEST_RPC = usesProductionKey ? "https://linea-sepolia.infura.io/v3/" + keyProvider.getInfuraKey() : LINEA_TEST_FREE_RPC;
    public static final String BASE_RPC = usesProductionKey ? "https://base-mainnet.infura.io/v3/" + keyProvider.getInfuraKey() : BASE_FREE_MAINNET_RPC;
    public static final String BASE_FALLBACK_RPC = usesProductionKey ? BASE_FREE_MAINNET_RPC : "https://base-mainnet.public.blastapi.io";
    public static final String BASE_TEST_RPC = usesProductionKey ? "https://base-sepolia.infura.io/v3/" + keyProvider.getInfuraKey() : BASE_FREE_TESTNET_RPC;
    public static final String BASE_TEST_FALLBACK_RPC = usesProductionKey ? BASE_FREE_TESTNET_RPC : "https://sepolia.base.org";

    //Note that AlphaWallet now uses a double node configuration. See class AWHttpService comment 'try primary node'.
    //If you supply a main RPC and secondary it will try the secondary if the primary node times out after 10 seconds.
    //See the declaration of NetworkInfo - it has a member backupNodeUrl. Put your secondary node here.

    public static final String CLASSIC_RPC_URL = "https://etc.rivet.link";
    public static final String MUMBAI_FALLBACK_RPC_URL = "https://matic-mumbai.chainstacklabs.com";

    //All chains that have fiat/real value (not testnet) must be put here
    //Note: This list also determines the order of display for main net chains in the wallet.
    //If your wallet prioritises xDai for example, you may want to move the XDAI_ID to the front of this list,
    //Then xDai would appear as the first token at the top of the wallet
    private static final List<Long> hasValue = new ArrayList<>(Arrays.asList(
            MAINNET_ID, GNOSIS_ID, POLYGON_ID, ROOTSTOCK_MAINNET_ID, CLASSIC_ID, LINEA_ID, BASE_MAINNET_ID, MANTLE_MAINNET_ID, MINT_ID, BINANCE_MAIN_ID, AVALANCHE_ID,
            FANTOM_ID, OPTIMISTIC_MAIN_ID, CRONOS_MAIN_ID, ARBITRUM_MAIN_ID, PALM_ID, KLAYTN_ID, IOTEX_MAINNET_ID, AURORA_MAINNET_ID, MILKOMEDA_C1_ID, OKX_ID));

    private static final List<Long> testnetList = new ArrayList<>(Arrays.asList(
            SEPOLIA_TESTNET_ID, POLYGON_AMOY_ID, HOLESKY_ID, BASE_TESTNET_ID, MINT_SEPOLIA_TESTNET_ID, GOERLI_ID, BINANCE_TEST_ID,
            ROOTSTOCK_TESTNET_ID, CRONOS_TEST_ID, MANTLE_TESTNET_ID, POLYGON_TEST_ID, ARBITRUM_TEST_ID, LINEA_TEST_ID, KLAYTN_BAOBAB_ID,
            FANTOM_TEST_ID, IOTEX_TESTNET_ID, FUJI_TEST_ID,
            AURORA_TESTNET_ID, PALM_TEST_ID));

    private static final List<Long> deprecatedNetworkList = new ArrayList<>(Arrays.asList(
            // Add deprecated testnet IDs here
            POLYGON_TEST_ID, GOERLI_ID
    ));

    public static final Map<Long, String[]> CHAIN_CONFIG_RPC = new HashMap<>();

    static
    {
        CHAIN_CONFIG_RPC.put(MAINNET_ID, new String[]{
                "https://rpc.payload.de",
                "https://eth-mainnet.public.blastapi.io",
                "https://rpc.ankr.com/eth",
                "https://1rpc.io/eth",
                "https://api.mycryptoapi.com/eth",
                "https://cloudflare-eth.com",
                "https://ethereum-rpc.publicnode.com",
                "https://mainnet.gateway.tenderly.co",
                "https://rpc.blocknative.com/boost",
                "https://rpc.flashbots.net",
                "https://rpc.flashbots.net/fast",
                "https://rpc.mevblocker.io",
                "https://rpc.mevblocker.io/fast",
                "https://rpc.mevblocker.io/noreverts",
                "https://rpc.mevblocker.io/fullprivacy",
                "https://eth.drpc.org",
                "https://eth.llamarpc.com"
        });
        CHAIN_CONFIG_RPC.put(SEPOLIA_TESTNET_ID, new String[]{
                "https://sepolia.infura.io/v3/" + keyProvider.getInfuraKey(), // Assuming keyProvider.getInfuraKey() exists and is accessible
                "https://eth-sepolia.api.onfinality.io/public",
                "https://rpc.sepolia.org"
        });
        CHAIN_CONFIG_RPC.put(HOLESKY_ID, new String[]{
                "https://holesky.drpc.org",
                "https://holesky.gateway.tenderly.co",
                "https://1rpc.io/holesky",
                "https://api.zan.top/eth-holesky",
                "https://rpc-holesky.rockx.com",
                "https://ethereum-holesky-rpc.publicnode.com"
        });
        CHAIN_CONFIG_RPC.put(POLYGON_ID, new String[]{
                "https://polygon.llamarpc.com",
                "https://polygon-rpc.com/",
                "https://polygon.lava.build",
                "https://rpc.ankr.com/polygon",
                "https://polygon.meowrpc.com"
                /* "https://polygon-bor.publicnode.com",
                   "https://polygon.gateway.tenderly.co" */  //Note: commented out lines from original TS
        });
        CHAIN_CONFIG_RPC.put(BINANCE_MAIN_ID, new String[]{
                "https://binance.llamarpc.com",
                "https://bsc.drpc.org",
                "https://bsc.rpc.blxrbdn.com",
                "https://bsc.blockrazor.xyz",
                "https://bsc.drpc.org",
                "https://bsc-rpc.publicnode.com",
                "https://bsc-dataseed.binance.org/",
                "https://bsc-dataseed1.bnbchain.org",
                "https://bsc-dataseed2.bnbchain.org",
                "https://bsc-dataseed3.bnbchain.org",
                "https://bsc-dataseed4.bnbchain.org",
                "https://bsc-dataseed1.defibit.io",
                "https://bsc-dataseed2.defibit.io",
                "https://bsc-dataseed3.defibit.io",
                "https://bsc-dataseed4.defibit.io",
                "https://bsc-dataseed1.ninicoin.io",
                "https://bsc-dataseed2.ninicoin.io",
                "https://bsc-dataseed3.ninicoin.io",
                "https://bsc-dataseed4.ninicoin.io",
                "https://bsc-rpc.publicnode.com"
        });
        CHAIN_CONFIG_RPC.put(BINANCE_TEST_ID, new String[]{
                "https://api.zan.top/bsc-testnet",
                "https://bsc-testnet-rpc.publicnode.com",
                "https://data-seed-prebsc-2-s1.bnbchain.org:8545",
                "https://bsc-testnet.drpc.org",
                "https://data-seed-prebsc-1-s1.binance.org:8545"
        });
        CHAIN_CONFIG_RPC.put(AVALANCHE_ID, new String[]{
                "https://api.avax.network/ext/bc/C/rpc",
                "https://avax.meowrpc.com",
                "https://1rpc.io/avax/c",
                "https://avalanche.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(FANTOM_ID, new String[]{
                "https://rpc.fantom.network/",
                "https://rpcapi.fantom.network",
                "https://rpc3.fantom.network",
                "https://rpc2.fantom.network"
        });
        CHAIN_CONFIG_RPC.put(ARBITRUM_MAIN_ID, new String[]{
                "https://arbitrum.rpc.subquery.network/public",
                "https://rpc.ankr.com/arbitrum",
                "https://arbitrum.meowrpc.com",
                "https://arb1.arbitrum.io/rpc",
                "https://endpoints.omniatech.io/v1/arbitrum/one/public",
                "https://arbitrum-mainnet.infura.io/v3/" + keyProvider.getInfuraKey(), // Assuming keyProvider.getInfuraKey() exists and is accessible
                "https://arbitrum-one.publicnode.com"
        });
        CHAIN_CONFIG_RPC.put(OPTIMISTIC_MAIN_ID, new String[]{
                "https://mainnet.optimism.io",
                "https://optimism-mainnet.public.blastapi.io",
                "https://rpc.optimism.gateway.fm",
                "https://endpoints.omniatech.io/v1/op/mainnet/public",
                "https://optimism-rpc.publicnode.com",
                "https://optimism.gateway.tenderly.co",
                "https://optimism.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(KLAYTN_ID, new String[]{
                "https://public-en.node.kaia.io",
                "https://rpc.ankr.com/klaytn",
                "https://klaytn-pokt.nodies.app",
                "https://1rpc.io/klay"
        });
        CHAIN_CONFIG_RPC.put(KLAYTN_BAOBAB_ID, new String[]{
                "https://public-en.kairos.node.kaia.io",
                "https://rpc.ankr.com/klaytn_testnet",
                "https://kaia-kairos.blockpi.network/v1/rpc/public"
        });
        CHAIN_CONFIG_RPC.put(MANTLE_MAINNET_ID, new String[]{
                "https://rpc.mantle.xyz",
                "https://mantle-mainnet.public.blastapi.io",
                "https://api.zan.top/mantle-mainnet",
                "https://1rpc.io/mantle",
                "https://rpc.owlracle.info/mantle/70d38ce1826c4a60bb2a8e05a6c8b20f"
        });
        CHAIN_CONFIG_RPC.put(MANTLE_TESTNET_ID, new String[]{
                "https://rpc.sepolia.mantle.xyz"
        });
        CHAIN_CONFIG_RPC.put(BASE_MAINNET_ID, new String[]{
                "https://developer-access-mainnet.base.org",
                "https://base.llamarpc.com",
                "https://base.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(BASE_TESTNET_ID, new String[]{
                "https://sepolia.base.org",
                "https://base-sepolia.gateway.tenderly.co",
                "https://base-sepolia-rpc.publicnode.com"
        });
        CHAIN_CONFIG_RPC.put(MILKOMEDA_C1_ID, new String[]{
                "https://rpc-mainnet-cardano-evm.c1.milkomeda.com"
        });
        CHAIN_CONFIG_RPC.put(ROOTSTOCK_MAINNET_ID, new String[]{
                "https://rpc.rootstock.network",
                "https://public-node.rsk.co",
                "https://mycrypto.rsk.co",
                "https://rootstock-mainnet.public.blastapi.io",
                "https://rootstock.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(ROOTSTOCK_TESTNET_ID, new String[]{
                "https://public-node.testnet.rsk.co",
                "https://mycrypto.testnet.rsk.co"
        });
        CHAIN_CONFIG_RPC.put(AURORA_MAINNET_ID, new String[]{
                "https://mainnet.aurora.dev",
                "https://1rpc.io/aurora",
                "https://aurora.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(AURORA_TESTNET_ID, new String[]{
                "https://testnet.aurora.dev",
                "https://aurora-testnet.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(OKX_ID, new String[]{
                "https://1rpc.io/oktc",
                "https://okc.gateway.tenderly.co",
                "https://oktc.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(LINEA_ID, new String[]{
                "https://rpc.linea.build",
                "https://linea.drpc.org",
                "https://rpc.linea.build",
                "https://1rpc.io/linea",
                "https://linea.gateway.tenderly.co"
        });
        CHAIN_CONFIG_RPC.put(LINEA_TEST_ID, new String[]{
                "https://rpc.sepolia.linea.build",
                "https://linea-sepolia-rpc.publicnode.com",
                "https://linea-sepolia.drpc.org",
                "https://1rpc.io/linea-sepolia"
        });
        CHAIN_CONFIG_RPC.put(IOTEX_MAINNET_ID, new String[]{
                "https://babel-api.mainnet.iotex.io",
                "https://iotex-network.rpc.thirdweb.com",
                "https://babel-api.mainnet.iotex.one"
        });
        CHAIN_CONFIG_RPC.put(IOTEX_TESTNET_ID, new String[]{
                "https://babel-api.testnet.iotex.io"
        });
        CHAIN_CONFIG_RPC.put(CLASSIC_ID, new String[]{
                "https://rpc.etcinscribe.com",
                "https://etc.mytokenpocket.vip",
                "https://etc.etcdesktop.com",
                "https://besu-at.etc-network.info",
                "https://0xrpc.io/etc",
                "https://geth-at.etc-network.info"
        });
        CHAIN_CONFIG_RPC.put(MINT_ID, new String[]{
                "https://global.rpc.mintchain.io",
                "https://asia.rpc.mintchain.io",
                "https://asia.rpc.mintchain.io"
        });
        CHAIN_CONFIG_RPC.put(MINT_SEPOLIA_TESTNET_ID, new String[]{
                "https://sepolia-testnet-rpc.mintchain.io"
        });
        CHAIN_CONFIG_RPC.put(GNOSIS_ID, new String[]{
                "https://1rpc.io/gnosis",
                "https://rpc.ankr.com/gnosis",
                "https://gnosis-mainnet.public.blastapi.io",
                "https://endpoints.omniatech.io/v1/gnosis/mainnet/public",
                "https://0xrpc.io/gno",
                "https://rpc.gnosischain.com"
        });
        CHAIN_CONFIG_RPC.put(FUJI_TEST_ID, new String[]{
                "https://api.zan.top/avax-fuji/ext/bc/C/rpc",
                "https://avalanche-fuji.drpc.org",
                "https://ava-testnet.public.blastapi.io/ext/bc/C/rpc",
                "https://avalanche-fuji-c-chain-rpc.publicnode.com",
                "https://endpoints.omniatech.io/v1/avax/fuji/public"
        });
        CHAIN_CONFIG_RPC.put(FANTOM_TEST_ID, new String[]{
                "https://rpc.testnet.fantom.network",
                "https://endpoints.omniatech.io/v1/fantom/testnet/public",
                "https://fantom-testnet-rpc.publicnode.com"
        });
        CHAIN_CONFIG_RPC.put(POLYGON_AMOY_ID, new String[]{
                "https://rpc-amoy.polygon.technology",
                "https://api.zan.top/polygon-amoy",
                "https://polygon-amoy-bor-rpc.publicnode.com",
                "https://polygon-amoy.drpc.org",
                "https://polygon-amoy.gateway.tenderly.co"
        });
        CHAIN_CONFIG_RPC.put(CRONOS_MAIN_ID, new String[]{
                "https://rpc.vvs.finance",
                "https://1rpc.io/cro",
                "https://evm.cronos.org",
                "https://cronos-evm-rpc.publicnode.com",
                "https://cronos.drpc.org"
        });
        CHAIN_CONFIG_RPC.put(CRONOS_TEST_ID, new String[]{
                "https://evm-t3.cronos.org",
                "https://cronos-testnet.drpc.org",
                "https://endpoints.omniatech.io/v1/cronos/testnet/public"
        });
        CHAIN_CONFIG_RPC.put(PALM_ID, new String[]{
                "https://palm-mainnet.public.blastapi.io",
                "https://palm-mainnet.infura.io/v3/" + keyProvider.getInfuraKey(),
                "https://palm-mainnet.gateway.tenderly.co"
        });
        CHAIN_CONFIG_RPC.put(PALM_TEST_ID, new String[]{
                "https://palm-testnet.public.blastapi.io",
                "https://palm-testnet.infura.io/v3/" + keyProvider.getInfuraKey(),
                "https://palm-testnet.gateway.tenderly.co"
        });
        CHAIN_CONFIG_RPC.put(ARBITRUM_TEST_ID, new String[]{
                "https://arbitrum-sepolia.drpc.org",
                "https://api.zan.top/arb-sepolia",
                "https://arbitrum-sepolia.gateway.tenderly.co",
                "https://endpoints.omniatech.io/v1/arbitrum/sepolia/public"
        });
    };

    private static final String INFURA_ENDPOINT = ".infura.io/v3/";

    public static boolean isInfura(String rpcServerUrl)
    {
        return rpcServerUrl.contains(INFURA_ENDPOINT);
    }

    public static boolean isOKX(NetworkInfo networkInfo)
    {
        return networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanAPI) && networkInfo.etherscanAPI.startsWith("https://www.oklink.com");
    }

    // for reset built-in network
    private static final LongSparseArray<NetworkInfo> builtinNetworkMap = new LongSparseArray<NetworkInfo>()
    {
        {
            put(MAINNET_ID, new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MAINNET_ID),
                    "https://cn.etherscan.com/tx/", MAINNET_ID,
                    "https://api-cn.etherscan.com/api?"));
            put(CLASSIC_ID, new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.ETC_SYMBOL,
                    CHAIN_CONFIG_RPC.get(CLASSIC_ID),
                    "https://blockscout.com/etc/mainnet/tx/", CLASSIC_ID,
                    "https://blockscout.com/etc/mainnet/api?"));
            put(GNOSIS_ID, new NetworkInfo(C.XDAI_NETWORK_NAME, C.xDAI_SYMBOL,
                    CHAIN_CONFIG_RPC.get(GNOSIS_ID),
                    "https://gnosis.blockscout.com/tx/", GNOSIS_ID, "https://gnosis.blockscout.com?"));
            put(GOERLI_ID, new NetworkInfo(C.GOERLI_NETWORK_NAME, C.GOERLI_SYMBOL,
                    new String[] {GOERLI_RPC_URL, GOERLI_FALLBACK_RPC_URL},
                    "https://goerli.etherscan.io/tx/", GOERLI_ID,
                    "https://api-goerli.etherscan.io/api?"));
            put(BINANCE_TEST_ID, new NetworkInfo(C.BINANCE_TEST_NETWORK, C.BINANCE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(BINANCE_TEST_ID),
                    "https://testnet.bscscan.com/tx/", BINANCE_TEST_ID,
                    "https://api-testnet.bscscan.com/api?"));
            put(BINANCE_MAIN_ID, new NetworkInfo(C.BINANCE_MAIN_NETWORK, C.BINANCE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(BINANCE_MAIN_ID),
                    "https://bscscan.com/tx/", BINANCE_MAIN_ID,
                    "https://api.bscscan.com/api?"));
            put(AVALANCHE_ID, new NetworkInfo(C.AVALANCHE_NETWORK, C.AVALANCHE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(AVALANCHE_ID),
                    "https://cchain.explorer.avax.network/tx/", AVALANCHE_ID,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(FUJI_TEST_ID, new NetworkInfo(C.FUJI_TEST_NETWORK, C.AVALANCHE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(FUJI_TEST_ID),
                    "https://cchain.explorer.avax-test.network/tx/", FUJI_TEST_ID,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(FANTOM_ID, new NetworkInfo(C.FANTOM_NETWORK, C.FANTOM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(FANTOM_ID),
                    "https://ftmscan.com/tx/", FANTOM_ID,
                    "https://api.ftmscan.com/api?"));
            put(FANTOM_TEST_ID, new NetworkInfo(C.FANTOM_TEST_NETWORK, C.FANTOM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(FANTOM_TEST_ID),
                    "https://explorer.testnet.fantom.network/tx/", FANTOM_TEST_ID,
                    "https://api.covalenthq.com/v1/" + COVALENT)); //NB: Fantom testnet not yet supported by Covalent
            put(POLYGON_ID, new NetworkInfo(C.POLYGON_NETWORK, C.POLYGON_SYMBOL,
                    CHAIN_CONFIG_RPC.get(POLYGON_ID),
                    "https://polygonscan.com/tx/", POLYGON_ID,
                    "https://api.polygonscan.com/api?"));
            put(POLYGON_TEST_ID, new NetworkInfo(C.POLYGON_TEST_NETWORK, C.POLYGON_SYMBOL,
                    new String[] {FREE_MUMBAI_RPC_URL},
                    "https://mumbai.polygonscan.com/tx/", POLYGON_TEST_ID,
                    "https://api-testnet.polygonscan.com/api?"));
            put(POLYGON_AMOY_ID, new NetworkInfo(C.AMOY_TESTNET_NAME, C.AMOY_TESTNET_SYMBOL,
                    CHAIN_CONFIG_RPC.get(POLYGON_AMOY_ID),
                    "https://amoy.polygonscan.com/tx/", POLYGON_AMOY_ID,
                    "https://api-amoy.polygonscan.com/api?"));
            put(OPTIMISTIC_MAIN_ID, new NetworkInfo(C.OPTIMISTIC_NETWORK, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(OPTIMISTIC_MAIN_ID),
                    "https://optimistic.etherscan.io/tx/", OPTIMISTIC_MAIN_ID,
                    "https://api-optimistic.etherscan.io/api?"));
            put(CRONOS_MAIN_ID, new NetworkInfo(C.CRONOS_MAIN_NETWORK, C.CRONOS_SYMBOL,
                    CHAIN_CONFIG_RPC.get(CRONOS_MAIN_ID),
                    "https://cronos.org/explorer/tx/", CRONOS_MAIN_ID,
                    "https://cronos.org/explorer/api?"));
            put(CRONOS_TEST_ID, new NetworkInfo(C.CRONOS_TEST_NETWORK, C.CRONOS_TEST_SYMBOL,
                    CHAIN_CONFIG_RPC.get(CRONOS_TEST_ID),
                    "https://explorer.cronos.org/testnet/tx/", CRONOS_TEST_ID,
                    "https://testnet.cronoscan.com/api?"));
            put(ARBITRUM_MAIN_ID, new NetworkInfo(C.ARBITRUM_ONE_NETWORK, C.ARBITRUM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(ARBITRUM_MAIN_ID),
                    "https://arbiscan.io/tx/", ARBITRUM_MAIN_ID,
                    "https://api.arbiscan.io/api?"));
            put(PALM_ID, new NetworkInfo(C.PALM_NAME, C.PALM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(PALM_ID),
                    "https://explorer.palm.io/tx/", PALM_ID,
                    "https://explorer.palm.io/api?"));
            put(PALM_TEST_ID, new NetworkInfo(C.PALM_TEST_NAME, C.PALM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(PALM_TEST_ID),
                    "https://explorer.palm-uat.xyz/tx/", PALM_TEST_ID,
                    "https://explorer.palm-uat.xyz/api?"));
            put(KLAYTN_ID, new NetworkInfo(C.KLAYTN_NAME, C.KLAYTN_SYMBOL,
                    CHAIN_CONFIG_RPC.get(KLAYTN_ID),
                "https://kaiascan.io/tx/", KLAYTN_ID,
                ""));
            put(KLAYTN_BAOBAB_ID, new NetworkInfo(C.KLAYTN_BAOBAB_NAME, C.KLAYTN_SYMBOL,
                    CHAIN_CONFIG_RPC.get(KLAYTN_BAOBAB_ID),
                "https://kairos.kaiascan.io/tx/", KLAYTN_BAOBAB_ID,
                ""));
            put(IOTEX_MAINNET_ID, new NetworkInfo(C.IOTEX_NAME, C.IOTEX_SYMBOL,
                    CHAIN_CONFIG_RPC.get(IOTEX_MAINNET_ID),
                    "https://iotexscan.io/tx/", IOTEX_MAINNET_ID,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(IOTEX_TESTNET_ID, new NetworkInfo(C.IOTEX_TESTNET_NAME, C.IOTEX_SYMBOL,
                    CHAIN_CONFIG_RPC.get(IOTEX_TESTNET_ID),
                    "https://testnet.iotexscan.io/tx/", IOTEX_TESTNET_ID,
                    "https://api.covalenthq.com/v1/" + COVALENT));
            put(AURORA_MAINNET_ID, new NetworkInfo(C.AURORA_MAINNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(AURORA_MAINNET_ID),
                    "https://aurorascan.dev/tx/", AURORA_MAINNET_ID,
                    "https://api.aurorascan.dev/api?"));
            put(AURORA_TESTNET_ID, new NetworkInfo(C.AURORA_TESTNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(AURORA_TESTNET_ID),
                    "https://testnet.aurorascan.dev/tx/", AURORA_TESTNET_ID,
                    "https://api-testnet.aurorascan.dev/api?"));
            put(MILKOMEDA_C1_ID, new NetworkInfo(C.MILKOMEDA_NAME, C.MILKOMEDA_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MILKOMEDA_C1_ID),
                    "https://explorer-mainnet-cardano-evm.c1.milkomeda.com/tx/", MILKOMEDA_C1_ID,
                    "https://explorer-mainnet-cardano-evm.c1.milkomeda.com/api?"));
            put(SEPOLIA_TESTNET_ID, new NetworkInfo(C.SEPOLIA_TESTNET_NAME, C.SEPOLIA_SYMBOL,
                    CHAIN_CONFIG_RPC.get(SEPOLIA_TESTNET_ID),
                    "https://sepolia.etherscan.io/tx/", SEPOLIA_TESTNET_ID,
                    "https://api-sepolia.etherscan.io/api?"));
            put(ARBITRUM_TEST_ID, new NetworkInfo(C.ARBITRUM_TESTNET_NAME, C.ARBITRUM_SYMBOL,
                    CHAIN_CONFIG_RPC.get(ARBITRUM_TEST_ID),
                    "https://testnet.arbiscan.io/tx/", ARBITRUM_TEST_ID,
                    "https://api-goerli.arbiscan.io/api?"));
            put(OKX_ID, new NetworkInfo(C.OKX_NETWORK_NAME, C.OKX_SYMBOL,
                    CHAIN_CONFIG_RPC.get(OKX_ID),
                "https://www.oklink.com/en/okc/tx/", OKX_ID,
                "https://www.oklink.com/api"));

            put(ROOTSTOCK_MAINNET_ID, new NetworkInfo(C.ROOTSTOCK_NETWORK_NAME, C.ROOTSTOCK_SYMBOL,
                    CHAIN_CONFIG_RPC.get(ROOTSTOCK_MAINNET_ID),
                    "https://blockscout.com/rsk/mainnet/tx/", ROOTSTOCK_MAINNET_ID,
                    "https://blockscout.com/rsk/mainnet/api?"));
            put(ROOTSTOCK_TESTNET_ID, new NetworkInfo(C.ROOTSTOCK_TESTNET_NAME, C.ROOTSTOCK_TEST_SYMBOL,
                    CHAIN_CONFIG_RPC.get(ROOTSTOCK_TESTNET_ID),
                    "", ROOTSTOCK_TESTNET_ID,
                    ""));
            put(LINEA_ID, new NetworkInfo(C.LINEA_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(LINEA_ID),
                    "https://lineascan.build/tx/", LINEA_ID,
                    "https://api.lineascan.build/api?"));
            put(LINEA_TEST_ID, new NetworkInfo(C.LINEA_TESTNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(LINEA_TEST_ID),
                    "https://sepolia.lineascan.build/tx/", LINEA_TEST_ID,
                    "https://api-sepolia.lineascan.build/api?"));
            put(HOLESKY_ID, new NetworkInfo(C.HOLESKY_TESTNET_NAME, C.HOLESKY_TEST_SYMBOL,
                    CHAIN_CONFIG_RPC.get(HOLESKY_ID),
                    "https://holesky.etherscan.io/tx/", HOLESKY_ID,
                    "https://api-holesky.etherscan.io/api?"));
            put(BASE_MAINNET_ID, new NetworkInfo(C.BASE_MAINNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(BASE_MAINNET_ID),
                    "https://basescan.org/tx/", BASE_MAINNET_ID,
                    "https://api.basescan.org/api?"));
            put(BASE_TESTNET_ID, new NetworkInfo(C.BASE_TESTNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(BASE_TESTNET_ID),
                    "https://sepolia.basescan.org/tx/", BASE_TESTNET_ID,
                    "https://api-sepolia.basescan.org/api?"));

            put(MANTLE_MAINNET_ID, new NetworkInfo(C.MANTLE_MAINNET_NAME, C.MANTLE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MANTLE_MAINNET_ID),
                    "https://explorer.mantle.xyz/tx/", MANTLE_MAINNET_ID,
                    "https://explorer.mantle.xyz/api?"));

            put(MANTLE_TESTNET_ID, new NetworkInfo(C.MANTLE_TESTNET_NAME, C.MANTLE_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MANTLE_TESTNET_ID),
                    "https://explorer.sepolia.mantle.xyz/tx/", MANTLE_TESTNET_ID,
                    "https://explorer.sepolia.mantle.xyz/api?"));

            // Add deprecated networks after this line
            put(MINT_ID, new NetworkInfo(C.MINT_MAINNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MINT_ID),
                    "https://explorer.mintchain.io/tx/", MINT_ID,
                    "https://explorer.mintchain.io/api/v2/"));

            put(MINT_SEPOLIA_TESTNET_ID, new NetworkInfo(C.MINT_TESTNET_NAME, C.ETH_SYMBOL,
                    CHAIN_CONFIG_RPC.get(MINT_SEPOLIA_TESTNET_ID),
                    "https://sepolia-testnet-explorer.mintchain.io/tx/", MINT_SEPOLIA_TESTNET_ID,
                    "https://sepolia-testnet-explorer.mintchain.io/api/v2/")); //https://sepolia-testnet-explorer.mintchain.io/api/v2/addresses/0x76626Fc07d050d59c9fc1Ac5b853a9952B5E9Afe/transactions?filter=to%20%7C%20from
        }
    };

    //List of network details. Note, the advantage of using LongSparseArray is efficiency and also
    //the entries are automatically sorted into numerical order
    private static final LongSparseArray<NetworkInfo> networkMap = builtinNetworkMap.clone();

    private static final LongSparseArray<Integer> chainLogos = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.drawable.ic_token_eth);
            put(CLASSIC_ID, R.drawable.ic_icons_network_etc); //classic_logo
            put(GNOSIS_ID, R.drawable.ic_icons_network_gnosis);
            put(GOERLI_ID, R.drawable.ic_goerli);
            put(BINANCE_MAIN_ID, R.drawable.ic_binance_logo);
            put(BINANCE_TEST_ID, R.drawable.ic_icons_tokens_bnb_testnet);
            put(FANTOM_ID, R.drawable.ic_fantom);
            put(FANTOM_TEST_ID, R.drawable.ic_icons_fantom_test);
            put(AVALANCHE_ID, R.drawable.ic_icons_tokens_avalanche);
            put(FUJI_TEST_ID, R.drawable.ic_icons_tokens_avalanche_testnet);
            put(POLYGON_ID, R.drawable.ic_icons_polygon);
            put(POLYGON_AMOY_ID, R.drawable.ic_icons_tokens_mumbai);
            put(OPTIMISTIC_MAIN_ID, R.drawable.ic_optimism_logo);
            put(CRONOS_MAIN_ID, R.drawable.ic_cronos_mainnet);
            put(CRONOS_TEST_ID, R.drawable.ic_cronos);
            put(ARBITRUM_MAIN_ID, R.drawable.ic_icons_arbitrum);
            put(PALM_ID, R.drawable.ic_icons_network_palm);
            put(PALM_TEST_ID, R.drawable.palm_logo_test);
            put(KLAYTN_ID, R.drawable.ic_icons_kaia);
            put(KLAYTN_BAOBAB_ID, R.drawable.ic_icons_kaia_test);
            put(IOTEX_MAINNET_ID, R.drawable.ic_iotex);
            put(IOTEX_TESTNET_ID, R.drawable.ic_iotex_test);
            put(AURORA_MAINNET_ID, R.drawable.ic_aurora);
            put(AURORA_TESTNET_ID, R.drawable.ic_aurora_test);
            put(MILKOMEDA_C1_ID, R.drawable.ic_milkomeda);
            put(SEPOLIA_TESTNET_ID, R.drawable.ic_sepolia_test);
            put(ARBITRUM_TEST_ID, R.drawable.ic_icons_arbitrum_test);
            put(OKX_ID, R.drawable.ic_okx);
            put(ROOTSTOCK_MAINNET_ID, R.drawable.ic_rootstock_logo);
            put(ROOTSTOCK_TESTNET_ID, R.drawable.ic_rootstock_test_logo);
            put(LINEA_ID, R.drawable.ic_icons_linea);
            put(LINEA_TEST_ID, R.drawable.ic_icons_linea_testnet);
            put(HOLESKY_ID, R.drawable.ic_icons_holesky);
            put(POLYGON_TEST_ID, R.drawable.ic_icons_tokens_mumbai);
            put(BASE_MAINNET_ID, R.drawable.ic_base_logo);
            put(BASE_TESTNET_ID, R.drawable.ic_base_test_logo);
            put(MANTLE_MAINNET_ID, R.drawable.ic_mantle_logo);
            put(MANTLE_TESTNET_ID, R.drawable.ic_mantle_test_logo);
            put(MINT_ID, R.drawable.ic_mint_logo);
            put(MINT_SEPOLIA_TESTNET_ID, R.drawable.ic_mint_test_logo);
        }
    };

    private static final LongSparseArray<Integer> smallChainLogos = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.drawable.ic_icons_network_eth);
            put(CLASSIC_ID, R.drawable.ic_icons_network_etc);
            put(GNOSIS_ID, R.drawable.ic_icons_network_gnosis);
            put(GOERLI_ID, R.drawable.ic_goerli);
            put(BINANCE_MAIN_ID, R.drawable.ic_icons_network_bsc);
            put(BINANCE_TEST_ID, R.drawable.ic_icons_tokens_bnb_testnet);
            put(FANTOM_ID, R.drawable.ic_icons_network_fantom);
            put(FANTOM_TEST_ID, R.drawable.ic_icons_fantom_test);
            put(AVALANCHE_ID, R.drawable.ic_icons_network_avalanche);
            put(FUJI_TEST_ID, R.drawable.ic_icons_tokens_avalanche_testnet);
            put(POLYGON_ID, R.drawable.ic_icons_network_polygon);
            put(POLYGON_AMOY_ID, R.drawable.ic_icons_tokens_mumbai);
            put(POLYGON_TEST_ID, R.drawable.ic_icons_tokens_mumbai);
            put(OPTIMISTIC_MAIN_ID, R.drawable.ic_icons_network_optimism);
            put(CRONOS_MAIN_ID, R.drawable.ic_cronos_mainnet);
            put(CRONOS_TEST_ID, R.drawable.ic_cronos);
            put(ARBITRUM_MAIN_ID, R.drawable.ic_icons_network_arbitrum);
            put(PALM_ID, R.drawable.ic_icons_network_palm);
            put(PALM_TEST_ID, R.drawable.palm_logo_test);
            put(KLAYTN_ID, R.drawable.ic_icons_kaia);
            put(KLAYTN_BAOBAB_ID, R.drawable.ic_icons_kaia_test);
            put(IOTEX_MAINNET_ID, R.drawable.ic_iotex);
            put(IOTEX_TESTNET_ID, R.drawable.ic_iotex_test);
            put(AURORA_MAINNET_ID, R.drawable.ic_aurora);
            put(AURORA_TESTNET_ID, R.drawable.ic_aurora_test);
            put(MILKOMEDA_C1_ID, R.drawable.ic_milkomeda);
            put(SEPOLIA_TESTNET_ID, R.drawable.ic_sepolia_test);
            put(ARBITRUM_TEST_ID, R.drawable.ic_icons_arbitrum_test);
            put(OKX_ID, R.drawable.ic_okx);
            put(ROOTSTOCK_MAINNET_ID, R.drawable.ic_rootstock_logo);
            put(ROOTSTOCK_TESTNET_ID, R.drawable.ic_rootstock_test_logo);
            put(LINEA_ID, R.drawable.ic_icons_linea);
            put(LINEA_TEST_ID, R.drawable.ic_icons_linea_testnet);
            put(HOLESKY_ID, R.drawable.ic_icons_holesky);
            put(BASE_MAINNET_ID, R.drawable.ic_base_logo);
            put(BASE_TESTNET_ID, R.drawable.ic_base_test_logo);
            put(MANTLE_MAINNET_ID, R.drawable.ic_mantle_logo);
            put(MANTLE_TESTNET_ID, R.drawable.ic_mantle_test_logo);
            put(MINT_ID, R.drawable.ic_mint_logo);
            put(MINT_SEPOLIA_TESTNET_ID, R.drawable.ic_mint_test_logo);
        }
    };

    private static final LongSparseArray<Integer> chainColours = new LongSparseArray<Integer>()
    {
        {
            put(MAINNET_ID, R.color.mainnet);
            put(CLASSIC_ID, R.color.classic);
            put(GNOSIS_ID, R.color.xdai);
            put(GOERLI_ID, R.color.goerli);
            put(BINANCE_MAIN_ID, R.color.binance_main);
            put(BINANCE_TEST_ID, R.color.binance_test);
            put(FANTOM_ID, R.color.fantom_main);
            put(FANTOM_TEST_ID, R.color.fantom_test);
            put(AVALANCHE_ID, R.color.avalanche_main);
            put(FUJI_TEST_ID, R.color.avalanche_test);
            put(POLYGON_ID, R.color.polygon_main);
            put(POLYGON_TEST_ID, R.color.polygon_test);
            put(POLYGON_AMOY_ID, R.color.polygon_test);
            put(OPTIMISTIC_MAIN_ID, R.color.optimistic_main);
            put(CRONOS_MAIN_ID, R.color.cronos_main);
            put(CRONOS_TEST_ID, R.color.cronos_test);
            put(ARBITRUM_MAIN_ID, R.color.arbitrum_main);
            put(PALM_ID, R.color.palm_main);
            put(PALM_TEST_ID, R.color.palm_test);
            put(KLAYTN_ID, R.color.klaytn_main);
            put(KLAYTN_BAOBAB_ID, R.color.klaytn_test);
            put(IOTEX_MAINNET_ID, R.color.iotex_mainnet);
            put(IOTEX_TESTNET_ID, R.color.iotex_mainnet);
            put(AURORA_MAINNET_ID, R.color.aurora_mainnet);
            put(AURORA_TESTNET_ID, R.color.aurora_testnet);
            put(MILKOMEDA_C1_ID, R.color.milkomeda);
            put(SEPOLIA_TESTNET_ID, R.color.sepolia);
            put(ARBITRUM_TEST_ID, R.color.arbitrum_test);
            put(OKX_ID, R.color.okx);
            put(ROOTSTOCK_MAINNET_ID, R.color.rootstock);
            put(ROOTSTOCK_TESTNET_ID, R.color.rootstock);
            put(LINEA_ID, R.color.black);
            put(LINEA_TEST_ID, R.color.pinkish_grey);
            put(HOLESKY_ID, R.color.azure);
            put(BASE_MAINNET_ID, R.color.base_logo);
            put(BASE_TESTNET_ID, R.color.base_logo);
            put(MANTLE_MAINNET_ID, R.color.rootstock);
            put(MANTLE_TESTNET_ID, R.color.rootstock);
            put(MINT_ID, R.color.mint_chain);
            put(MINT_SEPOLIA_TESTNET_ID, R.color.mint_chain);
        }
    };

    //Does the chain have a gas oracle?
    //Add it to this list here if so. Note that so far, all gas oracles follow the same format:
    //  <etherscanAPI from the above list> + GAS_API
    //If the gas oracle you're adding doesn't follow this spec then you'll have to change the getGasOracle method
    private static final List<Long> hasGasOracleAPI = Arrays.asList(MAINNET_ID, POLYGON_ID, ARBITRUM_MAIN_ID, AVALANCHE_ID, BINANCE_MAIN_ID, CRONOS_MAIN_ID, GOERLI_ID,
            SEPOLIA_TESTNET_ID, FANTOM_ID, LINEA_ID, OPTIMISTIC_MAIN_ID, POLYGON_TEST_ID, POLYGON_AMOY_ID, BASE_MAINNET_ID, BASE_TESTNET_ID);
    private static final List<Long> hasEtherscanGasOracleAPI = Arrays.asList(MAINNET_ID, BINANCE_MAIN_ID, POLYGON_ID);
    private static final List<Long> hasBlockNativeGasOracleAPI = Arrays.asList(MAINNET_ID, POLYGON_ID);
    //These chains don't allow custom gas
    private static final List<Long> hasLockedGas = Arrays.asList(KLAYTN_ID, KLAYTN_BAOBAB_ID);
    private static final List<Long> hasOpenSeaAPI = Arrays.asList(MAINNET_ID, POLYGON_ID, ARBITRUM_TEST_ID, AVALANCHE_ID, KLAYTN_ID, GOERLI_ID);

    private static final LongSparseArray<BigInteger> blockGasLimit = new LongSparseArray<BigInteger>()
    {
        {
            put(MAINNET_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX));
            put(KLAYTN_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX_KLAYTN));
            put(AURORA_MAINNET_ID, BigInteger.valueOf(C.GAS_LIMIT_MAX_AURORA));
        }
    };

    public static String getEtherscanGasOracle(long chainId)
    {
        if (hasEtherscanGasOracleAPI.contains(chainId) && networkMap.indexOfKey(chainId) >= 0)
        {
            return networkMap.get(chainId).etherscanAPI + GAS_API;
        }
        else
        {
            return "";
        }
    }

    public static String getGasOracle(long chainId)
    {
        if (hasGasOracleAPI.contains(chainId) && networkMap.indexOfKey(chainId) >= 0)
        {
            //construct API route:
            return INFURA_GAS_API.replace("CHAIN_ID", Long.toString(chainId));
        }
        else
        {
            return "";
        }
    }

    private static final String BLOCKNATIVE_GAS_API = "https://api.blocknative.com/gasprices/blockprices?chainid=";

    public static String getBlockNativeOracle(long chainId)
    {
        if (hasBlockNativeGasOracleAPI.contains(chainId) && networkMap.indexOfKey(chainId) >= 0)
        {
            return BLOCKNATIVE_GAS_API + chainId;
        }
        else
        {
            return "";
        }
    }

    /**
     * This function determines the order in which chains appear in the main wallet view
     *
     * TODO: Modify so that custom networks with value appear between the 'hasValue' and 'testnetList' chains
     *
     * @param chainId
     * @return
     */
    public static int getChainOrdinal(long chainId)
    {
        if (hasValue.contains(chainId))
        {
            return hasValue.indexOf(chainId);
        }
        else if (testnetList.contains(chainId))
        {
            return hasValue.size() + testnetList.indexOf(chainId);
        }
        else
        {
            return hasValue.size() + testnetList.size() + (int) chainId % 500;
        }
    }

    public static final int INFURA_BATCH_LIMIT = 512;
    public static final String INFURA_DOMAIN = "infura.io";

    public static void setBatchProcessingError(long chainId)
    {
        batchProcessingLimitMap.put(chainId, 0);
    }

    //TODO: Refactor when we bump the version of java to allow switch on Long (Finally!!)
    //Also TODO: add a test to check these batch limits of each chain we support
    private static int batchProcessingLimit(long chainId)
    {
        //for now return don't batch
        return 0;
        /*
        NetworkInfo info = builtinNetworkMap.get(chainId);
        if (info.rpcServerUrl.contains(INFURA_DOMAIN)) //infura supported chains can handle tx batches of 1000 and up
        {
            return INFURA_BATCH_LIMIT;
        }
        else if (info.rpcServerUrl.contains("klaytn") || info.rpcServerUrl.contains("rpc.ankr.com"))
        {
            return 0;
        }
        else if (chainId == GNOSIS_ID)
        {
            return 6; //TODO: Check limit:
        }
        else if (info.rpcServerUrl.contains("cronos.org"))
        {
            return 5; //TODO: Check limit
        }
        else
        {
            return 32;
        }
         */
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
        {
            return batchProcessingLimitMap.get(chainId, 0); //default to zero / no batching
        }
    }

    @Override
    public boolean hasLockedGas(long chainId)
    {
        return hasLockedGas.contains(chainId);
    }

    @Override
    public boolean hasBlockNativeGasAPI(long chainId)
    {
        return hasBlockNativeGasOracleAPI.contains(chainId);
    }

    static final Map<Long, String> addressOverride = new HashMap<Long, String>()
    {
        {
            put(OPTIMISTIC_MAIN_ID, "0x4200000000000000000000000000000000000006");
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

        private String preProcessNetworks(String networks)
        {
            if (networks != null && !networks.isEmpty())
            {
                try
                {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<CustomNetworks>()
                    {
                    }.getType();
                    CustomNetworks customNetworks = gson.fromJson(networks, listType);

                    boolean converted = false;
                    if (customNetworks.list != null)
                    {
                        for (NetworkInfo networkInfo : customNetworks.list)
                        {
                            if (networkInfo.rpcServerUrl != null && (networkInfo.rpcUrls == null || networkInfo.rpcUrls.length == 0))
                            {
                                networkInfo.rpcUrls = new String[]{networkInfo.rpcServerUrl};
                                converted = true;
                                break; // Moved break inside the loop
                            }
                        }
                        if (converted)
                        {
                            String updatedNetworks = gson.toJson(customNetworks);
                            preferences.setCustomRPCNetworks(updatedNetworks);
                            networks = updatedNetworks; // Reinstate this line
                        }
                    }
                }
                catch (Exception e)
                {
                    System.err.println("Error processing custom networks: " + e.getMessage());
                }
            }
            return networks;
        }

        public void restore()
        {
            String networks = preferences.getCustomRPCNetworks();

            networks = preProcessNetworks(networks);

            if (!TextUtils.isEmpty(networks))
            {
                CustomNetworks cn = new Gson().fromJson(networks, CustomNetworks.class);
                this.list = cn.list;
                this.mapToTestNet = cn.mapToTestNet;
                if (list == null)
                {
                    return;
                }

                checkCustomNetworkSetting();

                for (NetworkInfo info : list)
                {
                    boolean containsBadUrl = false;
                    for (String thisUrl : info.rpcUrls)
                    {
                        if (!isValidUrl(thisUrl)) //ensure RPC doesn't contain malicious code
                        {
                            containsBadUrl = true;
                            break;
                        }
                    }

                    if (containsBadUrl)
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
            if (!list.isEmpty() && !list.get(0).isCustom)
            { //need to update the list
                List<NetworkInfo> copyList = new ArrayList<>(list);
                list.clear();
                for (NetworkInfo n : copyList)
                {
                    boolean isCustom = builtinNetworkMap.indexOfKey(n.chainId) == -1;
                    NetworkInfo newInfo = new NetworkInfo(n.name, n.symbol, new String[] {n.rpcServerUrl}, n.etherscanUrl, n.chainId, n.etherscanAPI, isCustom);
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
                    && !result.contains(network))
            {
                result.add(network);
            }
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
        return addressOverride.getOrDefault(chainId, "");
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

    // Static variant to replace static in the other EthereumNetworkBase
    public static NetworkInfo getNetwork(long chainId)
    {
        return networkMap.get(chainId);
    }

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
        return getSelectedFilters();
    }

    @Override
    public List<Long> getSelectedFilters()
    {
        String filterList = preferences.getNetworkFilterList();
        List<Long> storedIds = Utils.longListToArray(filterList);
        List<Long> selectedIds = new ArrayList<>();

        for (Long networkId : storedIds)
        {
            NetworkInfo check = networkMap.get(networkId);
            if (check != null) selectedIds.add(networkId);
        }

        if (selectedIds.isEmpty())
        {
            selectedIds.add(getDefaultNetwork());
        }

        return selectedIds;
    }

    @Override
    public Long getDefaultNetwork()
    {
        return CustomViewSettings.primaryChain;
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
        addNetworks(allNetworks, networks, true);
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
            Random random = new Random();
            return info.rpcUrls[random.nextInt(info.rpcUrls.length)];
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
     * @param chainId
     * @return
     */
    public static String getDefaultNodeURL(long chainId)
    {
        NetworkInfo info = networkMap.get(chainId);
        Random r = new Random();
        return info.rpcUrls[r.nextInt(info.rpcUrls.length)];
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

    public void saveCustomRPCNetwork(String networkName, String rpcUrl, long chainId, String symbol, String blockExplorerUrl, String explorerApiUrl, boolean isTestnet, Long oldChainId)
    {
        NetworkInfo builtInNetwork = builtinNetworkMap.get(chainId);
        boolean isCustom = builtInNetwork == null;
        //make new entry the first in the list
        String[] networks = builtInNetwork != null ? builtInNetwork.rpcUrls : new String[] {};
        if (isCustom)
        {
            //add new entry to the front of the list
            String[] newNetworks = new String[networks.length + 1];
            newNetworks[0] = rpcUrl;
            System.arraycopy(networks, 0, newNetworks, 1, networks.length);
            networks = newNetworks;
        }
        NetworkInfo info = new NetworkInfo(networkName, symbol, networks, blockExplorerUrl, chainId, explorerApiUrl, isCustom);
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
            // Unsupported network: method caller should handle this scenario
            return "";
        }
    }

    public static boolean isChainSupported(long chainId)
    {
        return networkMap.get(chainId) != null;
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

    public static boolean isEventBlockLimitEnforced(long chainId)
    {
        return chainId == POLYGON_ID || chainId == POLYGON_TEST_ID || chainId == POLYGON_AMOY_ID;
    }

    public static BigInteger getMaxEventFetch(long chainId)
    {
        if (chainId == POLYGON_ID || chainId == POLYGON_TEST_ID)
        {
            return BigInteger.valueOf(POLYGON_BLOCK_SEARCH_INTERVAL);
        }
        else if (chainId == OKX_ID)
        {
            return BigInteger.valueOf(OKX_BLOCK_SEARCH_INTERVAL);
        }
        else
        {
            return BigInteger.valueOf(BLOCK_SEARCH_INTERVAL);
        }
    }

    public static String getNodeURLForEvents(long chainId)
    {
        if (chainId == POLYGON_ID)
        {
            return EthereumNetworkBase.FREE_POLYGON_RPC_URL; // Better than Infura for fetching events
        }
        else if (chainId == POLYGON_TEST_ID)
        {
            return EthereumNetworkBase.MUMBAI_FALLBACK_RPC_URL;
        }
        else
        {
            return getNodeURLByNetworkId(chainId);
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

    @Override
    public void commitPrefs()
    {
        preferences.commit();
    }

    public static List<Long> getAllNetworks()
    {
        ArrayList<Long> list = new ArrayList<>();
        list.addAll(hasValue);
        list.addAll(testnetList);
        return list;
    }
}
