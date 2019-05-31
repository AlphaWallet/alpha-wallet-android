package io.stormbird.token.entity;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.stormbird.token.tools.TokenDefinition;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

/**
 * Created by James on 10/11/2018.
 * Stormbird in Singapore
 */

public class FunctionDefinition
{
    public ContractInfo contract;
    public String method;
    public TokenDefinition.Syntax syntax;
    public TokenDefinition.As as;
    public List<MethodArg> parameters = new ArrayList<>();

    public String result;
    public long resultTime = 0;
    public BigInteger tokenId;
    public EthereumTransaction tx;

    public Function generateTransactionFunction(String walletAddr, BigInteger tokenId, TokenDefinition definition)
    {
        List<Type> params = new ArrayList<Type>();
        List<TypeReference<?>> returnTypes = new ArrayList<TypeReference<?>>();
        for (MethodArg arg : parameters)
        {
            resolveReference(arg, tokenId, definition);
            switch (arg.parameterType)
            {
                case "uint256":
                    switch (arg.element.ref)
                    {
                        case "tokenId":
                            params.add(new Uint256(tokenId));
                            break;
                        case "value":
                        default:
                            params.add(new Uint256(new BigInteger(arg.element.value)));
                            break;
                    }
                    break;
                case "address":
                    switch (arg.element.ref)
                    {
                        case "ownerAddress":
                            params.add(new Address(walletAddr));
                            break;
                        case "value":
                        default:
                            params.add(new Address(arg.element.value));
                            break;
                    }
                    break;
                default:
                    System.out.println("NOT IMPLEMENTED: " + arg.parameterType);
                    break;
            }
        }
        switch (syntax)
        {
            case Boolean:
            case Integer:
            case NumericString:
                returnTypes.add(new TypeReference<Uint256>()
                {
                });
                break;
            case IA5String:
            case DirectoryString:
                returnTypes.add(new TypeReference<Utf8String>()
                {
                });
                break;
        }

        return new Function(method,
                                         params, returnTypes);
    }

    private void resolveReference(MethodArg arg, BigInteger tokenId, TokenDefinition definition)
    {
        if (definition != null && definition.attributeTypes.containsKey(arg.element.ref))
        {
            arg.element.value = definition.fetchAttrResult(arg.element.ref, tokenId, null, definition.context.attrInterface).blockingSingle().text;
        }
    }

    private void handleTransactionResult(TransactionResult result, Function function, String responseValue)
    {
        try
        {
            long currentTime = System.currentTimeMillis();
            //try to interpret the value
            List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
            if (response.size() > 0)
            {
                result.resultTime = currentTime;
                Type val = response.get(0);
                switch (syntax)
                {
                    case Boolean:
                        BigDecimal value = new BigDecimal(((Uint256) val).getValue());
                        result.result = value.equals(BigDecimal.ZERO) ? "FALSE" : "TRUE";
                        break;
                    case Integer:
                    case NumericString:
                        result.result = new BigDecimal(((Uint256) val).getValue()).toString();
                        break;
                    case IA5String:
                    case DirectoryString:
                        result.result = (String) response.get(0).getValue();
                        if (responseValue.length() > 2 && result.result.length() == 0)
                        {
                            result.result = checkBytesString(responseValue);
                        }
                        break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String checkBytesString(String responseValue) throws Exception
    {
        String name = "";
        if (responseValue.length() > 0)
        {
            //try raw bytes
            byte[] data = Numeric.hexStringToByteArray(responseValue);
            //check leading bytes for non-zero
            if (data[0] != 0)
            {
                //truncate zeros
                int index = data.length - 1;
                while (data[index] == 0 && index > 0)
                    index--;
                if (index != (data.length - 1))
                {
                    data = Arrays.copyOfRange(data, 0, index + 1);
                }
                name = new String(data, "UTF-8");
            }
        }

        return name;
    }

    public TokenScriptResult.Attribute parseFunctionResult(TransactionResult transactionResult, AttributeType attr)
    {
        String res = transactionResult.result;
        BigInteger val = transactionResult.tokenId;
        if (attr.syntax == TokenDefinition.Syntax.NumericString)
        {
            if (transactionResult.result == null)
            {
                res = "0";
            }
            else if (transactionResult.result.startsWith("0x"))
            {
                res = res.substring(2);
            }
            val = new BigInteger(res, 16);
        }
        return new TokenScriptResult.Attribute(attr.id, attr.name, val, res);
    }

    public static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    /**
     * Haven't pre-cached this value yet, so need to fetch it before we can proceed
     * @param override
     * @param attr
     * @param tokenId
     * @param definition
     * @return
     */
    public Observable<TransactionResult> fetchResultFromEthereum(ContractAddress override, AttributeType attr, BigInteger tokenId, TokenDefinition definition)
    {
        return Observable.fromCallable(() -> {
            ContractAddress useAddress;
            if (override == null) // contract not specified - is not holder contract
            {
                //determine address using definition context
                useAddress = new ContractAddress(this, definition.context.cAddr.chainId, definition.context.cAddr.address);
            }
            else
            {
                useAddress = override;
            }
            TransactionResult transactionResult = new TransactionResult(useAddress.chainId, useAddress.address, tokenId, attr);
            // 1: create transaction call
            org.web3j.abi.datatypes.Function transaction = generateTransactionFunction(ZERO_ADDRESS, tokenId, definition);
            // 2: create web3 connection
            OkHttpClient okClient = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(false)
                        .build();

            HttpService nodeService = new HttpService(MagicLinkInfo.getNodeURLByNetworkId(useAddress.chainId), okClient, false);

            Web3j web3j = Web3j.build(nodeService);

            //now push the transaction
            String result = callSmartContractFunction(web3j, transaction, useAddress.address, ZERO_ADDRESS);

            attr.function.handleTransactionResult(transactionResult, transaction, result);
            return transactionResult;
        });
    }

    private String callSmartContractFunction(Web3j web3j,
            Function function, String contractAddress, String walletAddr) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(walletAddr, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (IOException e)
        {
            //Connection error. Use cached value
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}