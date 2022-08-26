package com.alphawallet.app.util.ens;

import android.text.TextUtils;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.ens.NameHash;

import java.util.Arrays;

import timber.log.Timber;

public class AvatarResolver implements Resolvable
{
    private final EnsResolver ensResolver;

    public AvatarResolver(EnsResolver ensResolver)
    {
        this.ensResolver = ensResolver;
    }

    public String resolve(String ensName)
    {
        if (ensResolver.validate(ensName))
        {
            try
            {
                String resolverAddress = ensResolver.getResolverAddress(ensName);
                if (!TextUtils.isEmpty(resolverAddress))
                {
                    byte[] nameHash = NameHash.nameHashAsBytes(ensName);
                    //now attempt to get the address of this ENS
                    return ensResolver.getContractData(resolverAddress, getAvatar(nameHash), "");
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

    private Function getAvatar(byte[] nameHash)
    {
        return new Function("text",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash),
                        new org.web3j.abi.datatypes.Utf8String("avatar")),
                Arrays.asList(new TypeReference<Utf8String>()
                {
                }));
    }
}
