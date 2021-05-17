package com.alphawallet.token.entity;

import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.ethereum.NetworkInfo;

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

    public static String getNetworkNameById(int networkId) {
        NetworkInfo info = EthereumNetworkBase.getNetworkByChain(networkId);
        if (info != null)
        {
            return info.name;
        }
        else
        {
            return EthereumNetworkBase.getNetworkByChain(MAINNET_ID).name;
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

    //TODO: Refactor to use the centralised source
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
