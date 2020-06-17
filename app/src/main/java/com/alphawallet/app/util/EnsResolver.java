package com.alphawallet.app.util;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.unstoppabledomains.resolution.Resolution;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

/**
 * EnsResolver from Web3j adapted for Android Java's BigInteger
 */
public class EnsResolver {

    public static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;
    public static final String REVERSE_NAME_SUFFIX = ".addr.reverse";

    private final Web3j web3j;
    private final int addressLength;
    private final Resolution resolution;
    private long syncThreshold; // non-final in case this value needs to be tweaked

    public EnsResolver(Web3j web3j, long syncThreshold, int addressLength) {
        this.web3j = web3j;
        this.syncThreshold = syncThreshold;
        this.addressLength = addressLength;
        this.resolution = new Resolution(EthereumNetworkRepository.MAINNET_RPC_URL); //for .crypto domain
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
     * @param contractId
     * @return
     */
    public String resolve(String contractId)
    {
        String contractAddress = contractId;
        if (isValidEnsName(contractId, addressLength))
        {
            try
            {
                if (!isSynced()) //ensure node is synced
                {
                    throw new EnsResolutionException("Node is not currently synced");
                }
                else if (contractId.endsWith(".crypto")) //check crypto namespace
                {
                    return resolution.addr(contractId, "eth");
                }
                else
                {
                    String resolverAddress = lookupResolver(contractId);
                    if (!TextUtils.isEmpty(resolverAddress))
                    {
                        byte[] nameHash = NameHash.nameHashAsBytes(contractId);
                        //now attempt to get the address of this ENS
                        contractAddress = getContractData(EthereumNetworkBase.MAINNET_ID, resolverAddress, getAddr(nameHash));
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to execute Ethereum request", e);
            }

            if (!WalletUtils.isValidAddress(contractAddress))
            {
                throw new RuntimeException("Unable to resolve address for name: " + contractId);
            }
            else
            {
                return contractAddress;
            }
        }
        else
        {
            return contractId;
        }
    }

    /**
     * Reverse name resolution as documented in the <a
     * href="https://docs.ens.domains/contract-api-reference/reverseregistrar">specification</a>.
     *
     * @param address an ethereum address, example: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
     * @return a EnsName registered for provided address
     */
    public String reverseResolve(String address)
    {
        String name = null;
        if (WalletUtils.isValidAddress(address))
        {
            String reverseName = Numeric.cleanHexPrefix(address) + REVERSE_NAME_SUFFIX;
            try
            {
                String resolverAddress = lookupResolver(reverseName);
                byte[] nameHash = NameHash.nameHashAsBytes(reverseName);
                name = getContractData(EthereumNetworkBase.MAINNET_ID, resolverAddress, getName(nameHash));
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to execute Ethereum request", e);
            }

            if (!isValidEnsName(name, addressLength))
            {
                throw new RuntimeException("Unable to resolve name for address: " + address);
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

    private String lookupResolver(String ensName) throws Exception
    {
        NetVersion netVersion = web3j.netVersion().send();
        String registryContract = Contracts.resolveRegistryContract(netVersion.getNetVersion());
        byte[] nameHash = NameHash.nameHashAsBytes(ensName);
        Function resolver = getResolver(nameHash);
        return getContractData(EthereumNetworkBase.MAINNET_ID, registryContract, resolver);
    }

    private Function getResolver(byte[] nameHash)
    {
        return new Function("resolver",
                            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Address>()
                            {
                            }));
    }

    private Function getAddr(byte[] nameHash)
    {
        return new Function("addr",
                            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Address>()
                            {
                            }));
    }

    private Function getName(byte[] nameHash)
    {
        return new Function("name",
                            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>()
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
            Function function, String contractAddress, int chainId) throws Exception
    {
        try
        {
            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(TokenscriptFunction.ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = TokenRepository.getWeb3jService(chainId).ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (InterruptedIOException | UnknownHostException e)
        {
            //expected to happen when user switches wallets
            return "0x";
        }
    }

    private <T> T getContractData(int chainId, String address, Function function) throws Exception
    {
        String responseValue = callSmartContractFunction(function, address, chainId);

        if (TextUtils.isEmpty(responseValue))
        {
            throw new Exception("Bad contract value");
        }
        else if (responseValue.equals("0x"))
        {
            return null;
        }

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

    public static boolean isValidEnsName(String input) {
        return isValidEnsName(input, Keys.ADDRESS_LENGTH_IN_HEX);
    }

    public static boolean isValidEnsName(String input, int addressLength) {
        return input != null // will be set to null on new Contract creation
                && (input.contains(".") || !WalletUtils.isValidAddress(input));
    }
}
