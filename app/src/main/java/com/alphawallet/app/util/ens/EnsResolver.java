package com.alphawallet.app.util.ens;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3j.ens.Contracts;
import com.alphawallet.app.web3j.ens.EnsGatewayRequestDTO;
import com.alphawallet.app.web3j.ens.EnsGatewayResponseDTO;
import com.alphawallet.app.web3j.ens.EnsResolutionException;
import com.alphawallet.app.web3j.ens.EnsUtils;
import com.alphawallet.app.web3j.ens.NameHash;
import com.alphawallet.app.web3j.ens.OffchainLookup;
import com.alphawallet.token.entity.ContractAddress;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import timber.log.Timber;

/** Resolution logic for contract addresses. According to https://eips.ethereum.org/EIPS/eip-2544 */
public class EnsResolver implements Resolvable
{

    private static final Logger log = LoggerFactory.getLogger(EnsResolver.class);

    public static final MediaType JSON = MediaType.parse("application/json");

    // Permit number offchain calls  for a single contract call.
    public static final int LOOKUP_LIMIT = 4;
    private static final long ENS_CACHE_TIME_VALIDITY = 10 * (1000*60); //10 minutes

    public static final String REVERSE_NAME_SUFFIX = ".addr.reverse";

    public static final long USE_ENS_CHAIN = MAINNET_ID;

    public static final String CANCELLED_REQUEST = "##C";

    private final Web3j web3j;
    protected final int addressLength;
    protected long chainId;

    private OkHttpClient client = new OkHttpClient();

    private static DefaultFunctionReturnDecoder decoder;

    // Cancellation mechanism
    private volatile String currentResolveRequestId = null;
    private final ConcurrentLinkedQueue<String> cancelledRequests = new ConcurrentLinkedQueue<>();

