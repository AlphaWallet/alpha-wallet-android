package com.alphawallet.app.entity.tokenscript;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.token.entity.Attribute;
import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.EventDefinition;
import com.alphawallet.token.entity.NamedType;
import com.alphawallet.token.entity.TokenScriptResult;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.BytesType;
import org.web3j.abi.datatypes.Event;
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
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import timber.log.Timber;

import static org.web3j.tx.Contract.staticExtractEventParameters;

/**
 * Created by JB on 23/03/2020.
 *
 * Class to contain event filter creation functions
 *
 * This class would be better placed entirely in the common library. However because the app uses web3j-Android and DMZ uses web3j
 * the function signatures are incompatible.
 *
 * The cleanest solution is to link the main web3j into the library. Web3j-Android and web3j currently have
 * incompatibilities. Web3j labs would like to eliminate Web3j-Android, which would clear things up very well
 * but this can only be done after we drop API23, even then it may not be possible immediately.
 *
 * Recommend: see if web3j base is compatible with Android API24+. If so, phase out API23 by warning users support will discontinue,
 * then do refactor and move these functions into the EventDefinition class in the library.
 *
 */
public abstract class EventUtils
{
    public static EthFilter generateLogFilter(EventDefinition ev, List<BigInteger> tokenIds, AttributeInterface attrIf) throws Exception
    {
        return generateLogFilter(ev, null, tokenIds, attrIf);
    }

    public static EthFilter generateLogFilter(EventDefinition ev, Token originToken, AttributeInterface attrIf) throws Exception
    {
        if (originToken == null) return null;
        else return generateLogFilter(ev, originToken, originToken.getUniqueTokenIds(), attrIf);
    }

    public static EthFilter generateLogFilter(EventDefinition ev, Token originToken, List<BigInteger> tokenIds, AttributeInterface attrIf) throws Exception
    {
        long chainId = ev.contract.addresses.keySet().iterator().next();
        String eventContractAddr = ev.contract.addresses.get(chainId).get(0);

        final Event resolverEvent = generateEventFunction(ev);
        //work out which topics to filter on
        String filterTopic = ev.getFilterTopicIndex();
        String filterTopicValue = ev.getFilterTopicValue();

        //find the topic index - at this stage we only handle single topics
        int topicIndex = ev.getTopicIndex(filterTopic);

        //isolate which indexed param it is
        List<String> indexedParams = ev.type.getArgNames(true);

        DefaultBlockParameter startBlock = DefaultBlockParameterName.EARLIEST;

        if (ev.readBlock.compareTo(BigInteger.ZERO) > 0)
        {
            startBlock = DefaultBlockParameter.valueOf(ev.readBlock);
        }

        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        DefaultBlockParameterName.LATEST,
                        eventContractAddr) // contract address
                        .addSingleTopic(EventEncoder.encode(resolverEvent));// event name

        for (int i = 0; i < indexedParams.size(); i++)
        {
            if (i == topicIndex)
            {
                if (!addTopicFilter(ev, filter, filterTopicValue, tokenIds, attrIf)) //add the required log filter - allowing for multiple tokenIds
                {
                    return null;
                }
                break;
            }
            else
            {
                filter.addSingleTopic(null);
            }
        }

