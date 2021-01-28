package com.alphawallet.scripttool.Entity;


import io.reactivex.Observable;
import com.alphawallet.token.entity.*;
import com.alphawallet.token.tools.TokenDefinition;
import okhttp3.OkHttpClient;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Int;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes1;
import org.web3j.abi.datatypes.generated.Bytes10;
import org.web3j.abi.datatypes.generated.Bytes11;
import org.web3j.abi.datatypes.generated.Bytes12;
import org.web3j.abi.datatypes.generated.Bytes13;
import org.web3j.abi.datatypes.generated.Bytes14;
import org.web3j.abi.datatypes.generated.Bytes15;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes17;
import org.web3j.abi.datatypes.generated.Bytes18;
import org.web3j.abi.datatypes.generated.Bytes19;
import org.web3j.abi.datatypes.generated.Bytes2;
import org.web3j.abi.datatypes.generated.Bytes20;
import org.web3j.abi.datatypes.generated.Bytes21;
import org.web3j.abi.datatypes.generated.Bytes22;
import org.web3j.abi.datatypes.generated.Bytes23;
import org.web3j.abi.datatypes.generated.Bytes24;
import org.web3j.abi.datatypes.generated.Bytes25;
import org.web3j.abi.datatypes.generated.Bytes26;
import org.web3j.abi.datatypes.generated.Bytes27;
import org.web3j.abi.datatypes.generated.Bytes28;
import org.web3j.abi.datatypes.generated.Bytes29;
import org.web3j.abi.datatypes.generated.Bytes3;
import org.web3j.abi.datatypes.generated.Bytes30;
import org.web3j.abi.datatypes.generated.Bytes31;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Bytes5;
import org.web3j.abi.datatypes.generated.Bytes6;
import org.web3j.abi.datatypes.generated.Bytes7;
import org.web3j.abi.datatypes.generated.Bytes8;
import org.web3j.abi.datatypes.generated.Bytes9;
import org.web3j.abi.datatypes.generated.Int104;
import org.web3j.abi.datatypes.generated.Int112;
import org.web3j.abi.datatypes.generated.Int120;
import org.web3j.abi.datatypes.generated.Int128;
import org.web3j.abi.datatypes.generated.Int136;
import org.web3j.abi.datatypes.generated.Int144;
import org.web3j.abi.datatypes.generated.Int152;
import org.web3j.abi.datatypes.generated.Int16;
import org.web3j.abi.datatypes.generated.Int160;
import org.web3j.abi.datatypes.generated.Int168;
import org.web3j.abi.datatypes.generated.Int176;
import org.web3j.abi.datatypes.generated.Int184;
import org.web3j.abi.datatypes.generated.Int192;
import org.web3j.abi.datatypes.generated.Int200;
import org.web3j.abi.datatypes.generated.Int208;
import org.web3j.abi.datatypes.generated.Int216;
import org.web3j.abi.datatypes.generated.Int224;
import org.web3j.abi.datatypes.generated.Int232;
import org.web3j.abi.datatypes.generated.Int24;
import org.web3j.abi.datatypes.generated.Int240;
import org.web3j.abi.datatypes.generated.Int248;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Int32;
import org.web3j.abi.datatypes.generated.Int40;
import org.web3j.abi.datatypes.generated.Int48;
import org.web3j.abi.datatypes.generated.Int56;
import org.web3j.abi.datatypes.generated.Int64;
import org.web3j.abi.datatypes.generated.Int72;
import org.web3j.abi.datatypes.generated.Int8;
import org.web3j.abi.datatypes.generated.Int80;
import org.web3j.abi.datatypes.generated.Int88;
import org.web3j.abi.datatypes.generated.Int96;
import org.web3j.abi.datatypes.generated.Uint104;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint120;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint136;
import org.web3j.abi.datatypes.generated.Uint144;
import org.web3j.abi.datatypes.generated.Uint152;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.abi.datatypes.generated.Uint168;
import org.web3j.abi.datatypes.generated.Uint176;
import org.web3j.abi.datatypes.generated.Uint184;
import org.web3j.abi.datatypes.generated.Uint192;
import org.web3j.abi.datatypes.generated.Uint200;
import org.web3j.abi.datatypes.generated.Uint208;
import org.web3j.abi.datatypes.generated.Uint216;
import org.web3j.abi.datatypes.generated.Uint224;
import org.web3j.abi.datatypes.generated.Uint232;
import org.web3j.abi.datatypes.generated.Uint24;
import org.web3j.abi.datatypes.generated.Uint240;
import org.web3j.abi.datatypes.generated.Uint248;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint40;
import org.web3j.abi.datatypes.generated.Uint48;
import org.web3j.abi.datatypes.generated.Uint56;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint72;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.generated.Uint80;
import org.web3j.abi.datatypes.generated.Uint88;
import org.web3j.abi.datatypes.generated.Uint96;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

