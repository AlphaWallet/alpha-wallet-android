package io.stormbird.token.web.Ethereum;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.stormbird.token.entity.BadContract;
import rx.schedulers.Schedulers;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TransactionHandler
{
    private static final String CONTRACT_ADDR = "0x6ae0e6d98955ba13dacf654c4819e6a1886e978f";
    private static final String MAIN_NODEURL = "http://54.222.132.228:8545";
    private static final String ROPSTEN_NODEURL = "http://66.96.208.58:8545";
    private static final String RINKEBY_NODEURL = "http://66.96.208.58:8541";

    private static Web3j mWeb3;

    public TransactionHandler()
    {
        mWeb3 = Web3j.build(new HttpService(RINKEBY_NODEURL));

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
        byte[] temp = new byte[16];
        org.web3j.abi.datatypes.Function function = balanceOfArray(address);
        List<Bytes32> indices = callSmartContractFunctionArray(function, contractAddress, address);
        if (indices == null) throw new BadContract();
        for (Bytes32 val : indices)
        {
            result.add(getCorrectedValue(val, temp));
        }
        return result;
    }

    private void convertToBytes32(List gList, String address, String selection)

    {
        List<Bytes32> list = (List<Bytes32>) gList;
        receiveBalance(list, address, selection);
    }

    //result from the queryBalance observable
    private void receiveBalance(List<Bytes32> list, String address, String selection)
    {
        List<BigInteger> resultList = new ArrayList<>();
        byte[] buffer = new byte[16];

        //receive a list of ticket indexes that belong to this address
        for (Bytes32 index : list)
        {
            resultList.add(correctValue(index, buffer));
        }

        sendBalanceResult(resultList, address, selection);
    }

    private void sendBalanceResult(List<BigInteger> resultList, String address, String selection)
    {

    }

    /**
     * checking if we need to read a top 16 byte value specifically
     * We should keep this function in here because when we start to use 32 byte values there is
     * potentially a problem with the 'always move 16 bytes to low 16' force solution.
     * <p>
     * A better solution is not to fight this ethereum feature - we simply start interpreting the XML from
     * the top byte.
     */
    private BigInteger correctValue(Bytes32 val, byte[] temp)
    {
        BigInteger retVal;
        //does the top second byte have a value and the lower 16 bytes are zero?
        long lowCheck = 0;
        long highCheck = val.getValue()[0] + val.getValue()[1];
        for (int i = 16; i < 32; i++) lowCheck += val.getValue()[i];
        if (highCheck != 0 && lowCheck == 0)
        {
            System.arraycopy(val.getValue(), 0, temp, 0, 16);
            retVal = Numeric.toBigInt(temp);
        }
        else
        {
            retVal = Numeric.toBigInt(val.getValue());
        }

        return retVal;
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

    private void onError(Throwable throwable)
    {

    }

    private void handleName(String name, String address)
    {
        int zeroIndex = name.indexOf('\0');
        if (zeroIndex > 0)
        {
            name = name.substring(0, zeroIndex);
        }


    }

    private static org.web3j.abi.datatypes.Function stringParam(String param) {
        return new Function(param,
                            Arrays.<Type>asList(),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private void handleHasExpired(Boolean res, String address)
    {
        System.out.println("Has " + address + " Expired? : " + (res ? " YES " : " NO"));
    }

    private static org.web3j.abi.datatypes.Function balanceOfArray(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Bytes32>>() {}));
    }

    /**
     * checking if we need to read a top 16 byte value specifically
     * We should keep this function in here because when we start to use 32 byte values there is
     * potentially a problem with the 'always move 16 bytes to low 16' force solution.
     *
     * A better solution is not to fight this ethereum feature - we simply start interpreting the XML from
     * the top byte.
     */
    private BigInteger getCorrectedValue(Bytes32 val, byte[] temp)
    {
        BigInteger retVal;
        //does the top second byte have a value and the lower 16 bytes are zero?
        long lowCheck = 0;
        long highCheck = val.getValue()[0] + val.getValue()[1];
        for (int i = 16; i < 32; i++) lowCheck += val.getValue()[i];
        if (highCheck != 0 && lowCheck == 0)
        {
            System.arraycopy(val.getValue(), 0, temp, 0, 16);
            retVal = Numeric.toBigInt(temp);
        }
        else
        {
            retVal = Numeric.toBigInt(val.getValue());
        }

        return retVal;
    }



}