        return filter;
    }

    public static String getSelectVal(EventDefinition ev, EthLog.LogResult ethLog)
    {
        String selectVal = "";
        final Event resolverEvent = generateEventFunction(ev);
        final EventValues eventValues = staticExtractEventParameters(resolverEvent, (Log)ethLog.get());
        int selectIndexInNonIndexed = ev.getSelectIndex(false);
        int selectIndexInIndexed = ev.getSelectIndex(true);

        if (selectIndexInNonIndexed >= 0)
        {
            selectVal = getValueFromParams(eventValues.getNonIndexedValues(), selectIndexInNonIndexed);
        }
        else if (selectIndexInIndexed >= 0)
        {
            selectVal = getValueFromParams(eventValues.getIndexedValues(), selectIndexInIndexed);
        }

        return selectVal;
    }

    public static String getTopicVal(EventDefinition ev, EthLog.LogResult ethLog)
    {
        String topicVal = "";
        final Event resolverEvent = generateEventFunction(ev);
        final EventValues eventValues = staticExtractEventParameters(resolverEvent, (Log)ethLog.get());
        String filterTopic = ev.getFilterTopicIndex();
        int topicIndex = ev.getTopicIndex(filterTopic);

        topicVal = getValueFromParams(eventValues.getIndexedValues(), topicIndex);

        return topicVal;
    }

    public static Single<EthBlock> getBlockDetails(String blockHash, Web3j web3j)
    {
        return Single.fromCallable(() -> {
            EthBlock txResult;
            try
            {
                txResult = web3j.ethGetBlockByHash(blockHash.trim(), false).send();
                //if (BuildConfig.DEBUG) Timber.d("TS EVENT: " + txResult.getResult().getHash());
            }
            catch (IOException | NullPointerException e)
            {
                Timber.e(e);
                txResult = new EthBlock();
            }

            return txResult;
        });
    }

    public static Single<EthTransaction> getTransactionDetails(String blockHash, Web3j web3j)
    {
        return Single.fromCallable(() -> {
            EthTransaction txResult;
            try
            {
                txResult = web3j.ethGetTransactionByHash(blockHash.trim()).send();
                //if (BuildConfig.DEBUG) Timber.d("TS EVENT: " + txResult.getResult().getHash());
            }
            catch (IOException | NullPointerException e)
            {
                Timber.e(e);
                txResult = new EthTransaction();
            }

            return txResult;
        });
    }

    private static String getValueFromParams(List<Type> responseParams, int selectIndex)
    {
        Type t = responseParams.get(selectIndex);
        String typeName = t.getTypeAsString();
        //strip numbers
        int i = typeName.length() - 1;
        while (Character.isDigit(typeName.charAt(i))) { i--; }//strip
        typeName = typeName.substring(0, i+1);
        byte[] val;

        String selectVal;

        // Note this param gets interpreted according to the script 'syntax'
        // by the attribute value conversion function getSyntaxVal(String ...) in class Attribute
        switch (typeName.toLowerCase())
        {
            case "string":
            case "address":
            case "uint":
            case "int":
            case "bool":
            case "fixed":
                selectVal = t.getValue().toString();
                break;
            case "bytes":
                val = (byte[])(t.getValue());
                selectVal = Numeric.toHexString(val);
                break;

            default:
                selectVal = "Unexpected type: " + t.getTypeAsString();
                break;
        }

        return selectVal;
    }

    /**
     * This function generates an input definition given an arg sequence, including if the arg is indexed
     * Could be used to generate any input function spec to pass to Web3j
     * @param args
     * @return
     */
    private static List<TypeReference<?>> generateFunctionDefinition(List<NamedType.SequenceElement> args)
    {
        List<TypeReference<?>> paramList = new ArrayList<>();
        for (NamedType.SequenceElement element : args)
        {
            switch (element.type)
            {
                case "int":
                    paramList.add(new TypeReference<Int>(element.indexed) { });
                    break;
                case "int8":
                    paramList.add(new TypeReference<Int8>(element.indexed) { });
                    break;
                case "int16":
                    paramList.add(new TypeReference<Int16>(element.indexed) { });
                    break;
                case "int24":
                    paramList.add(new TypeReference<Int24>(element.indexed) { });
                    break;
                case "int32":
                    paramList.add(new TypeReference<Int32>(element.indexed) { });
                    break;
                case "int40":
                    paramList.add(new TypeReference<Int40>(element.indexed) { });
                    break;
                case "int48":
                    paramList.add(new TypeReference<Int48>(element.indexed) { });
                    break;
                case "int56":
                    paramList.add(new TypeReference<Int56>(element.indexed) { });
                    break;
                case "int64":
                    paramList.add(new TypeReference<Int64>(element.indexed) { });
                    break;
                case "int72":
                    paramList.add(new TypeReference<Int72>(element.indexed) { });
                    break;
                case "int80":
                    paramList.add(new TypeReference<Int80>(element.indexed) { });
                    break;
                case "int88":
                    paramList.add(new TypeReference<Int88>(element.indexed) { });
                    break;
                case "int96":
                    paramList.add(new TypeReference<Int96>(element.indexed) { });
                    break;
                case "int104":
                    paramList.add(new TypeReference<Int104>(element.indexed) { });
                    break;
                case "int112":
                    paramList.add(new TypeReference<Int112>(element.indexed) { });
                    break;
                case "int120":
                    paramList.add(new TypeReference<Int120>(element.indexed) { });
                    break;
                case "int128":
                    paramList.add(new TypeReference<Int128>(element.indexed) { });
                    break;
                case "int136":
                    paramList.add(new TypeReference<Int136>(element.indexed) { });
                    break;
                case "int144":
                    paramList.add(new TypeReference<Int144>(element.indexed) { });
                    break;
                case "int152":
                    paramList.add(new TypeReference<Int152>(element.indexed) { });
                    break;
                case "int160":
                    paramList.add(new TypeReference<Int160>(element.indexed) { });
                    break;
                case "int168":
                    paramList.add(new TypeReference<Int168>(element.indexed) { });
                    break;
                case "int176":
                    paramList.add(new TypeReference<Int176>(element.indexed) { });
                    break;
                case "int184":
                    paramList.add(new TypeReference<Int184>(element.indexed) { });
                    break;
                case "int192":
                    paramList.add(new TypeReference<Int192>(element.indexed) { });
                    break;
                case "int200":
                    paramList.add(new TypeReference<Int200>(element.indexed) { });
                    break;
                case "int208":
                    paramList.add(new TypeReference<Int208>(element.indexed) { });
                    break;
                case "int216":
                    paramList.add(new TypeReference<Int216>(element.indexed) { });
                    break;
                case "int224":
                    paramList.add(new TypeReference<Int224>(element.indexed) { });
                    break;
                case "int232":
                    paramList.add(new TypeReference<Int232>(element.indexed) { });
                    break;
                case "int240":
                    paramList.add(new TypeReference<Int240>(element.indexed) { });
                    break;
                case "int248":
                    paramList.add(new TypeReference<Int248>(element.indexed) { });
                    break;
                case "int256":
                    paramList.add(new TypeReference<Int256>(element.indexed) { });
                    break;
                case "uint":
                    paramList.add(new TypeReference<Uint>(element.indexed) { });
                    break;

                case "uint8":
                    paramList.add(new TypeReference<Uint8>(element.indexed) { });
                    break;
                case "uint16":
                    paramList.add(new TypeReference<Uint16>(element.indexed) { });
                    break;
                case "uint24":
                    paramList.add(new TypeReference<Uint24>(element.indexed) { });
                    break;
                case "uint32":
                    paramList.add(new TypeReference<Uint32>(element.indexed) { });
                    break;
                case "uint40":
                    paramList.add(new TypeReference<Uint40>(element.indexed) { });
                    break;
                case "uint48":
                    paramList.add(new TypeReference<Uint48>(element.indexed) { });
                    break;
                case "uint56":
                    paramList.add(new TypeReference<Uint56>(element.indexed) { });
                    break;
                case "uint64":
                    paramList.add(new TypeReference<Uint64>(element.indexed) { });
                    break;
                case "uint72":
                    paramList.add(new TypeReference<Uint72>(element.indexed) { });
                    break;
                case "uint80":
                    paramList.add(new TypeReference<Uint80>(element.indexed) { });
                    break;
                case "uint88":
                    paramList.add(new TypeReference<Uint88>(element.indexed) { });
                    break;
                case "uint96":
                    paramList.add(new TypeReference<Uint96>(element.indexed) { });
                    break;
                case "uint104":
                    paramList.add(new TypeReference<Uint104>(element.indexed) { });
                    break;
                case "uint112":
                    paramList.add(new TypeReference<Uint112>(element.indexed) { });
                    break;
                case "uint120":
                    paramList.add(new TypeReference<Uint120>(element.indexed) { });
                    break;
                case "uint128":
                    paramList.add(new TypeReference<Uint128>(element.indexed) { });
                    break;
                case "uint136":
                    paramList.add(new TypeReference<Uint136>(element.indexed) { });
                    break;
                case "uint144":
                    paramList.add(new TypeReference<Uint144>(element.indexed) { });
                    break;
                case "uint152":
                    paramList.add(new TypeReference<Uint152>(element.indexed) { });
                    break;
                case "uint160":
                    paramList.add(new TypeReference<Uint160>(element.indexed) { });
                    break;
                case "uint168":
                    paramList.add(new TypeReference<Uint168>(element.indexed) { });
                    break;
                case "uint176":
                    paramList.add(new TypeReference<Uint176>(element.indexed) { });
                    break;
                case "uint184":
                    paramList.add(new TypeReference<Uint184>(element.indexed) { });
                    break;
                case "uint192":
                    paramList.add(new TypeReference<Uint192>(element.indexed) { });
                    break;
                case "uint200":
                    paramList.add(new TypeReference<Uint200>(element.indexed) { });
                    break;
                case "uint208":
                    paramList.add(new TypeReference<Uint208>(element.indexed) { });
                    break;
                case "uint216":
                    paramList.add(new TypeReference<Uint216>(element.indexed) { });
                    break;
                case "uint224":
                    paramList.add(new TypeReference<Uint224>(element.indexed) { });
                    break;
                case "uint232":
                    paramList.add(new TypeReference<Uint232>(element.indexed) { });
                    break;
                case "uint240":
                    paramList.add(new TypeReference<Uint240>(element.indexed) { });
                    break;
                case "uint248":
                    paramList.add(new TypeReference<Uint248>(element.indexed) { });
                    break;
                case "uint256":
                    paramList.add(new TypeReference<Uint256>(element.indexed) { });
                    break;
                case "address":
                    paramList.add(new TypeReference<Address>(element.indexed) { });
                    break;
                case "string":
                    paramList.add(new TypeReference<Utf8String>(element.indexed) { });
                    break;
                case "bytes":
                    paramList.add(new TypeReference<BytesType>(element.indexed) { });
                    break;
                case "bytes1":
                    paramList.add(new TypeReference<Bytes1>(element.indexed) { });
                    break;
                case "bytes2":
                    paramList.add(new TypeReference<Bytes2>(element.indexed) { });
                    break;
                case "bytes3":
                    paramList.add(new TypeReference<Bytes3>(element.indexed) { });
                    break;
                case "bytes4":
                    paramList.add(new TypeReference<Bytes4>(element.indexed) { });
                    break;
                case "bytes5":
                    paramList.add(new TypeReference<Bytes5>(element.indexed) { });
                    break;
                case "bytes6":
                    paramList.add(new TypeReference<Bytes6>(element.indexed) { });
                    break;
                case "bytes7":
                    paramList.add(new TypeReference<Bytes7>(element.indexed) { });
                    break;
                case "bytes8":
                    paramList.add(new TypeReference<Bytes8>(element.indexed) { });
                    break;
                case "bytes9":
                    paramList.add(new TypeReference<Bytes9>(element.indexed) { });
                    break;
                case "bytes10":
                    paramList.add(new TypeReference<Bytes10>(element.indexed) { });
                    break;
                case "bytes11":
                    paramList.add(new TypeReference<Bytes11>(element.indexed) { });
                    break;
                case "bytes12":
                    paramList.add(new TypeReference<Bytes12>(element.indexed) { });
                    break;
                case "bytes13":
                    paramList.add(new TypeReference<Bytes13>(element.indexed) { });
                    break;
                case "bytes14":
                    paramList.add(new TypeReference<Bytes14>(element.indexed) { });
                    break;
                case "bytes15":
                    paramList.add(new TypeReference<Bytes15>(element.indexed) { });
                    break;
                case "bytes16":
                    paramList.add(new TypeReference<Bytes16>(element.indexed) { });
                    break;
                case "bytes17":
                    paramList.add(new TypeReference<Bytes17>(element.indexed) { });
                    break;
                case "bytes18":
                    paramList.add(new TypeReference<Bytes18>(element.indexed) { });
                    break;
                case "bytes19":
                    paramList.add(new TypeReference<Bytes19>(element.indexed) { });
                    break;
                case "bytes20":
                    paramList.add(new TypeReference<Bytes20>(element.indexed) { });
                    break;
                case "bytes21":
                    paramList.add(new TypeReference<Bytes21>(element.indexed) { });
                    break;
                case "bytes22":
                    paramList.add(new TypeReference<Bytes22>(element.indexed) { });
                    break;
                case "bytes23":
                    paramList.add(new TypeReference<Bytes23>(element.indexed) { });
                    break;
                case "bytes24":
                    paramList.add(new TypeReference<Bytes24>(element.indexed) { });
                    break;
                case "bytes25":
                    paramList.add(new TypeReference<Bytes25>(element.indexed) { });
                    break;
                case "bytes26":
                    paramList.add(new TypeReference<Bytes26>(element.indexed) { });
                    break;
                case "bytes27":
                    paramList.add(new TypeReference<Bytes27>(element.indexed) { });
                    break;
                case "bytes28":
                    paramList.add(new TypeReference<Bytes28>(element.indexed) { });
                    break;
                case "bytes29":
                    paramList.add(new TypeReference<Bytes29>(element.indexed) { });
                    break;
                case "bytes30":
                    paramList.add(new TypeReference<Bytes30>(element.indexed) { });
                    break;
                case "bytes31":
                    paramList.add(new TypeReference<Bytes31>(element.indexed) { });
                    break;
                case "bytes32":
                    paramList.add(new TypeReference<Bytes32>(element.indexed) { });
                    break;
                default:
                    Timber.d("NOT IMPLEMENTED: " + element.type);
                    break;
            }
        }

        return paramList;
    }

    private static Event generateEventFunction(EventDefinition ev)
    {
        List<TypeReference<?>> eventArgSpec = EventUtils.generateFunctionDefinition(ev.type.getSequenceArgs());
        return new Event(ev.type.name, eventArgSpec);
    }

    private static boolean addTopicFilter(EventDefinition ev, EthFilter filter, String filterTopicValue, List<BigInteger> tokenIds, AttributeInterface attrIf) throws Exception
    {
        boolean filterSuccess = true;
        //find the topic value
        switch (filterTopicValue)
        {
            case "tokenId":
                if (tokenIds.size() == 0)
                {
                    filterSuccess = false;
                }
                else if (tokenIds.size() == 1)
                {
                    filter.addSingleTopic("0x" + TypeEncoder.encode(new Uint256(tokenIds.get(0))));
                }
                else
                {
                    //listen for multiple tokenIds
                    List<String> optionals = new ArrayList<>();
                    for (BigInteger uid : tokenIds)
                    {
                        String entry = "0x" + TypeEncoder.encode(new Uint256(uid));
                        optionals.add(entry);
                    }
                    filter.addOptionalTopics(optionals.toArray(new String[0]));
                }
                break;
            case "ownerAddress":
                filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(attrIf.getWalletAddr())));
                break;
            default:
                Attribute attr = attrIf.fetchAttribute(ev.contract, filterTopicValue);
                if (attr != null)
                {
                    ContractAddress tokenAddr = new ContractAddress(ev.getEventChainId(), ev.getEventContractAddress());//new ContractAddress(originToken.tokenInfo.chainId, originToken.getAddress());
                    if (tokenIds.size() == 0)
                    {
                        filterSuccess = false;
                    }
                    else if (tokenIds.size() == 1)
                    {
                        TokenScriptResult.Attribute attrResult = attrIf.fetchAttrResult(tokenAddr, attr, tokenIds.get(0));
                        filter.addSingleTopic("0x" + TypeEncoder.encode(new Uint256(attrResult.value)));
                    }
                    else
                    {
                        //listen for multiple tokenId results
                        List<String> optionals = new ArrayList<>();
                        for (BigInteger uid : tokenIds)
                        {
                            TokenScriptResult.Attribute attrResult = attrIf.fetchAttrResult(tokenAddr, attr, uid);
                            String entry = "0x" + TypeEncoder.encode(new Uint256(attrResult.value));
                            optionals.add(entry);
                        }
                        filter.addOptionalTopics(optionals.toArray(new String[0]));
                    }
                }
                else
                {
                    filterSuccess = false;
                    throw new Exception("Unresolved event filter name: " + filterTopicValue);
                }
                break;
        }

        return filterSuccess;
    }

    public static String getAllTopics(EventDefinition ev, EthLog.LogResult log)
    {
        final Event resolverEvent = generateEventFunction(ev);
        final EventValues eventValues = staticExtractEventParameters(resolverEvent, (Log)log.get());

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (NamedType.SequenceElement e : ev.type.getSequenceArgs())
        {
            if (!first) sb.append(",");
            sb.append(e.name);
            sb.append(",");
            sb.append(e.type);
            sb.append(",");

            String result = getEventResult(ev, e.name, eventValues);
            sb.append(result);
            first = false;
        }

        return sb.toString();
    }

    private static String getEventResult(EventDefinition ev, String name, final EventValues eventValues)
    {
        int indexed = ev.getTopicIndex(name);
        int nonIndexed = ev.getNonIndexedIndex(name);

        if (indexed >= 0)
        {
            return eventValues.getIndexedValues().get(indexed).getValue().toString();
        }
        else
        {
            return eventValues.getNonIndexedValues().get(nonIndexed).getValue().toString();
        }
    }

    public static BigInteger getTokenId(EventDefinition ev, EthLog.LogResult log)
    {
        BigInteger tokenId;
        String filterTopicValue = ev.getFilterTopicValue();
        if (filterTopicValue.equals("tokenId"))
        {
            String tokenIdStr = EventUtils.getTopicVal(ev, log);
            if (tokenIdStr.startsWith("0x"))
            {
                tokenId = com.alphawallet.token.tools.Numeric.toBigInt(tokenIdStr);
            }
            else
            {
                tokenId = new BigInteger(tokenIdStr);
            }
        }
        else
        {
            tokenId = BigInteger.ZERO;
        }

        return tokenId;
    }
}
