package com.alphawallet.app.util;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.UnableToResolveENS;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.web3j.protocol.Web3j;

import java.util.HashMap;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by James on 29/05/2019.
 * Stormbird in Sydney
 */
public class AWEnsResolver extends EnsResolver
{
    static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;
    private final Context context;

    public AWEnsResolver(Web3j web3j, Context context) {
        super(web3j, DEFAULT_SYNC_THRESHOLD);
        this.context = context;
    }

    /**
     * Given an address, find any corresponding ENS name (eg fredblogs.eth)
     * @param address Ethereum address
     * @return ENS name or empty string
     */
    public Single<String> resolveEnsName(String address)
    {
        return Single.fromCallable(() -> {
            String ensName = checkENSHistoryForAddress(address); //First check known ENS names

            try
            {
                if (TextUtils.isEmpty(ensName))
                {
                    ensName = reverseResolve(address); //no known ENS for this address, resolve from reverse resolver
                }
                if (!TextUtils.isEmpty(ensName))
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolve(ensName);
                    if (!resolveAddress.equalsIgnoreCase(address))
                    {
                        ensName = "";
                    }
                }
            }
            catch (UnableToResolveENS resolve)
            {
                ensName = fetchPreviouslyUsedENS(address);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                // no action
            }
            return ensName;
        });
    }

    //Only checks wallet history for ENS name
    //TODO: Check address book for name, once addressbook is implemented
    public String checkENSHistoryForAddress(String address)
    {
        String ensName = "";
        //try previously resolved names
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.ENS_HISTORY_PAIR, "");
        if (historyJson.length() > 0)
        {
            HashMap<String, String> history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>() {}.getType());
            if (history.containsKey(address.toLowerCase()))
            {
                ensName = history.get(address.toLowerCase());
            }
        }

        return ensName;
    }

    private String fetchPreviouslyUsedENS(String address)
    {
        String ensName = "";
        //try previously resolved names
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.ENS_HISTORY_PAIR, "");
        if (historyJson.length() > 0)
        {
            HashMap<String, String> history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>() {}.getType());
            if (history.containsKey(address.toLowerCase()))
            {
                String previouslyUsedDomain = history.get(address.toLowerCase());
                //perform an additional check, to ensure this ENS name is still valid, try this ENS name to see if it resolves to the address
                ensName = resolveENSAddress(previouslyUsedDomain)
                        .map(resolvedAddress -> checkResolvedAddressMatches(resolvedAddress, address, previouslyUsedDomain))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .blockingGet();
            }
        }

        return ensName;
    }

    private String checkResolvedAddressMatches(String resolvedAddress, String address, String previouslyUsedDomain)
    {
        if (resolvedAddress.equalsIgnoreCase(address))
        {
            return previouslyUsedDomain;
        }
        else
        {
            return "";
        }
    }

    /**
     * Given an ENS Name (eg fredblogs.eth), find corresponding Ethereum address
     * @param ensName ensName to be resolved to address
     * @return Ethereum address or empty string
     */
    public Single<String> resolveENSAddress(String ensName)
    {
        return Single.fromCallable(() -> {
            if (BuildConfig.DEBUG) System.out.println("Verify: " + ensName);
            String address = "";
            if (!isValidEnsName(ensName)) return "";
            try
            {
                address = resolve(ensName);
            }
            catch (Exception e)
            {
                System.out.println("Verify: error: " + e.getMessage());
                // no action
            }
            return address;
        });
    }

    public static boolean couldBeENS(String address)
    {
        if (address == null || address.length() == 0) return false;

        String[] split = address.split("[.]");
        if (split.length > 1)
        {
            String extension = split[split.length - 1];
            return extension.length() > 0 && Utils.isAlNum(extension);
        }

        return false;
    }
}
