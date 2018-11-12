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
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.stormbird.token.entity.BadContract;
import okhttp3.OkHttpClient;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TransactionHandler
{
    private static final String CONTRACT_ADDR = "0x6ae0e6d98955ba13dacf654c4819e6a1886e978f";
    private static final String MAIN_NODEURL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String ROPSTEN_NODEURL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    private static final String RINKEBY_NODEURL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";

    private static Web3j mWeb3;

    public TransactionHandler()
    {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(20, TimeUnit.SECONDS);
        builder.readTimeout(20, TimeUnit.SECONDS);

        HttpService service = new HttpService(MAIN_NODEURL, builder.build(), false);
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

        org.web3j.protocol.core.methods.request.Transaction transaction
                = createEthCallTransaction(null, contractAddress, encodedFunction);
        EthCall response = mWeb3.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        return response.getValue();
    }

    private List callSmartContractFunctionArray(
            org.web3j.abi.datatypes.Function function, String contractAddress, String address) throws Exception
    {
        String encodedFunction = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.response.EthCall ethCall = mWeb3.ethCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(address, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        String value = ethCall.getValue();
        List<Type> values = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        if (values.isEmpty()) return null;

        Type T = values.get(0);
        Object o = T.getValue();
        return (List) o;
    }

    private static org.web3j.abi.datatypes.Function stringParam(String param) {
        return new Function(param,
                            Arrays.<Type>asList(),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static org.web3j.abi.datatypes.Function balanceOfArray(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }
}
