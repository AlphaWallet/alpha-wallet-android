/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alphawallet.app.util.ens;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/** Resolution logic for contract addresses. According to https://eips.ethereum.org/EIPS/eip-2544 */
public class EnsResolver implements Resolvable
{

    private static final Logger log = LoggerFactory.getLogger(EnsResolver.class);

    public static final MediaType JSON = MediaType.parse("application/json");

    // Permit number offchain calls  for a single contract call.
    public static final int LOOKUP_LIMIT = 4;

    public static final String REVERSE_NAME_SUFFIX = ".addr.reverse";

    private final Web3j web3j;
    protected final int addressLength;
    protected final long chainId;

    private OkHttpClient client = new OkHttpClient();

    private static DefaultFunctionReturnDecoder decoder;

    public EnsResolver(Web3j web3j, int addressLength)
    {
        this.web3j = web3j;
        this.addressLength = addressLength;

        long chainId = 1;

        try
        {
            NetVersion v = web3j.netVersion().send();
            String ver = v.getNetVersion();
            chainId = Long.parseLong(ver);
        }
        catch (Exception e)
        {
            //
        }

        this.chainId = chainId;
    }

    public EnsResolver(Web3j web3j) {
        this(web3j, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    protected ContractAddress obtainOffchainResolverAddr(String ensName) throws Exception
    {
        return new ContractAddress(chainId, getResolverAddress(ensName));
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
        if (TextUtils.isEmpty(ensName) || (ensName.trim().length() == 1 && ensName.contains("."))) {
            return null;
        }

        try {
            if (isValidEnsName(ensName, addressLength))
            {
                ContractAddress resolverAddress = obtainOffchainResolverAddr(ensName);

                boolean supportWildcard =
                        supportsInterface(EnsUtils.ENSIP_10_INTERFACE_ID, resolverAddress.address);
                byte[] nameHash = NameHash.nameHashAsBytes(ensName);

                String resolvedName;
                if (supportWildcard) {
                    String dnsEncoded = NameHash.dnsEncode(ensName);
                    String addrFunction = encodeResolverAddr(nameHash);

                    EthCall result =
                            resolve(
                                            Numeric.hexStringToByteArray(dnsEncoded),
                                            Numeric.hexStringToByteArray(addrFunction),
                                            resolverAddress.address);

                    String lookupDataHex = result.isReverted() ? Utils.removeDoubleQuotes(result.getError().getData()) : result.getValue();// .toString();
                    resolvedName = resolveOffchain(lookupDataHex, resolverAddress, LOOKUP_LIMIT);
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

    protected String resolveOffchain(
            String lookupData, ContractAddress resolverAddress, int lookupCounter)
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

            String resolvedNameHex = result.isReverted() ? Utils.removeDoubleQuotes(result.getError().getData()) : result.getValue();// .toString();

            // This protocol can result in multiple lookups being requested by the same contract.
            if (EnsUtils.isEIP3668(resolvedNameHex))
            {
                if (lookupCounter <= 0)
                {
                    throw new EnsResolutionException("Lookup calls is out of limit.");
                }

                return resolveOffchain(lookupData, resolverAddress, --lookupCounter);
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
            Request request;
            try {
                request = buildRequest(url, sender, data);
            } catch (JsonProcessingException | EnsResolutionException e) {
                log.error(e.getMessage(), e);
                break;
            }

            try (okhttp3.Response response = client.newCall(request).execute()) {
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
                            sb.append(line).append("\n");
                        }
                        return sb.toString();
                    }
                    catch (Exception e)
                    {
                        //
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
            ContractAddress resolverAddress = obtainOffchainResolverAddr(reverseName);

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
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_addr,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(node)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));

        return getContractData(address, function, "");
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
            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (InterruptedIOException | UnknownHostException | JsonParseException e)
        {
            //expected to happen when user switches wallets
            return "0x";
        }
    }
}