    public EnsResolver(Web3j web3j, int addressLength)
    {
        this.web3j = web3j;
        this.addressLength = addressLength;

        chainId = USE_ENS_CHAIN;

        Single.fromCallable(() -> {
                    NetVersion v = web3j.netVersion().send();
                    String ver = v.getNetVersion();
                    return Long.parseLong(ver);
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(id -> this.chainId = id, Timber::w)
                .isDisposed();
    }

    public EnsResolver(Web3j web3j) {
        this(web3j, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    protected ContractAddress obtainOffChainResolverAddress(String ensName) throws Exception
    {
        String resolverAddress = cachedResolver.containsKey(ensName) ? cachedResolver.get(ensName) : getResolverAddress(ensName);
        if (!TextUtils.isEmpty(resolverAddress))
        {
            cachedResolver.put(ensName, resolverAddress);
        }
        return new ContractAddress(chainId, resolverAddress);
    }

    /**
     * Cancels any ongoing resolve operation
     */
    public void cancelCurrentResolve() {
        if (currentResolveRequestId != null) {
            cancelledRequests.add(currentResolveRequestId);
        }
    }

    /**
     * Checks if a request was cancelled
     */
    private boolean isRequestCancelled(String requestId) {
        return cancelledRequests.contains(requestId);
    }

    /**
     * Removes a request from the cancelled requests queue
     */
    private void removeCancelledRequest(String requestId) {
        cancelledRequests.remove(requestId);
    }

    /**
     * Returns the address of the resolver for the specified node.
     *
     * @param ensName The specified node.
     * @return address of the resolver.
     */
    @Override
    public String resolve(String ensName) throws Exception
    {
        String requestId = UUID.randomUUID().toString();
        currentResolveRequestId = requestId;

        try {
            String result = resolveInternal(ensName, requestId);
            
            // Check if this request was cancelled before returning the result
            if (isRequestCancelled(requestId)) {
                removeCancelledRequest(requestId);
                return CANCELLED_REQUEST;
            }
            
            return result;
        } finally {
            if (requestId.equals(currentResolveRequestId)) {
                currentResolveRequestId = null;
            }
        }
    }

    //Need to cache results for Resolve
    private static final Map<String, CachedENSRead> cachedNameReads = new ConcurrentHashMap<>();
    private static final Map<String, String> cachedResolver = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> cachedSuportsWildcard = new ConcurrentHashMap<>();

    private String cacheKey(String ensName, String addrFunction)
    {
        return ((ensName != null) ? ensName : "") + "%" + ((addrFunction != null) ? addrFunction : "");
    }

    private String resolveWithCaching(String ensName, byte[] nameHash, String resolverAddr) throws Exception
    {
        String dnsEncoded = NameHash.dnsEncode(ensName);
        String addrFunction = encodeResolverAddr(nameHash);

        CachedENSRead lookupData = cachedNameReads.get(cacheKey(ensName, addrFunction));
        String lookupDataHex = lookupData != null ? lookupData.cachedResult : null;

        if (lookupData == null || !lookupData.isValid())
        {
            EthCall result =
                    resolve(
                            Numeric.hexStringToByteArray(dnsEncoded),
                            Numeric.hexStringToByteArray(addrFunction),
                            resolverAddr);
            lookupDataHex = result.isReverted() ? Utils.removeDoubleQuotes(result.getError().getData()) : result.getValue();// .toString();
            if (!TextUtils.isEmpty(lookupDataHex) && !lookupDataHex.equals("0x"))
            {
                cachedNameReads.put(cacheKey(ensName, addrFunction), new CachedENSRead(lookupDataHex));
            }
        }

        return lookupDataHex;
    }

    private String resolveInternal(String ensName, String requestId) throws Exception
    {
        if (TextUtils.isEmpty(ensName) || (ensName.trim().length() == 1 && ensName.contains("."))) {
            return null;
        }

        try {
            if (isValidEnsName(ensName, addressLength))
            {
                ContractAddress resolverAddress = obtainOffChainResolverAddress(ensName);

                boolean supportWildcard = getSupportsWildcard(resolverAddress.address);
                byte[] nameHash = NameHash.nameHashAsBytes(ensName);

                String resolvedName;
                if (supportWildcard) {
                    String lookupDataHex = resolveWithCaching(ensName, nameHash, resolverAddress.address);
                    resolvedName = resolveOffchain(lookupDataHex, resolverAddress, LOOKUP_LIMIT, requestId);
                } else {
                    try {
                        resolvedName = resolverAddr(nameHash, resolverAddress.address);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to execute Ethereum request: ", e);
                    }
                }

                if (!WalletUtils.isValidAddress(resolvedName)) {
                    throw new EnsResolutionException(
                            "Unable to resolve address for name: " + ensName);
                } else {
                    return resolvedName;
                }

            } else {
                return ensName;
            }
        } catch (Exception e) {
            throw new EnsResolutionException(e);
        }
    }

    private boolean getSupportsWildcard(String address) throws Exception
    {
        if (cachedSuportsWildcard.containsKey(address))
        {
            return cachedSuportsWildcard.get(address);
        }

        boolean supportWildcard =
                supportsInterface(EnsUtils.ENSIP_10_INTERFACE_ID, address);
        cachedSuportsWildcard.put(address, supportWildcard);
        return supportWildcard;
    }

    protected String resolveOffchain(
            String lookupData, ContractAddress resolverAddress, int lookupCounter, String requestId)
            throws Exception
    {
        if (EnsUtils.isEIP3668(lookupData))
        {
            OffchainLookup offchainLookup =
                    OffchainLookup.build(Numeric.hexStringToByteArray(lookupData.substring(10)));

            if (!resolverAddress.address.equals(offchainLookup.getSender()))
            {
                throw new EnsResolutionException(
                        "Cannot handle OffchainLookup raised inside nested call");
            }

            String gatewayResult =
                    ccipReadFetch(
                            offchainLookup.getUrls(),
                            offchainLookup.getSender(),
                            Numeric.toHexString(offchainLookup.getCallData()));

            if (gatewayResult == null)
            {
                throw new EnsResolutionException("CCIP Read disabled or provided no URLs.");
            }

            ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
            EnsGatewayResponseDTO gatewayResponseDTO =
                    objectMapper.readValue(gatewayResult, EnsGatewayResponseDTO.class);

            EthCall result =
                    resolveWithProof(
                            Numeric.hexStringToByteArray(gatewayResponseDTO.getData()),
                            offchainLookup.getExtraData(), resolverAddress.address);

            String resolvedNameHex = result.isReverted() ? Utils.removeDoubleQuotes(result.getError().getData()) : result.getValue();

            // This protocol can result in multiple lookups being requested by the same contract.
            if (EnsUtils.isEIP3668(resolvedNameHex))
            {
                if (lookupCounter <= 0)
                {
                    throw new EnsResolutionException("Lookup calls is out of limit.");
                }

                return resolveOffchain(lookupData, resolverAddress, --lookupCounter, requestId);
            }
            else
            {
                byte[] resolvedNameBytes = decodeDynamicBytes(resolvedNameHex);

                return decodeAddress(
                        Numeric.toHexString(resolvedNameBytes));
            }
        }

        return lookupData;
    }

    private static byte[] decodeDynamicBytes(String rawInput)
    {
        if (decoder == null) decoder = new DefaultFunctionReturnDecoder();
        List outputParameters = new ArrayList<TypeReference<Type>>();
        outputParameters.add(new TypeReference<DynamicBytes>() {});

        List<Type> typeList = decoder.decodeFunctionResult(rawInput, outputParameters);

        return typeList.isEmpty() ? null : ((DynamicBytes) typeList.get(0)).getValue();
    }

    private static String decodeAddress(String rawInput)
    {
        if (decoder == null) decoder = new DefaultFunctionReturnDecoder();
        List outputParameters = new ArrayList<TypeReference<Type>>();
        outputParameters.add(new TypeReference<Address>() {});

        List<Type> typeList = decoder.decodeFunctionResult(rawInput, outputParameters);

        return typeList.isEmpty() ? null : ((Address) typeList.get(0)).getValue();
    }

    protected String ccipReadFetch(List<String> urls, String sender, String data) {
        List<String> errorMessages = new ArrayList<>();

        for (String url : urls) {
            if (isRequestCancelled(currentResolveRequestId)) {
                return null;
            }

            Request request;
            try {
                request = buildRequest(url, sender, data);
            } catch (JsonProcessingException | EnsResolutionException e) {
                log.error(e.getMessage(), e);
                break;
            }

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (isRequestCancelled(currentResolveRequestId)) {
                    return null;
                }

                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        log.warn("Response body is null, url: {}", url);
                        break;
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream())))
                    {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (isRequestCancelled(currentResolveRequestId)) {
                                return null;
                            }
                            sb.append(line).append("\n");
                        }
                        return sb.toString();
                    }
                    catch (Exception e)
                    {
                        return "";
                    }
                } else {
                    int statusCode = response.code();
                    // 4xx indicates the result is not present; stop
                    if (statusCode >= 400 && statusCode < 500) {
                        log.error(
                                "Response error during CCIP fetch: url {}, error: {}",
                                url,
                                response.message());
                        throw new EnsResolutionException(response.message());
                    }

                    // 5xx indicates server issue; try the next url
                    errorMessages.add(response.message());

                    log.warn(
                            "Response error 500 during CCIP fetch: url {}, error: {}",
                            url,
                            response.message());
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        log.warn(Arrays.toString(errorMessages.toArray()));
        return null;
    }

    protected Request buildRequest(String url, String sender, String data)
            throws JsonProcessingException {
        if (sender == null || !WalletUtils.isValidAddress(sender)) {
            throw new EnsResolutionException("Sender address is null or not valid");
        }
        if (data == null) {
            throw new EnsResolutionException("Data is null");
        }
        if (!url.contains("{sender}")) {
            throw new EnsResolutionException("Url is not valid, sender parameter is not exist");
        }

        // URL expansion
        String href = url.replace("{sender}", sender).replace("{data}", data);

        Request.Builder builder = new Request.Builder().url(href);

        if (url.contains("{data}")) {
            return builder.get().build();
        } else {
            EnsGatewayRequestDTO requestDTO = new EnsGatewayRequestDTO(data);
            ObjectMapper om = ObjectMapperFactory.getObjectMapper();

            return builder.post(RequestBody.create(om.writeValueAsString(requestDTO), JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();
        }
    }

    /**
     * Reverse name resolution as documented in the <a
     * href="https://docs.ens.domains/contract-api-reference/reverseregistrar">specification</a>.
     *
     * @param address an ethereum address, example: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
     * @return a EnsName registered for provided address
     */
    public String reverseResolve(String address) throws Exception
    {
        if (WalletUtils.isValidAddress(address, addressLength))
        {
            String reverseName = Numeric.cleanHexPrefix(address) + REVERSE_NAME_SUFFIX;
            ContractAddress resolverAddress = obtainOffChainResolverAddress(reverseName);

            byte[] nameHash = NameHash.nameHashAsBytes(reverseName);
            String name;
            try {
                name = resolveName(nameHash, resolverAddress.address);
            } catch (Exception e) {
                throw new RuntimeException("Unable to execute Ethereum request", e);
            }

            if (!isValidEnsName(name, addressLength)) {
                throw new RuntimeException("Unable to resolve name for address: " + address);
            } else {
                return name;
            }
        } else {
            throw new EnsResolutionException("Address is invalid: " + address);
        }
    }

    private Function getResolver(byte[] nameHash)
    {
        return new Function("resolver",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                Arrays.asList(new TypeReference<Address>()
                {
                }));
    }

    public String getResolverAddress(String ensName) throws Exception
    {
        String registryContract = Contracts.resolveRegistryContract(chainId);
        byte[] nameHash = NameHash.nameHashAsBytes(ensName);
        Function resolver = getResolver(nameHash);
        String address = getContractData(registryContract, resolver, "");

        if (EnsUtils.isAddressEmpty(address)) {
            address = getResolverAddress(EnsUtils.getParent(ensName));
        }

        return address;
    }

    public boolean validate(String input) {
        return isValidEnsName(input, addressLength);
    }

    public static boolean isValidEnsName(String input) {
        return isValidEnsName(input, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    public static boolean isValidEnsName(String input, int addressLength) {
        return input != null // will be set to null on new Contract creation
                && (input.contains(".") || !WalletUtils.isValidAddress(input, addressLength));
    }

    public void setHttpClient(OkHttpClient client) {
        this.client = client;
    }

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";
    public static final String FUNC_addr = "addr";
    public static final String FUNC_RESOLVE = "resolve";
    public static final String FUNC_RESOLVEWITHPROOF = "resolveWithProof";
    public static final String FUNC_NAME = "name";

    public boolean supportsInterface(byte[] interfaceID, String address) throws Exception
    {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SUPPORTSINTERFACE,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceID)),
                Arrays.asList(new TypeReference<Bool>()
                {
                }));

        return getContractData(address, function, true);
    }

    public String resolverAddr(byte[] node, String address) throws Exception
    {
        //use caching
        String nodeData = Numeric.toHexString(node);
        CachedENSRead resolverData = cachedNameReads.get(cacheKey(nodeData, address));
        String resolverAddr = resolverData != null ? resolverData.cachedResult : null;

        if (resolverData == null || !resolverData.isValid())
        {
            final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_addr,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(node)),
                    Arrays.<TypeReference<?>>asList(new TypeReference<Address>()
                    {
                    }));

            resolverAddr = getContractData(address, function, "");
            if (!TextUtils.isEmpty(resolverAddr) && resolverAddr.length() > 2)
            {
                cachedNameReads.put(cacheKey(nodeData, address), new CachedENSRead(resolverAddr));
            }
        }
        return resolverAddr;
    }

