package com.alphawallet.token.web.Ethereum;

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

import com.alphawallet.token.entity.BadContract;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.web.Service.EthRPCNodes;

import okhttp3.OkHttpClient;

public class TransactionHandler
{
    private static Web3j mWeb3;

    public TransactionHandler(long networkId)
    {
        String nodeURL = EthRPCNodes.getNodeURLByNetworkId(networkId);
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

    public String getNameOnly(String address)
    {
        String name = "";
        try
        {
            name = callSmartContractAndGetResult(address, stringParam("name"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return name;
    }

    public String getSymbolOnly(String address)
    {
        String symbol = "";
        try
        {
            symbol = callSmartContractAndGetResult(address, stringParam("symbol"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return symbol;
    }

    public String getName(String address)
    {
        String name = "";
        try
        {
            name = callSmartContractAndGetResult(address, stringParam("name"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return name;
    }

    public String getOwnerOf721(String address, BigInteger tokenId) {
        String owner = "";
        try
        {
            owner = Numeric.toHexStringWithPrefix(callSmartContractAndGetResult(address, ownerOf721(tokenId)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return owner;
    }

    public List<BigInteger> getBalanceArray721Tickets(String owner, String contractAddress) {
        List<BigInteger> castBalances = new ArrayList<>();
        try
        {
            List<Uint256> balances = callSmartContractAndGetResult(contractAddress, getBalances721TicketToken(owner));
            for(Uint256 token: balances) {
                castBalances.add(token.getValue());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return castBalances;
    }

    private <T> T callSmartContractAndGetResult(String address, org.web3j.abi.datatypes.Function function) throws Exception
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

    private static org.web3j.abi.datatypes.Function getBalances721TicketToken(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "getBalances",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private static org.web3j.abi.datatypes.Function ownerOf721(BigInteger token) {
        return new org.web3j.abi.datatypes.Function(
                "ownerOf",
                Collections.singletonList(new Uint256(token)),
                Collections.singletonList(new TypeReference<Address>() {}));
    }

}