/**
 * Created by James on 13/06/2019.
 * Stormbird in Sydney
 */
public abstract class TokenscriptFunction
{
    public static final String TOKENSCRIPT_CONVERSION_ERROR = "<error>";

    private final Map<String, Attribute> localAttrs = new ConcurrentHashMap<>();
    private final Map<String, String> refTags = new ConcurrentHashMap<>();

    public Function generateTransactionFunction(String walletAddr, BigInteger tokenId, TokenDefinition definition, FunctionDefinition function, AttributeInterface attrIf)
    {
        boolean valueNotFound = false;
        //pre-parse tokenId.
        if (tokenId.bitCount() > 256) tokenId = tokenId.or(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)); //truncate tokenId too large

        List<Type> params = new ArrayList<Type>();
        List<TypeReference<?>> returnTypes = new ArrayList<TypeReference<?>>();
        for (MethodArg arg : function.parameters)
        {
            String value = resolveReference(walletAddr, arg.element, tokenId, definition, attrIf);
            //get arg.element.value in the form of BigInteger if appropriate
            byte[] argValueBytes = null;
            BigInteger argValueBI = null;

            if (valueNotFound)
            {
                params = null;
                continue;
            }
            if (value != null && !arg.parameterType.equals("string"))
            {
                argValueBytes = convertArgToBytes(value);
                argValueBI = new BigInteger(1, argValueBytes);
            }

            try
            {
                switch (arg.parameterType)
                {
                    case "int":
                        params.add(new Int(argValueBI));
                        break;
                    case "int8":
                        params.add(new Int8(argValueBI));
                        break;
                    case "int16":
                        params.add(new Int16(argValueBI));
                        break;
                    case "int24":
                        params.add(new Int24(argValueBI));
                        break;
                    case "int32":
                        params.add(new Int32(argValueBI));
                        break;
                    case "int40":
                        params.add(new Int40(argValueBI));
                        break;
                    case "int48":
                        params.add(new Int48(argValueBI));
                        break;
                    case "int56":
                        params.add(new Int56(argValueBI));
                        break;
                    case "int64":
                        params.add(new Int64(argValueBI));
                        break;
                    case "int72":
                        params.add(new Int72(argValueBI));
                        break;
                    case "int80":
                        params.add(new Int80(argValueBI));
                        break;
                    case "int88":
                        params.add(new Int88(argValueBI));
                        break;
                    case "int96":
                        params.add(new Int96(argValueBI));
                        break;
                    case "int104":
                        params.add(new Int104(argValueBI));
                        break;
                    case "int112":
                        params.add(new Int112(argValueBI));
                        break;
                    case "int120":
                        params.add(new Int120(argValueBI));
                        break;
                    case "int128":
                        params.add(new Int128(argValueBI));
                        break;
                    case "int136":
                        params.add(new Int136(argValueBI));
                        break;
                    case "int144":
                        params.add(new Int144(argValueBI));
                        break;
                    case "int152":
                        params.add(new Int152(argValueBI));
                        break;
                    case "int160":
                        params.add(new Int160(argValueBI));
                        break;
                    case "int168":
                        params.add(new Int168(argValueBI));
                        break;
                    case "int176":
                        params.add(new Int176(argValueBI));
                        break;
                    case "int184":
                        params.add(new Int184(argValueBI));
                        break;
                    case "int192":
                        params.add(new Int192(argValueBI));
                        break;
                    case "int200":
                        params.add(new Int200(argValueBI));
                        break;
                    case "int208":
                        params.add(new Int208(argValueBI));
                        break;
                    case "int216":
                        params.add(new Int216(argValueBI));
                        break;
                    case "int224":
                        params.add(new Int224(argValueBI));
                        break;
                    case "int232":
                        params.add(new Int232(argValueBI));
                        break;
                    case "int240":
                        params.add(new Int240(argValueBI));
                        break;
                    case "int248":
                        params.add(new Int248(argValueBI));
                        break;
                    case "int256":
                        params.add(new Int256(argValueBI));
                        break;
                    case "uint":
                        params.add(new Uint(argValueBI));
                        break;
                    case "uint8":
                        params.add(new Uint8(argValueBI));
                        break;
                    case "uint16":
                        params.add(new Uint16(argValueBI));
                        break;
                    case "uint24":
                        params.add(new Uint24(argValueBI));
                        break;
                    case "uint32":
                        params.add(new Uint32(argValueBI));
                        break;
                    case "uint40":
                        params.add(new Uint40(argValueBI));
                        break;
                    case "uint48":
                        params.add(new Uint48(argValueBI));
                        break;
                    case "uint56":
                        params.add(new Uint56(argValueBI));
                        break;
                    case "uint64":
                        params.add(new Uint64(argValueBI));
                        break;
                    case "uint72":
                        params.add(new Uint72(argValueBI));
                        break;
                    case "uint80":
                        params.add(new Uint80(argValueBI));
                        break;
                    case "uint88":
                        params.add(new Uint88(argValueBI));
                        break;
                    case "uint96":
                        params.add(new Uint96(argValueBI));
                        break;
                    case "uint104":
                        params.add(new Uint104(argValueBI));
                        break;
                    case "uint112":
                        params.add(new Uint112(argValueBI));
                        break;
                    case "uint120":
                        params.add(new Uint120(argValueBI));
                        break;
                    case "uint128":
                        params.add(new Uint128(argValueBI));
                        break;
                    case "uint136":
                        params.add(new Uint136(argValueBI));
                        break;
                    case "uint144":
                        params.add(new Uint144(argValueBI));
                        break;
                    case "uint152":
                        params.add(new Uint152(argValueBI));
                        break;
                    case "uint160":
                        params.add(new Uint160(argValueBI));
                        break;
                    case "uint168":
                        params.add(new Uint168(argValueBI));
                        break;
                    case "uint176":
                        params.add(new Uint176(argValueBI));
                        break;
                    case "uint184":
                        params.add(new Uint184(argValueBI));
                        break;
                    case "uint192":
                        params.add(new Uint192(argValueBI));
                        break;
                    case "uint200":
                        params.add(new Uint200(argValueBI));
                        break;
                    case "uint208":
                        params.add(new Uint208(argValueBI));
                        break;
                    case "uint216":
                        params.add(new Uint216(argValueBI));
                        break;
                    case "uint224":
                        params.add(new Uint224(argValueBI));
                        break;
                    case "uint232":
                        params.add(new Uint232(argValueBI));
                        break;
                    case "uint240":
                        params.add(new Uint240(argValueBI));
                        break;
                    case "uint248":
                        params.add(new Uint248(argValueBI));
                        break;
                    case "uint256":
                        switch (arg.element.ref)
                        {
                            case "tokenId":
                                params.add(new Uint256(tokenId));
                                break;
                            case "value":
                            default:
                                params.add(new Uint256(argValueBI));
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
                                params.add(new Address(Numeric.toHexString(argValueBytes)));
                                break;
                        }
                        break;
                    case "string":
                        if (value == null) throw new Exception("Attempt to use null value");
                        params.add(new Utf8String(value));
                        break;
                    case "bytes":
                        if (value == null) throw new Exception("Attempt to use null value");
                        params.add(new DynamicBytes(Numeric.hexStringToByteArray(value)));
                        break;
                    case "bytes1":
                        params.add(new Bytes1(argValueBytes));
                        break;
                    case "bytes2":
                        params.add(new Bytes2(argValueBytes));
                        break;
                    case "bytes3":
                        params.add(new Bytes3(argValueBytes));
                        break;
                    case "bytes4":
                        params.add(new Bytes4(argValueBytes));
                        break;
                    case "bytes5":
                        params.add(new Bytes5(argValueBytes));
                        break;
                    case "bytes6":
                        params.add(new Bytes6(argValueBytes));
                        break;
                    case "bytes7":
                        params.add(new Bytes7(argValueBytes));
                        break;
                    case "bytes8":
                        params.add(new Bytes8(argValueBytes));
                        break;
                    case "bytes9":
                        params.add(new Bytes9(argValueBytes));
                        break;
                    case "bytes10":
                        params.add(new Bytes10(argValueBytes));
                        break;
                    case "bytes11":
                        params.add(new Bytes11(argValueBytes));
                        break;
                    case "bytes12":
                        params.add(new Bytes12(argValueBytes));
                        break;
                    case "bytes13":
                        params.add(new Bytes13(argValueBytes));
                        break;
                    case "bytes14":
                        params.add(new Bytes14(argValueBytes));
                        break;
                    case "bytes15":
                        params.add(new Bytes15(argValueBytes));
                        break;
                    case "bytes16":
                        params.add(new Bytes16(argValueBytes));
                        break;
                    case "bytes17":
                        params.add(new Bytes17(argValueBytes));
                        break;
                    case "bytes18":
                        params.add(new Bytes18(argValueBytes));
                        break;
                    case "bytes19":
                        params.add(new Bytes19(argValueBytes));
                        break;
                    case "bytes20":
                        params.add(new Bytes20(argValueBytes));
                        break;
                    case "bytes21":
                        params.add(new Bytes21(argValueBytes));
                        break;
                    case "bytes22":
                        params.add(new Bytes22(argValueBytes));
                        break;
                    case "bytes23":
                        params.add(new Bytes23(argValueBytes));
                        break;
                    case "bytes24":
                        params.add(new Bytes24(argValueBytes));
                        break;
                    case "bytes25":
                        params.add(new Bytes25(argValueBytes));
                        break;
                    case "bytes26":
                        params.add(new Bytes26(argValueBytes));
                        break;
                    case "bytes27":
                        params.add(new Bytes27(argValueBytes));
                        break;
                    case "bytes28":
                        params.add(new Bytes28(argValueBytes));
                        break;
                    case "bytes29":
                        params.add(new Bytes29(argValueBytes));
                        break;
                    case "bytes30":
                        params.add(new Bytes30(argValueBytes));
                        break;
                    case "bytes31":
                        params.add(new Bytes31(argValueBytes));
                        break;
                    case "bytes32": //sometimes tokenId can be passed as bytes32
                        switch (arg.element.ref)
                        {
                            case "tokenId":
                                params.add(new Bytes32(Numeric.toBytesPadded(tokenId, 32)));
                                break;
                            case "value":
                                params.add(new Bytes32(argValueBytes));
                                break;
                            default:
                                params.add(new Bytes32(Numeric.toBytesPadded(argValueBI, 32)));
                                break;
                        }
                        break;
                    default:
                        System.out.println("NOT IMPLEMENTED: " + arg.parameterType);
                        break;
                }
            }
            catch (Exception e)
            {
                //attempting to use unformed value
                valueNotFound = true;
            }
        }
        switch (function.as)
        {
            case UTF8:
                returnTypes.add(new TypeReference<Utf8String>() {});
                break;
            case Signed:
            case Unsigned:
            case UnsignedInput:
            case TokenId:
                returnTypes.add(new TypeReference<Uint256>() {});
                break;
            case Address:
                returnTypes.add(new TypeReference<Address>() {});
                break;
            case Mapping:
            case Boolean:
            default:
                returnTypes.add(new TypeReference<Bytes32>() {});
                break;
        }

