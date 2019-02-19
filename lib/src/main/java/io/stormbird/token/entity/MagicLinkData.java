package io.stormbird.token.entity;


import io.stormbird.token.tools.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MagicLinkData
{
    public long expiry;
    public byte[] prefix;
    public BigInteger nonce;
    public double price;
    public BigInteger priceWei;
    public List<BigInteger> tokenIds;
    public int[] tickets;
    public BigInteger amount;
    public int ticketStart;
    public int ticketCount;
    public String contractAddress;
    public byte[] signature = new byte[65];
    public byte[] message;
    public String ownerAddress;
    public String contractName;
    public byte contractType;

    public List<BigInteger> balanceInfo = null;

    //node urls
    private static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String CLASSIC_RPC_URL = "https://web3.gastracker.io";
    private static final String XDAI_RPC_URL = "https://dai.poa.network";
    private static final String POA_RPC_URL = "https://core.poa.network/";
    private static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String SOKOL_RPC_URL = "https://sokol.poa.network";

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
    private static final String customMagicLinkDomain = "custom.aw.app";

    //network ids
    private static final int MAINNET_NETWORK_ID = 1;
    private static final int CLASSIC_NETWORK_ID = 61;
    private static final int KOVAN_NETWORK_ID = 42;
    private static final int ROPSTEN_NETWORK_ID = 3;
    private static final int RINKEBY_NETWORK_ID = 4;
    private static final int POA_NETWORK_ID = 99;
    private static final int SOKOL_NETWORK_ID = 77;
    private static final int XDAI_NETWORK_ID = 100;

    //network names
    private static final String ETHEREUM_NETWORK = "Ethereum";
    private static final String CLASSIC_NETWORK = "Ethereum Classic";
    private static final String KOVAN_NETWORK = "Kovan";
    private static final String ROPSTEN_NETWORK = "Ropsten";
    private static final String RINKEBY_NETWORK = "Rinkeby";
    private static final String POA_NETWORK = "POA";
    private static final String SOKOL_NETWORK = "Sokol";
    private static final String XDAI_NETWORK = "xDAI";

    public String getNetworkNameById(int networkId) {
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
            default:
                return ETHEREUM_NETWORK;
        }
    }

    public String getNodeURLByNetworkId(int networkId) {
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
            default:
                return MAINNET_RPC_URL;
        }
    }

    public String getMagicLinkDomainFromNetworkId(int networkId) {
        switch (networkId) {
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
            default:
                return mainnetMagicLinkDomain;
        }
    }

    //For testing you will not have the correct domain (localhost)
    //To test, alter the else statement to return the network you wish to test
    public int getNetworkIdFromDomain(String domain) {
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
            default:
                return MAINNET_NETWORK_ID;
        }
    }

    public String formMagicLinkURLPrefixFromDomain(String domain)
    {
        return "https://" + domain + "/";
    }

    public String formPaymasterURLPrefixFromDomain(String domain)
    {
        return "https://" + domain + ":80/api/";
    }

    public boolean isValidOrder()
    {
        //check this order is not corrupt
        //first check the owner address - we should already have called getOwnerKey
        boolean isValid = true;

        if (this.ownerAddress == null || this.ownerAddress.length() < 20) isValid = false;
        if (this.contractAddress == null || this.contractAddress.length() < 20) isValid = false;
        if (this.message == null) isValid = false;

        return isValid;
    }

    public boolean balanceChange(List<BigInteger> balance)
    {
        //compare two balances
        //quick return, if sizes are different there's a change
        if (balanceInfo == null)
        {
            balanceInfo = new ArrayList<>(); //initialise the balance list
            return true;
        }
        if (balance.size() != balanceInfo.size()) return true;

        List<BigInteger> oldBalance = new ArrayList<>(balanceInfo);
        List<BigInteger> newBalance = new ArrayList<>(balance);

        oldBalance.removeAll(balanceInfo);
        newBalance.removeAll(balance);

        return (oldBalance.size() != 0 || newBalance.size() != 0);
    }

}
