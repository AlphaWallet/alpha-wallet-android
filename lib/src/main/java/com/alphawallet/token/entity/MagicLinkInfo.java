package com.alphawallet.token.entity;

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
    private static final String velasMagicLinkDomain = "velas.aw.app";
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
    private static final String velasTestEtherscan = "https://xtn.yopta.net/";
    private static final String velasMainEtherscan = "https://explorer.velas.com/";

    //network ids
    public static final int LEGACY_VALUE = 0;
    public static final int MAINNET_NETWORK_ID = 1;
    public static final int CLASSIC_NETWORK_ID = 61;
    public static final int KOVAN_NETWORK_ID = 42;
    public static final int ROPSTEN_NETWORK_ID = 3;
    public static final int RINKEBY_NETWORK_ID = 4;
    public static final int POA_NETWORK_ID = 99;
    public static final int SOKOL_NETWORK_ID = 77;
    public static final int XDAI_NETWORK_ID = 100;
    public static final int GOERLI_NETWORK_ID = 5;
    public static final int ARTIS_SIGMA1_NETWORK_ID = 246529;
    public static final int ARTIS_TAU1_NETWORK_ID = 246785;
    public static final int VELAS_TEST_NETWORK_ID = 111;
    public static final int VELAS_MAIN_NETWORK_ID = 106;

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
    private static final String VELAS_TEST_NETWORK = "VELA (Test)";
    private static final String VELAS_MAIN_NETWORK = "VELAS";

    public static String getNetworkNameById(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
                return ETHEREUM_NETWORK;
            case KOVAN_NETWORK_ID:
                return KOVAN_NETWORK;
            case ROPSTEN_NETWORK_ID:
                return ROPSTEN_NETWORK;
            case RINKEBY_NETWORK_ID:
                return RINKEBY_NETWORK;
            case POA_NETWORK_ID:
                return POA_NETWORK;
            case SOKOL_NETWORK_ID:
                return SOKOL_NETWORK;
            case CLASSIC_NETWORK_ID:
                return CLASSIC_NETWORK;
            case XDAI_NETWORK_ID:
                return XDAI_NETWORK;
            case GOERLI_NETWORK_ID:
                return GOERLI_NETWORK;
            case ARTIS_SIGMA1_NETWORK_ID:
                return ARTIS_SIGMA1_NETWORK;
            case ARTIS_TAU1_NETWORK_ID:
                return ARTIS_TAU1_NETWORK;
            case VELAS_TEST_NETWORK_ID:
                return VELAS_TEST_NETWORK;
            case VELAS_MAIN_NETWORK_ID:
                return VELAS_MAIN_NETWORK;
            default:
                return ETHEREUM_NETWORK;
        }
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId) {
        switch (networkId) {
            case LEGACY_VALUE:
                return legacyMagicLinkDomain;
            case MAINNET_NETWORK_ID:
            default:
                return mainnetMagicLinkDomain;
            case KOVAN_NETWORK_ID:
                return kovanMagicLinkDomain;
            case ROPSTEN_NETWORK_ID:
                return ropstenMagicLinkDomain;
            case RINKEBY_NETWORK_ID:
                return rinkebyMagicLinkDomain;
            case POA_NETWORK_ID:
                return poaMagicLinkDomain;
            case SOKOL_NETWORK_ID:
                return sokolMagicLinkDomain;
            case CLASSIC_NETWORK_ID:
                return classicMagicLinkDomain;
            case XDAI_NETWORK_ID:
                return xDaiMagicLinkDomain;
            case GOERLI_NETWORK_ID:
                return goerliMagicLinkDomain;
            case ARTIS_SIGMA1_NETWORK_ID:
                return artisSigma1MagicLinkDomain;
            case ARTIS_TAU1_NETWORK_ID:
                return artisTau1MagicLinkDomain;
            case VELAS_TEST_NETWORK_ID:
            case VELAS_MAIN_NETWORK_ID:
                return velasMagicLinkDomain;
        }
    }

    //For testing you will not have the correct domain (localhost)
    //To test, alter the else statement to return the network you wish to test
    public static int getNetworkIdFromDomain(String domain) {
        switch(domain) {
            case mainnetMagicLinkDomain:
            default:
                return MAINNET_NETWORK_ID;
            case legacyMagicLinkDomain:
                return MAINNET_NETWORK_ID;
            case classicMagicLinkDomain:
                return CLASSIC_NETWORK_ID;
            case kovanMagicLinkDomain:
                return KOVAN_NETWORK_ID;
            case ropstenMagicLinkDomain:
                return ROPSTEN_NETWORK_ID;
            case rinkebyMagicLinkDomain:
                return RINKEBY_NETWORK_ID;
            case poaMagicLinkDomain:
                return POA_NETWORK_ID;
            case sokolMagicLinkDomain:
                return SOKOL_NETWORK_ID;
            case xDaiMagicLinkDomain:
                return XDAI_NETWORK_ID;
            case goerliMagicLinkDomain:
                return GOERLI_NETWORK_ID;
            case artisSigma1MagicLinkDomain:
                return ARTIS_SIGMA1_NETWORK_ID;
            case artisTau1MagicLinkDomain:
                return ARTIS_TAU1_NETWORK_ID;
            case velasMagicLinkDomain:
                return VELAS_MAIN_NETWORK_ID;
        }
    }

    public static String getEtherscanURLbyNetwork(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
            default:
                return mainNetEtherscan;
            case KOVAN_NETWORK_ID:
                return kovanEtherscan;
            case ROPSTEN_NETWORK_ID:
                return ropstenEtherscan;
            case RINKEBY_NETWORK_ID:
                return rinkebyEtherscan;
            case POA_NETWORK_ID:
                return poaEtherscan;
            case SOKOL_NETWORK_ID:
                return sokolEtherscan;
            case CLASSIC_NETWORK_ID:
                return classicEtherscan;
            case XDAI_NETWORK_ID:
                return xDaiEtherscan;
            case GOERLI_NETWORK_ID:
                return goerliEtherscan;
            case ARTIS_SIGMA1_NETWORK_ID:
                return artisSigma1Etherscan;
            case ARTIS_TAU1_NETWORK_ID:
                return artisTau1Etherscan;
            case VELAS_TEST_NETWORK_ID:
                return velasTestEtherscan;
            case VELAS_MAIN_NETWORK_ID:
                return velasMainEtherscan;
        }
    }

    public static int identifyChainId(String link)
    {
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
