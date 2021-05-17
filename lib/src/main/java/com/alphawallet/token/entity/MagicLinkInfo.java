package com.alphawallet.token.entity;

import com.alphawallet.ethereum.EthereumNetworkBase;

import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_SIGMA1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.GOERLI_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KOVAN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MUMBAI_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.POA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROPSTEN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SOKOL_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_ID;

/**
 * Created by James on 2/03/2019.
 * Stormbird in Singapore
 */
public class MagicLinkInfo
{
    //domains for DMZ
    public static final String mainnetMagicLinkDomain = "aw.app";
    private static final String legacyMagicLinkDomain = "app.awallet.io";
    private static final String classicMagicLinkDomain = "classic.aw.app";
    private static final String callistoMagicLinkDomain = "callisto.aw.app";
    private static final String kovanMagicLinkDomain = "kovan.aw.app";
    private static final String ropstenMagicLinkDomain = "ropsten.aw.app";
    private static final String rinkebyMagicLinkDomain = "rinkeby.aw.app";
    private static final String poaMagicLinkDomain = "poa.aw.app";
    private static final String sokolMagicLinkDomain = "sokol.aw.app";
    private static final String xDaiMagicLinkDomain = "xdai.aw.app";
    private static final String goerliMagicLinkDomain = "goerli.aw.app";
    private static final String artisSigma1MagicLinkDomain = "artissigma1.aw.app";
    private static final String artisTau1MagicLinkDomain = "artistau1.aw.app";
    private static final String customMagicLinkDomain = "custom.aw.app";

    //Etherscan domains
    private static final String mainNetEtherscan = "https://cn.etherscan.com/";
    private static final String classicEtherscan = "https://blockscout.com/etc/mainnet/";
    private static final String callistoEtherscan = "https://etherscan.io/"; //TODO: determine callisto etherscan
    private static final String kovanEtherscan = "https://kovan.etherscan.io/";
    private static final String ropstenEtherscan = "https://ropsten.etherscan.io/";
    private static final String rinkebyEtherscan = "https://rinkeby.etherscan.io/";
    private static final String poaEtherscan = "https://blockscout.com/poa/core/";
    private static final String sokolEtherscan = "https://blockscout.com/poa/sokol/";
    private static final String xDaiEtherscan = "https://blockscout.com/poa/dai/";
    private static final String goerliEtherscan = "https://goerli.etherscan.io/";
    private static final String artisSigma1Etherscan = "https://explorer.sigma1.artis.network/";
    private static final String artisTau1Etherscan = "https://explorer.tau1.artis.network/";


    //network names
    private static final String ETHEREUM_NETWORK = "Ethereum";
    private static final String CLASSIC_NETWORK = "Ethereum Classic";
    private static final String KOVAN_NETWORK = "Kovan";
    private static final String ROPSTEN_NETWORK = "Ropsten";
    private static final String RINKEBY_NETWORK = "Rinkeby";
    private static final String POA_NETWORK = "POA";
    private static final String SOKOL_NETWORK = "Sokol";
    private static final String XDAI_NETWORK = "xDAI";
    private static final String GOERLI_NETWORK = "Görli";
    private static final String ARTIS_SIGMA1_NETWORK = "ARTIS sigma1";
    private static final String ARTIS_TAU1_NETWORK = "ARTIS tau1";
    private static final String BINANCE_TEST_NETWORK = "BSC TestNet";
    private static final String BINANCE_MAIN_NETWORK = "Binance";
    private static final String HECO_MAIN_NETWORK = "Heco";
    private static final String HECO_TEST_NETWORK = "Heco (Test)";
    private static final String FANTOM_NETWORK = "Fantom Opera";
    private static final String FANTOM_TEST_NETWORK = "Fantom (Test)";
    private static final String AVALANCHE_NETWORK = "Avalanche";
    private static final String FUJI_TEST_NETWORK = "Avalanche FUJI (Test)";
    private static final String MATIC_NETWORK = "Polygon";
    private static final String MATIC_TEST_NETWORK = "Mumbai (Test)";

