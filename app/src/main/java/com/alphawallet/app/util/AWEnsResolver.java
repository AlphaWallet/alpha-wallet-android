package com.alphawallet.app.util;

import android.text.TextUtils;

import com.alphawallet.app.entity.Wallet;

import org.web3j.protocol.Web3j;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by James on 29/05/2019.
 * Stormbird in Sydney
 */
public class AWEnsResolver extends EnsResolver
{
    static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;

    public AWEnsResolver(Web3j web3j, long syncThreshold) {
        super(web3j, syncThreshold);
    }

    public AWEnsResolver(Web3j web3j) {
        this(web3j, DEFAULT_SYNC_THRESHOLD);
    }

    /**
     * Given an address, find any corresponding ENS name (eg fredblogs.eth)
     * @param address Ethereum address
     * @return ENS name or empty string
     */
    public Single<String> resolveEnsName(String address)
    {
        return Single.fromCallable(() -> {
            String ensName = "";
            try
            {
                ensName = reverseResolve(address);
                if (!TextUtils.isEmpty(ensName))
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolve(ensName);
                    if (!resolveAddress.equalsIgnoreCase(address))
                    {
                        ensName = "";
                    }
                }
                else
                {
                    ensName = "";
                }
            }
            catch (Exception e)
            {
                // no action
            }
            return ensName;
        });
    }

    /**
     * Given an ENS Name (eg fredblogs.eth), find corresponding Ethereum address
     * @param ensName ensName to be resolved to address
     * @return Ethereum address or empty string
     */
    public Single<String> resolveENSAddress(String ensName)
    {
        return Single.fromCallable(() -> {
            String address = "";
            if (!isValidEnsName(ensName)) return "";
            try
            {
                address = resolve(ensName);
            }
            catch (Exception e)
            {
                // no action
            }
            return address;
        });
    }
}
