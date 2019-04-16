package io.stormbird.token.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 2/03/2019.
 * Stormbird in Singapore
 */
public class MagicLinkInfo
{
    //node urls
    private static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String CLASSIC_RPC_URL = "https://web3.gastracker.io";
    private static final String XDAI_RPC_URL = "https://dai.poa.network";
    private static final String POA_RPC_URL = "https://core.poa.network/";
    private static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    private static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/da3717f25f824cc1baa32d812386d93f";

    //domains for DMZ
    private static final String mainnetMagicLinkDomain = "aw.app";
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
    private static final String customMagicLinkDomain = "custom.aw.app";

    //network ids
    private static final int LEGACY_VALUE = 0;
    private static final int MAINNET_NETWORK_ID = 1;
    private static final int CLASSIC_NETWORK_ID = 61;
    private static final int KOVAN_NETWORK_ID = 42;
    private static final int ROPSTEN_NETWORK_ID = 3;
    private static final int RINKEBY_NETWORK_ID = 4;
    private static final int POA_NETWORK_ID = 99;
    private static final int SOKOL_NETWORK_ID = 77;
    private static final int XDAI_NETWORK_ID = 100;
    private static final int GOERLI_NETWORK_ID = 5;

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
            default:
                return ETHEREUM_NETWORK;
        }
    }

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
            default:
                return MAINNET_RPC_URL;
        }
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId) {
        switch (networkId) {
            case LEGACY_VALUE:
                return legacyMagicLinkDomain;
            case MAINNET_NETWORK_ID:
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
            default:
                return mainnetMagicLinkDomain;
        }
    }

    //For testing you will not have the correct domain (localhost)
    //To test, alter the else statement to return the network you wish to test
    public static int getNetworkIdFromDomain(String domain) {
        switch(domain) {
            case mainnetMagicLinkDomain:
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
            default:
                return MAINNET_NETWORK_ID;
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

    public static String formPaymasterURLPrefixFromDomain(String domain)
    {
        return "https://" + domain + ":80/api/";
    }
}
