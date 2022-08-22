package com.alphawallet.app.util.ens;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.UnableToResolveENS;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Created by James on 29/05/2019.
 * Stormbird in Sydney
 */
public class AWEnsResolver
{
    private static final String OPENSEA_IMAGE_PREVIEW = "image_preview_url";
    private static final String OPENSEA_IMAGE_ORIGINAL = "image_original_url"; //in case of SVG; Opensea breaks SVG compression
    private final Context context;
    private final OkHttpClient client;
    private HashMap<String, Resolvable> resolvables;
    private final EnsResolver ensResolver;

    public AWEnsResolver(Web3j web3j, Context context)
    {
        this.ensResolver = new EnsResolver(web3j);
        this.context = context;
        this.client = setupClient();

        resolvables = new HashMap<String, Resolvable>() {
            {
                put(".bit", new DASResolver(client));
                put(".crypto", new CryptoResolver(ensResolver));
            }
        };
    }

    /**
     * Given an address, find any corresponding ENS name (eg fredblogs.eth)
     *
     * @param address Ethereum address
     * @return ENS name or empty string
     */
    public Single<String> reverseResolveEns(String address)
    {
        return Single.fromCallable(() ->
        {
            String ensName = "";

            try
            {
                ensName = ensResolver.reverseResolve(address); //no known ENS for this address, resolve from reverse resolver
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
                Timber.e(e);
                // no action
            }
            return ensName;
        }).onErrorReturnItem("");
    }

    public Single<String> getENSUrl(String ensName)
    {
        return Single.fromCallable(() ->
        {
            try
            {
                if (TextUtils.isEmpty(ensName))
                {
                    return "";
                }
                else if (Utils.isAddressValid(ensName))
                {
                    return resolveAvatarFromAddress(ensName);
                }
                else
                {
                    return resolveAvatar(ensName);
                }
            }
            catch (Exception e)
            {
                return "";
            }
        }).flatMap(this::convertLocator);
    }

    public Single<String> convertLocator(String locator)
    {
        if (TextUtils.isEmpty(locator)) return Single.fromCallable(() -> "");
        return Single.fromCallable(() ->
        {
            String url = "";
            switch (getLocatorType(locator))
            {
                case EIP155:
                    url = getEip155Url(locator);
                    break;
                case IPFS:
                    url = Utils.parseIPFS(locator);
                    break;
                case HTTPS:
                    url = locator;
                    break;
                case UNKNOWN:
                    url = "";
                    break;
            }

            return url;
        });
    }

    private String getEip155Url(String locator)
    {
        final Pattern findKey = Pattern.compile("(eip155:)([0-9]+)(\\/)([0-9a-zA-Z]+)(:)(0?x?[0-9a-fA-F]{40})(\\/)([0-9]+)");
        final Matcher matcher = findKey.matcher(locator);

        try
        {
            if (matcher.find())
            {
                long chainId = Long.parseLong(Objects.requireNonNull(matcher.group(2)));
                String tokenAddress = Numeric.prependHexPrefix(matcher.group(6));
                String tokenId = matcher.group(8);

                String asset = new OpenSeaService().fetchAsset(chainId, tokenAddress, tokenId);
                JSONObject assetObj = new JSONObject(asset);
                String url = assetObj.getString(OPENSEA_IMAGE_PREVIEW);
                if (!TextUtils.isEmpty(url) && url.endsWith(".svg"))
                {
                    String original = assetObj.getString(OPENSEA_IMAGE_ORIGINAL);
                    if (!TextUtils.isEmpty(original)) url = original;
                }
                return url;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return "";
    }

    private LocatorType getLocatorType(String locator)
    {
        String[] sp = locator.split(":");
        if (sp.length > 1)
        {
            switch (sp[0])
            {
                case "eip155":
                    return LocatorType.EIP155;
                case "ipfs":
                    return LocatorType.IPFS;
                case "https":
                    return LocatorType.HTTPS;
                default:
                    break;
            }
        }

        return LocatorType.UNKNOWN;
    }

    private enum LocatorType
    {
        EIP155, IPFS, HTTPS, UNKNOWN
    }

    //Only checks wallet history for ENS name
    //TODO: Check address book for name, once addressbook is implemented
    public String checkENSHistoryForAddress(String address)
    {
        String ensName = "";
        if (context == null) return ensName;
        //try previously resolved names
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.ENS_HISTORY_PAIR, "");
        if (historyJson.length() > 0)
        {
            HashMap<String, String> history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>()
            {
            }.getType());
            if (history.containsKey(address.toLowerCase(Locale.ENGLISH)))
            {
                ensName = history.get(address.toLowerCase(Locale.ENGLISH));
            }
        }

        return ensName;
    }

    private String fetchPreviouslyUsedENS(String address)
    {
        String ensName = "";
        if (context == null) return ensName;
        //try previously resolved names
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.ENS_HISTORY_PAIR, "");
        if (historyJson.length() > 0)
        {
            HashMap<String, String> history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>()
            {
            }.getType());
            if (history.containsKey(address.toLowerCase(Locale.ENGLISH)))
            {
                String previouslyUsedDomain = history.get(address.toLowerCase(Locale.ENGLISH));
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
     *
     * @param ensName ensName to be resolved to address
     * @return Ethereum address or empty string
     */
    public Single<String> resolveENSAddress(String ensName)
    {
        return Single.fromCallable(() ->
        {
            Timber.d("Verify: %s", ensName);
            String address = "";
            if (!EnsResolver.isValidEnsName(ensName)) return "";
            try
            {
                address = resolve(ensName);
            }
            catch (Exception e)
            {
                Timber.d("Verify: error: %s", e.getMessage());
                // no action
            }
            return address;
        }).onErrorReturnItem("");
    }

    public String resolve(String ensName) throws Exception
    {
        if (TextUtils.isEmpty(ensName))
        {
            return "";
        }

        Resolvable resolvable = resolvables.get(suffixOf(ensName));
        if (resolvable == null)
        {
            resolvable = ensResolver;
        }
        return resolvable.resolve(ensName);
    }

    private String suffixOf(String ensName)
    {
        return ensName.substring(ensName.lastIndexOf("."));
    }

    public String resolveAvatar(String ensName)
    {
        return new AvatarResolver(ensResolver).resolve(ensName);
    }

    public String resolveAvatarFromAddress(String address)
    {
        if (Utils.isAddressValid(address))
        {
            try
            {
                String ensName = ensResolver.reverseResolve(address);
                return resolveAvatar(ensName);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        return "";
    }

    private OkHttpClient setupClient()
    {
        return new OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }
}
