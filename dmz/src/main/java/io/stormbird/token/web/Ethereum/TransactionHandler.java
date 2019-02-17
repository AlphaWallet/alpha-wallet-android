package io.stormbird.token.web.Ethereum;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.stormbird.token.entity.BadContract;
import okhttp3.OkHttpClient;

public class TransactionHandler
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

    private static Web3j mWeb3;

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
            default:
                return ETHEREUM_NETWORK;
        }
    }

    private String getNodeURLByNetworkId(int networkId) {
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
            default:
                return MAINNET_NETWORK_ID;
        }
    }

    public TransactionHandler(int networkId)
    {
        String nodeURL = getNodeURLByNetworkId(networkId);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(20, TimeUnit.SECONDS);
        builder.readTimeout(20, TimeUnit.SECONDS);

        HttpService service = new HttpService(nodeURL, builder.build(), false);
        mWeb3 = Web3j.build(service);
        try
        {
            Web3ClientVersion web3ClientVersion = mWeb3.web3ClientVersion().sendAsync().get();
            System.out.println(web3ClientVersion.getWeb3ClientVersion());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public List<BigInteger> getBalanceArray(String address, String contractAddress) throws Exception
    {
        List<BigInteger> result = new ArrayList<>();
        org.web3j.abi.datatypes.Function function = balanceOfArray(address);
        List<Uint256> indices = callSmartContractFunctionArray(function, contractAddress, address);
        if (indices == null) throw new BadContract();
        for (Uint256 val : indices)
        {
            result.add(val.getValue());
        }
        return result;
    }

    public String getName(String address)
    {
        String name = "";
        String symbol = "";
        try
        {
            name = getContractData(address, stringParam("name"));
            symbol = getContractData(address, stringParam("symbol"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return name + " (" + symbol + ")";
    }

    private <T> T getContractData(String address, org.web3j.abi.datatypes.Function function) throws Exception
    {
        String responseValue = callSmartContractFunction(function, address);

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1)
        {
            return (T) response.get(0).getValue();
        }
        else
        {
            return null;
        }
    }

    private String callSmartContractFunction(
            Function function, String contractAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        return makeEthCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(null, contractAddress, encodedFunction));
    }

    private String makeEthCall(Transaction transaction) throws ExecutionException, InterruptedException
    {
        org.web3j.protocol.core.methods.response.EthCall ethCall = mWeb3.ethCall(transaction,
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();
        return ethCall.getValue();
    }

    private List callSmartContractFunctionArray(
            org.web3j.abi.datatypes.Function function, String contractAddress, String address) throws Exception
    {
        String encodedFunction = FunctionEncoder.encode(function);
        String value = makeEthCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(address, contractAddress, encodedFunction));

        List<Type> values = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        if (values.isEmpty()) return null;

        Type T = values.get(0);
        Object o = T.getValue();
        return (List) o;
    }

    private static org.web3j.abi.datatypes.Function stringParam(String param) {
        return new Function(param,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
    }

    private static org.web3j.abi.datatypes.Function balanceOfArray(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }
}