    public static String getNetworkNameById(int networkId) {
        switch (networkId) {
            case EthereumNetworkBase.MAINNET_ID:
                return ETHEREUM_NETWORK;
            case EthereumNetworkBase.KOVAN_ID:
                return KOVAN_NETWORK;
            case EthereumNetworkBase.ROPSTEN_ID:
                return ROPSTEN_NETWORK;
            case EthereumNetworkBase.RINKEBY_ID:
                return RINKEBY_NETWORK;
            case EthereumNetworkBase.POA_ID:
                return POA_NETWORK;
            case EthereumNetworkBase.SOKOL_ID:
                return SOKOL_NETWORK;
            case EthereumNetworkBase.CLASSIC_ID:
                return CLASSIC_NETWORK;
            case EthereumNetworkBase.XDAI_ID:
                return XDAI_NETWORK;
            case EthereumNetworkBase.GOERLI_ID:
                return GOERLI_NETWORK;
            case EthereumNetworkBase.ARTIS_SIGMA1_ID:
                return ARTIS_SIGMA1_NETWORK;
            case EthereumNetworkBase.ARTIS_TAU1_ID:
                return ARTIS_TAU1_NETWORK;
            case BINANCE_MAIN_ID:
                return BINANCE_MAIN_NETWORK;
            case BINANCE_TEST_ID:
                return BINANCE_TEST_NETWORK;
            case HECO_ID:
                return HECO_MAIN_NETWORK;
            case HECO_TEST_ID:
                return HECO_TEST_NETWORK;
            case FANTOM_ID:
                return FANTOM_NETWORK;
            case FANTOM_TEST_ID:
                return FANTOM_TEST_NETWORK;
            case AVALANCHE_ID:
                return AVALANCHE_NETWORK;
            case FUJI_TEST_ID:
                return FUJI_TEST_NETWORK;
            case MATIC_ID:
                return MATIC_NETWORK;
            case MATIC_TEST_ID:
                return MATIC_TEST_NETWORK;
            default:
                return ETHEREUM_NETWORK;
        }
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId) {
        switch (networkId) {
            case 0:
                return legacyMagicLinkDomain;
            case MAINNET_ID:
            default:
                return mainnetMagicLinkDomain;
            case KOVAN_ID:
                return kovanMagicLinkDomain;
            case ROPSTEN_ID:
                return ropstenMagicLinkDomain;
            case RINKEBY_ID:
                return rinkebyMagicLinkDomain;
            case POA_ID:
                return poaMagicLinkDomain;
            case SOKOL_ID:
                return sokolMagicLinkDomain;
            case CLASSIC_ID:
                return classicMagicLinkDomain;
            case XDAI_ID:
                return xDaiMagicLinkDomain;
            case GOERLI_ID:
                return goerliMagicLinkDomain;
            case ARTIS_SIGMA1_ID:
                return artisSigma1MagicLinkDomain;
            case ARTIS_TAU1_ID:
                return artisTau1MagicLinkDomain;
        }
    }

    //For testing you will not have the correct domain (localhost)
    //To test, alter the else statement to return the network you wish to test
    public static int getNetworkIdFromDomain(String domain) {
        switch(domain) {
            case mainnetMagicLinkDomain:
            default:
                return MAINNET_ID;
            case legacyMagicLinkDomain:
                return MAINNET_ID;
            case classicMagicLinkDomain:
                return CLASSIC_ID;
            case kovanMagicLinkDomain:
                return KOVAN_ID;
            case ropstenMagicLinkDomain:
                return ROPSTEN_ID;
            case rinkebyMagicLinkDomain:
                return RINKEBY_ID;
            case poaMagicLinkDomain:
                return POA_ID;
            case sokolMagicLinkDomain:
                return SOKOL_ID;
            case xDaiMagicLinkDomain:
                return XDAI_ID;
            case goerliMagicLinkDomain:
                return GOERLI_ID;
            case artisSigma1MagicLinkDomain:
                return ARTIS_SIGMA1_ID;
            case artisTau1MagicLinkDomain:
                return ARTIS_TAU1_ID;
        }
    }

    public static String getEtherscanURLbyNetwork(int networkId) {
        switch (networkId) {
            case MAINNET_ID:
            default:
                return mainNetEtherscan;
            case KOVAN_ID:
                return kovanEtherscan;
            case ROPSTEN_ID:
                return ropstenEtherscan;
            case RINKEBY_ID:
                return rinkebyEtherscan;
            case POA_ID:
                return poaEtherscan;
            case SOKOL_ID:
                return sokolEtherscan;
            case CLASSIC_ID:
                return classicEtherscan;
            case XDAI_ID:
                return xDaiEtherscan;
            case GOERLI_ID:
                return goerliEtherscan;
            case ARTIS_SIGMA1_ID:
                return artisSigma1Etherscan;
            case ARTIS_TAU1_ID:
                return artisTau1Etherscan;
        }
    }

    public static int identifyChainId(String link)
    {
        if (link == null || link.length() == 0) return 0;

        int chainId = 0;
        //split out the chainId from the magiclink
        int index = link.indexOf(mainnetMagicLinkDomain);
        int dSlash = link.indexOf("://");
        int legacy = link.indexOf(legacyMagicLinkDomain);
        //try new style link
        if (index > 0 && dSlash > 0)
        {
            String domain = link.substring(dSlash+3, index + mainnetMagicLinkDomain.length());
            chainId = getNetworkIdFromDomain(domain);
        }
        else if (legacy > 0)
        {
            chainId = 0;
        }

        return chainId;
    }

    public static String generatePrefix(int chainId)
    {
        return "https://" + getMagicLinkDomainFromNetworkId(chainId) + "/";
    }
}