        if (valueNotFound)
        {
            params = null;
        }

        return new Function(function.method,
                            params, returnTypes);
    }

    public static byte[] convertArgToBytes(String inputValue)
    {
        byte[] argBytes = new byte[1];
        try
        {
            String hexValue = inputValue;
            if (!Numeric.containsHexPrefix(inputValue))
            {
                BigInteger value;
                try
                {
                    value = new BigInteger(inputValue);
                }
                catch (NumberFormatException e)
                {
                    value = new BigInteger(inputValue, 16);
                }

                hexValue = Numeric.toHexStringNoPrefix(value.toByteArray());
                //fix sign condition
                if (hexValue.length() > 64 && hexValue.startsWith("00"))
                {
                    hexValue = hexValue.substring(2);
                }
            }

            argBytes = Numeric.hexStringToByteArray(hexValue);
        }
        catch (Exception e)
        {
            //no action
        }

        return argBytes;
    }

    private String handleTransactionResult(TransactionResult result, Function function, String responseValue, Attribute attr, long lastTransactionTime)
    {
        String transResult = null;
        try
        {
            //try to interpret the value. For now, just use the raw return value - this is more reliable until we need to interpret arrays
            List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
            if (response.size() > 0)
            {
                result.resultTime = lastTransactionTime;
                Type val = response.get(0);

                BigInteger value;
                byte[] bytes = Bytes.trimLeadingZeroes(Numeric.hexStringToByteArray(responseValue));
                String hexBytes = Numeric.toHexString(bytes);

                switch (attr.syntax)
                {
                    case Boolean:
                        value = Numeric.toBigInt(hexBytes);
                        transResult = value.equals(BigDecimal.ZERO) ? "FALSE" : "TRUE";
                        break;
                    case Integer:
                        value = Numeric.toBigInt(hexBytes);
                        transResult = value.toString();
                        break;
                    case BitString:
                    case NumericString:
                        if (val.getTypeAsString().equals("string"))
                        {
                            transResult = (String)val.getValue();
                            if (responseValue.length() > 2 && transResult.length() == 0)
                            {
                                transResult = checkBytesString(responseValue);
                            }
                        }
                        else
                        {
                            //should be a decimal string
                            value = Numeric.toBigInt(hexBytes);
                            transResult = value.toString();
                        }
                        break;
                    case IA5String:
                    case DirectoryString:
                    case GeneralizedTime:
                    case CountryString:
                        if (val.getTypeAsString().equals("string"))
                        {
                            transResult = (String)val.getValue();
                            if (responseValue.length() > 2 && transResult.length() == 0)
                            {
                                transResult = checkBytesString(responseValue);
                            }
                        }
                        else if (val.getTypeAsString().equals("address"))
                        {
                            transResult = (String)val.getValue();
                        }
                        else
                        {
                            transResult = hexBytes;
                        }
                        break;
                    default:
                        transResult = hexBytes;
                        break;
                }
            }
            else
            {
                result.resultTime = lastTransactionTime == -1 ? -1 : 0;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return transResult;
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

    public static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";


    /**
     * Haven't pre-cached this value yet, so need to fetch it before we can proceed
     * @param attr
     * @param tokenId
     * @param definition
     * @return
     */
    public Observable<TransactionResult> fetchResultFromEthereum(String walletAddress, ContractAddress contractAddress, Attribute attr,
                                                                 BigInteger tokenId, TokenDefinition definition, AttributeInterface attrIf, long lastTransactionTime)
    {
        return Observable.fromCallable(() -> {
            long txUpdateTime = lastTransactionTime;
            TransactionResult transactionResult = new TransactionResult(contractAddress.chainId, contractAddress.address, tokenId, attr);

            // 1: create transaction call
            org.web3j.abi.datatypes.Function transaction = generateTransactionFunction(walletAddress, tokenId, definition, attr.function, attrIf);
            // 2: create web3 connection
            OkHttpClient okClient = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build();

            String nodeURL = EthereumNetworkBase.getNetworkByChain(contractAddress.chainId).rpcServerUrl;
            HttpService nodeService = new HttpService(nodeURL, okClient, false);

            Web3j web3j = Web3j.build(nodeService);

            //now push the transaction
            String result;
            if (transaction.getInputParameters() == null)
            {
                //couldn't validate all the input param values
                result = "";
                txUpdateTime = -1;
            }
            else
            {
                //now push the transaction
                result = callSmartContractFunction(web3j, transaction, contractAddress.address, ZERO_ADDRESS);
            }

            transactionResult.result = handleTransactionResult(transactionResult, transaction, result, attr, txUpdateTime);
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


    public TokenScriptResult.Attribute parseFunctionResult(TransactionResult transactionResult, Attribute attr)
    {
        String res = attr.getSyntaxVal(transactionResult.result);
        BigInteger val = transactionResult.tokenId; //?

        if (attr.syntax == TokenDefinition.Syntax.Boolean)
        {
            if (res.equalsIgnoreCase("TRUE")) val = BigInteger.ONE;
            else val = BigInteger.ZERO;
        }
        else if (attr.syntax == TokenDefinition.Syntax.NumericString && attr.as != As.Address)
        {
            if (transactionResult.result == null)
            {
                res = "0";
            }
            else if (transactionResult.result.startsWith("0x"))
            {
                res = res.substring(2);
            }
            try
            {
                val = new BigInteger(res, 16);
            }
            catch (NumberFormatException e)
            {
                val = BigInteger.ZERO;
            }
        }
        return new TokenScriptResult.Attribute(attr.name, attr.label, val, res);
    }

    private void resolveReference(String walletAddress, MethodArg arg, BigInteger tokenId, TokenDefinition definition, AttributeInterface attrIf)
    {
        if (definition != null && definition.attributes.containsKey(arg.element.ref))
        {
            arg.element.value = fetchAttrResult(walletAddress, definition.attributes.get(arg.element.ref), tokenId, definition, attrIf).blockingSingle().text;
        }
    }


    public String resolveReference(String walletAddress, TokenscriptElement element, BigInteger tokenId, TokenDefinition definition, AttributeInterface attrIf)
    {
        if (isEmpty(element.value))
        {
            return element.value;
        }
        if (definition != null && definition.attributes.containsKey(element.ref)) //resolve from attribute
        {
            Attribute attr = definition.attributes.get(element.ref);
            return fetchArgValue(walletAddress, element, attr, tokenId, definition, attrIf);
        }
        else if (localAttrs.containsKey(element.ref)) //wasn't able to resolve, attempt to resolve from local attributes or mark null if unresolved user input
        {
            Attribute attr = localAttrs.get(element.ref);
            return fetchArgValue(walletAddress, element, attr, tokenId, definition, attrIf);
        }
        else if (localAttrs.containsKey(element.localRef))
        {
            Attribute attr = localAttrs.get(element.localRef);
            return fetchArgValue(walletAddress, element, attr, tokenId, definition, attrIf);
        }
        else if (!isEmpty(element.localRef) && refTags.containsKey(element.localRef))
        {
            return refTags.get(element.localRef);
        }
        else
        {
            return null;
        }
    }

    private String fetchArgValue(String walletAddress, TokenscriptElement element, Attribute attr, BigInteger tokenId, TokenDefinition definition, AttributeInterface attrIf)
    {
        if (attr.userInput)
        {
            if (!isEmpty(element.value)) return element.value; //nullify user input if value is not set
        }
        else if (!isEmpty(element.value))
        {
            return element.value;
        }
        else
        {
            return fetchAttrResult(walletAddress, attr, tokenId, definition, attrIf).blockingSingle().text;
        }

        return null;
    }

    private boolean isEmpty(String str)
    {
        return str == null || str.length() == 0;
    }

    /**
     * Fetches a TokenScript attribute
     *
     * May either return a static attribute sourced from the Token ID or a dynamic one from a contract function
     *
     * If a dynamic function, then test to see if we can source the value from the result cache -
     * Test to see if the contract has seen any transactions AFTER the result was cached, if so then invalidate the result and re-fetch
     *
     * TODO: there is currently an optimisation issue with this. If the contract being called to resolve the attribute is not the token
     *       contract which the script refers to then the result is not cached. This can be seen eg with 'ENABLE' functions which permit
     *       the script contract to manipulate the tokens which the 'ENABLE' function is being called on.
     *       It may not be possible to always safely cache these values; even with an event handler we have to interpret those events and invalidate
     *       any cached results. However if we're tracking the referenced contract as a token then it should be safe
     *
     * @param walletAddress
     * @param attr
     * @param tokenId
     * @param td
     * @param attrIf
     * @return
     */
    public Observable<TokenScriptResult.Attribute> fetchAttrResult(String walletAddress, Attribute attr, BigInteger tokenId,
                                                                   TokenDefinition td, AttributeInterface attrIf)
    {
        if (attr == null)
        {
            return Observable.fromCallable(() -> new TokenScriptResult.Attribute("bd", "bd", BigInteger.ZERO, ""));
        }
        else if (attr.event != null)
        {
            //retrieve events from DB
            ContractAddress useAddress = new ContractAddress(attr.event.contract.addresses.keySet().iterator().next(),
                                                             attr.event.contract.addresses.values().iterator().next().get(0));
            TransactionResult cachedResult = attrIf.getFunctionResult(useAddress, attr, tokenId); //Needs to allow for multiple tokenIds
            return resultFromDatabase(cachedResult, attr);
        }
        else if (attr.function == null)  // static attribute from tokenId (eg city mapping from tokenId)
        {
            return staticAttribute(attr, tokenId);
        }
        else
        {
            ContractAddress useAddress = new ContractAddress(attr.function); //always use the function attribute's address
            long lastTxUpdate = attrIf.getLastTokenUpdate(useAddress.chainId, useAddress.address);
            TransactionResult cachedResult = attrIf.getFunctionResult(useAddress, attr, tokenId); //Needs to allow for multiple tokenIds
            if (cachedResult.resultTime > 0 && ((!attr.isVolatile() && ((attrIf.resolveOptimisedAttr(useAddress, attr, cachedResult) || !cachedResult.needsUpdating(lastTxUpdate)))))) //can we use wallet's known data or cached value?
            {
                return resultFromDatabase(cachedResult, attr);
            }
            else  //if cached value is invalid or if value is dynamic
            {
                //for function query, never need wallet address
                return fetchResultFromEthereum(walletAddress, useAddress, attr, tokenId, td, attrIf, 0)          // Fetch function result from blockchain
                        .map(result -> restoreFromDBIfRequired(result, cachedResult))  // If network unavailable restore value from cache
                        .map(result -> attrIf.storeAuxData("", result))                                     // store new data
                        .map(result -> parseFunctionResult(result, attr));    // write
            }
        }
    }

    public Observable<TokenScriptResult.Attribute> resolveAttributes(String walletAddress, BigInteger tokenId, AttributeInterface attrIf, ContractAddress cAddr, TokenDefinition td)
    {
        td.context = new TokenscriptContext();
        td.context.cAddr = cAddr;
        td.context.attrInterface = attrIf;

        return Observable.fromIterable(new ArrayList<>(td.attributes.values()))
                .flatMap(attr -> fetchAttrResult(walletAddress, attr, tokenId, td, attrIf));
    }

    private Observable<TokenScriptResult.Attribute> staticAttribute(Attribute attr, BigInteger tokenId)
    {
        return Observable.fromCallable(() -> {
            try
            {
                BigInteger val = tokenId.and(attr.bitmask).shiftRight(attr.bitshift);
                return new TokenScriptResult.Attribute(attr.name, attr.label, val, attr.getSyntaxVal(attr.toString(val)));
            }
            catch (Exception e)
            {
                return new TokenScriptResult.Attribute(attr.name, attr.label, tokenId, "unsupported encoding");
            }
        });
    }

    private Observable<TokenScriptResult.Attribute> resultFromDatabase(TransactionResult transactionResult, Attribute attr)
    {
        return Observable.fromCallable(() -> parseFunctionResult(transactionResult, attr));
    }

    /**
     * Restore result from Database if required (eg connection failure), and if there was a database value to restore
     * @param result
     * @param transactionResult
     * @return
     */
    private TransactionResult restoreFromDBIfRequired(TransactionResult result, TransactionResult transactionResult)
    {
        if (result.resultTime == 0 && transactionResult != null)
        {
            result.result = transactionResult.result;
            result.resultTime = transactionResult.resultTime;
        }

        return result;
    }
}