    public String encodeResolverAddr(byte[] node)
    {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_addr,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(node)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));

        return FunctionEncoder.encode(function);
    }

    public EthCall resolve(byte[] name, byte[] data, String address) throws Exception
    {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_RESOLVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(name),
                        new org.web3j.abi.datatypes.DynamicBytes(data)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.request.Transaction transaction
                = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, address, encodedFunction);
        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
    }

    public EthCall resolveWithProof(byte[] response, byte[] extraData, String address) throws Exception
    {
        if (isRequestCancelled(currentResolveRequestId)) {
            return null;
        }

        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_RESOLVEWITHPROOF,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(response),
                        new org.web3j.abi.datatypes.DynamicBytes(extraData)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.request.Transaction transaction
                = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, address, encodedFunction);
        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
    }

    private String resolveName(byte[] node, String address) throws Exception
    {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAME,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(node)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));

        return getContractData(address, function, "");
    }

    public <T> T getContractData(String address, Function function, T type) throws Exception
    {
        String responseValue = callSmartContractFunction(function, address);

        if (TextUtils.isEmpty(responseValue))
        {
            throw new Exception("Bad contract value");
        }
        else if (responseValue.equals("0x"))
        {
            if (type instanceof Boolean)
            {
                return (T) Boolean.FALSE;
            }
            else
            {
                return null;
            }
        }

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1)
        {
            return (T) response.get(0).getValue();
        }
        else
        {
            if (type instanceof Boolean)
            {
                return (T) Boolean.FALSE;
            }
            else
            {
                return null;
            }
        }
    }

    private String callSmartContractFunction(
            Function function, String contractAddress) throws Exception
    {
        try
        {
            if (isRequestCancelled(currentResolveRequestId)) {
                return "0x";
            }

            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            if (isRequestCancelled(currentResolveRequestId)) {
                return "0x";
            }

            return response.getValue();
        }
        catch (InterruptedIOException | UnknownHostException | JsonParseException e)
        {
            //expected to happen when user switches wallets
            return "0x";
        }
        catch (Exception e)
        {
            return "0x";
        }
    }

    private static class CachedENSRead
    {
        public final String cachedResult;
        public final long cachedResultTime;

        public CachedENSRead(String result)
        {
            cachedResult = result;
            cachedResultTime = System.currentTimeMillis();
        }

        public boolean isValid()
        {
            return System.currentTimeMillis() < (cachedResultTime + ENS_CACHE_TIME_VALIDITY); //10 minutes cache validity
        }
    }
}
