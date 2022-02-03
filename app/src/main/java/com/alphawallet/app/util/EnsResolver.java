package com.alphawallet.app.util;

import android.text.TextUtils;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.UnableToResolveENS;
import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.TokenRepository;
import com.fasterxml.jackson.core.JsonParseException;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Keys;
import org.web3j.ens.Contracts;
import org.web3j.ens.EnsResolutionException;
import org.web3j.ens.NameHash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSyncing;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.utils.Numeric;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import timber.log.Timber;

/**
 * EnsResolver from Web3j adapted for Android Java's BigInteger
 */
public class EnsResolver {

    public static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;
    public static final String REVERSE_NAME_SUFFIX = ".addr.reverse";
    public static final String CRYPTO_RESOLVER = "0xD1E5b0FF1287aA9f9A268759062E4Ab08b9Dacbe";
    public static final String CRYPTO_ETH_KEY = "crypto.ETH.address";

    private final Web3j web3j;
    private final int addressLength;
    private long syncThreshold; // non-final in case this value needs to be tweaked

    public EnsResolver(Web3j web3j, long syncThreshold, int addressLength) {
        this.web3j = web3j;
        this.syncThreshold = syncThreshold;
        this.addressLength = addressLength;
    }

    public EnsResolver(Web3j web3j, long syncThreshold) {
        this(web3j, syncThreshold, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    public EnsResolver(Web3j web3j) {
        this(web3j, DEFAULT_SYNC_THRESHOLD);
    }

    public void setSyncThreshold(long syncThreshold) {
        this.syncThreshold = syncThreshold;
    }

    public long getSyncThreshold() {
        return syncThreshold;
    }

    /**
     * This function takes ensName (eg 'scotty.eth') and returns the matching Ethereum Address.
     * NOTE: It is highly important to check the node is synced before resolving, as this could be an attack
     * @param ensName
     * @return
     */
    public String resolve(String ensName)
    {
        String contractAddress = ensName;
        if (isValidEnsName(ensName, addressLength))
        {
            try
            {
                if (!isSynced()) //ensure node is synced
                {
                    throw new EnsResolutionException("Node is not currently synced");
                }
                else if (ensName.endsWith(".crypto")) //check crypto namespace
                {
                    byte[] nameHash = NameHash.nameHashAsBytes(ensName);
                    BigInteger nameId = new BigInteger(nameHash);
                    String resolverAddress = getContractData(MAINNET_ID, CRYPTO_RESOLVER, getResolverOf(nameId));
                    if (!TextUtils.isEmpty(resolverAddress))
                    {
                        contractAddress = getContractData(MAINNET_ID, resolverAddress, get(nameId));
                    }
                }
                else
                {
                    String resolverAddress = lookupResolver(ensName);
                    if (!TextUtils.isEmpty(resolverAddress))
                    {
                        byte[] nameHash = NameHash.nameHashAsBytes(ensName);
                        //now attempt to get the address of this ENS
                        contractAddress = getContractData(MAINNET_ID, resolverAddress, getAddr(nameHash));
                    }
                }
            }
            catch (Exception e)
            {
                //throw new RuntimeException("Unable to execute Ethereum request", e);
                return "";
            }

            if (!Utils.isAddressValid(contractAddress))
            {
                //throw new RuntimeException("Unable to resolve address for name: " + ensName);
                return "";
            }
            else
            {
                return contractAddress;
            }
        }
        else
        {
            return ensName;
        }
    }

    /**
     * Reverse name resolution as documented in the <a
     * href="https://docs.ens.domains/contract-api-reference/reverseregistrar">specification</a>.
     *
     * @param address an ethereum address, example: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
     * @return a EnsName registered for provided address
     */
    public String reverseResolve(String address) throws UnableToResolveENS
    {
        String name = "";
        if (Utils.isAddressValid(address))
        {
            String reverseName = Numeric.cleanHexPrefix(address) + REVERSE_NAME_SUFFIX;
            try
            {
                String resolverAddress = lookupResolver(reverseName);
                byte[] nameHash = NameHash.nameHashAsBytes(reverseName);
                name = getContractData(MAINNET_ID, resolverAddress, getName(nameHash));
            }
            catch (Exception e)
            {
                //throw new RuntimeException("Unable to execute Ethereum request", e);
                return "";
            }

            if (!isValidEnsName(name, addressLength))
            {
                throw new UnableToResolveENS("Unable to resolve name for address: " + address);
            }
            else
            {
                return name;
            }
        }
        else
        {
            throw new EnsResolutionException("Address is invalid: " + address);
        }
    }

    public String resolveAvatar(String ensName)
    {
        if (isValidEnsName(ensName, addressLength))
        {
            try
            {
                String resolverAddress = lookupResolver(ensName);
                if (!TextUtils.isEmpty(resolverAddress))
                {
                    byte[] nameHash = NameHash.nameHashAsBytes(ensName);
                    //now attempt to get the address of this ENS
                    return getContractData(MAINNET_ID, resolverAddress, getAvatar(nameHash));
                }
            }
            catch (Exception e)
            {
                //
                Timber.e(e);
            }
        }

        return "";
    }

    public String resolveAvatarFromAddress(String address)
    {
        if (Utils.isAddressValid(address))
        {
            String reverseName = Numeric.cleanHexPrefix(address.toLowerCase()) + REVERSE_NAME_SUFFIX;
            try
            {
                String resolverAddress = lookupResolver(reverseName);
                byte[] nameHash = NameHash.nameHashAsBytes(reverseName);
                String avatar = getContractData(MAINNET_ID, resolverAddress, getAvatar(nameHash));
                return avatar != null ? avatar : "";
            }
            catch (Exception e)
            {
                Timber.e(e);
                //throw new RuntimeException("Unable to execute Ethereum request", e);
            }
        }

        return "";
    }

    private String lookupResolver(String ensName) throws Exception
    {
        NetVersion netVersion = web3j.netVersion().send();
        String registryContract = Contracts.resolveRegistryContract(netVersion.getNetVersion());
        byte[] nameHash = NameHash.nameHashAsBytes(ensName);
        Function resolver = getResolver(nameHash);
        return getContractData(MAINNET_ID, registryContract, resolver);
    }

    private Function getResolver(byte[] nameHash)
    {
        return new Function("resolver",
                            Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.asList(new TypeReference<Address>()
                            {
                            }));
    }

    private Function getAvatar(byte[] nameHash)
    {
        return new Function("text",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash),
                                    new org.web3j.abi.datatypes.Utf8String("avatar")),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Utf8String>()
                {
                }));
    }

    private Function getResolverOf(BigInteger nameId)
    {
        return new Function("resolverOf",
                            Arrays.asList(new org.web3j.abi.datatypes.Uint(nameId)),
                            Arrays.asList(new TypeReference<Address>()
                            {
                            }));
    }

    private Function get(BigInteger nameId)
    {
        return new Function("get",
                            Arrays.asList(new org.web3j.abi.datatypes.Utf8String(EnsResolver.CRYPTO_ETH_KEY), new org.web3j.abi.datatypes.generated.Uint256(nameId)),
                            Arrays.asList(new TypeReference<Utf8String>()
                            {
                            }));
    }

    private Function getAddr(byte[] nameHash)
    {
        return new Function("addr",
                            Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.asList(new TypeReference<Address>()
                            {
                            }));
    }

    private Function getName(byte[] nameHash)
    {
        return new Function("name",
                            Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.asList(new TypeReference<Utf8String>()
                            {
                            }));
    }

    boolean isSynced() throws Exception {
        EthSyncing ethSyncing = web3j.ethSyncing().send();
        if (ethSyncing.isSyncing()) {
            return false;
        } else {
            EthBlock ethBlock =
                    web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            long timestamp = ethBlock.getBlock().getTimestamp().longValue() * 1000;

            return System.currentTimeMillis() - syncThreshold < timestamp;
        }
    }

    private String callSmartContractFunction(
            Function function, String contractAddress, long chainId) throws Exception
    {
        try
        {
            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = TokenRepository.getWeb3jService(chainId).ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (InterruptedIOException | UnknownHostException | JsonParseException e)
        {
            //expected to happen when user switches wallets
            return "0x";
        }
    }

    private <T> T getContractData(long chainId, String address, Function function) throws Exception
    {
        String responseValue = callSmartContractFunction(function, address, chainId);

        if (TextUtils.isEmpty(responseValue))
        {
            throw new Exception("Bad contract value");
        }
        else if (responseValue.equals("0x"))
        {
            return (T)"";
        }

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1)
        {
            return (T) response.get(0).getValue();
        }
        else
        {
            return (T)"";
        }
    }

    public static boolean isValidEnsName(String input) {
        return isValidEnsName(input, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    public static boolean isValidEnsName(String input, int addressLength) {
        return input != null && input.contains(".") && input.length() > 4;
    }
}
