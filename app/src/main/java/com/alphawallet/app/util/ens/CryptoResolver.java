package com.alphawallet.app.util.ens;

import android.text.TextUtils;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.ens.NameHash;

import java.math.BigInteger;
import java.util.Arrays;

public class CryptoResolver implements Resolvable
{
    private static final String CRYPTO_RESOLVER = "0xD1E5b0FF1287aA9f9A268759062E4Ab08b9Dacbe";
    private static final String CRYPTO_ETH_KEY = "crypto.ETH.address";

    private final EnsResolver ensResolver;

    public CryptoResolver(EnsResolver ensResolver)
    {
        this.ensResolver = ensResolver;
    }

    public String resolve(String ensName) throws Exception
    {
        byte[] nameHash = NameHash.nameHashAsBytes(ensName);
        BigInteger nameId = new BigInteger(nameHash);
        String resolverAddress = ensResolver.getContractData(CRYPTO_RESOLVER, getResolverOf(nameId), "");
        if (!TextUtils.isEmpty(resolverAddress))
        {
            return ensResolver.getContractData(resolverAddress, get(nameId), "");
        }
        else
        {
            return "";
        }
    }

    private Function get(BigInteger nameId)
    {
        return new Function("get",
                Arrays.asList(new org.web3j.abi.datatypes.Utf8String(CRYPTO_ETH_KEY), new org.web3j.abi.datatypes.generated.Uint256(nameId)),
                Arrays.asList(new TypeReference<Utf8String>()
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
}
