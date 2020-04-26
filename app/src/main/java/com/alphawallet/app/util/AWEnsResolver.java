package com.alphawallet.app.util;

import android.text.TextUtils;

import com.alphawallet.app.entity.Wallet;

import org.web3j.protocol.Web3j;

import io.reactivex.Single;

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

    public Single<Wallet> resolveWalletEns(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try
            {
                wallet.ENSname = reverseResolve(wallet.address);
                if (wallet.ENSname != null && wallet.ENSname.length() > 0)
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolve(wallet.ENSname);
                    if (!resolveAddress.equalsIgnoreCase(wallet.address))
                    {
                        wallet.ENSname = null;
                    }
                }
                else
                {
                    wallet.ENSname = null;
                }
            }
            catch (Exception e)
            {
                wallet.ENSname = null;
            }
            return wallet;
        });
    }

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
