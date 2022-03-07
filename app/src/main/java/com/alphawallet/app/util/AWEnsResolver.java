package com.alphawallet.app.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.UnableToResolveENS;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.util.das.DASBody;
import com.alphawallet.app.util.das.DASRecord;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import timber.log.Timber;

/**
 * Created by James on 29/05/2019.
 * Stormbird in Sydney
 */
public class AWEnsResolver extends EnsResolver
{
    private static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;
    private static final String DAS_LOOKUP = "https://indexer.da.systems/";
    private static final String DAS_NAME = "[DAS_NAME]";
    private static final String DAS_PAYLOAD = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"das_searchAccount\",\"params\":[\"" + DAS_NAME + "\"]}";
    private static final String OPENSEA_IMAGE_PREVIEW = "image_preview_url";
    private final Context context;
    private final OkHttpClient client;

    static {
        System.loadLibrary("keys");
    }

    public static native String getOpenSeaKey();

    public AWEnsResolver(Web3j web3j, Context context) {
        super(web3j, DEFAULT_SYNC_THRESHOLD);
        this.context = context;
        this.client = setupClient();
    }

    /**
     * Given an address, find any corresponding ENS name (eg fredblogs.eth)
     * @param address Ethereum address
     * @return ENS name or empty string
     */
    public Single<String> reverseResolveEns(String address)
    {
        return Single.fromCallable(() -> {
            String ensName = "";

            try
            {
                ensName = reverseResolve(address); //no known ENS for this address, resolve from reverse resolver
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
        return Single.fromCallable(() -> {
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
        return Single.fromCallable(() -> {
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

                JSONObject asset = fetchOpenseaAsset(chainId, tokenAddress, tokenId);

                if (asset != null && asset.has(OPENSEA_IMAGE_PREVIEW))
                {
                    return asset.getString(OPENSEA_IMAGE_PREVIEW);
                }
                else
                {
                    //TODO: fetch metadata directly
                    return "";
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return "";
    }

    private JSONObject fetchOpenseaAsset(long chainId, String tokenAddress, String tokenId)
    {
        String apiBase = OpenSeaService.apiMap.get(chainId);
        if (apiBase == null) return null;
        apiBase = apiBase.substring(0, apiBase.lastIndexOf("asset") + 5);

        Request.Builder requestB = new Request.Builder()
                    .url(apiBase + "/" + tokenAddress + "/" + tokenId)
                    .get();

        String apiKey = getOpenSeaKey();
        if (!TextUtils.isEmpty(apiKey) && !apiKey.equals("..."))
        {
            requestB.addHeader("X-API-KEY", apiKey);
        }

        try (okhttp3.Response response = client.newCall(requestB.build()).execute())
        {
            String jsonResult = response.body().string();
            return new JSONObject(jsonResult);
        }
        catch (InterruptedIOException e)
        {
            //If user switches account or network during a fetch
            //this exception is going to be thrown because we're terminating the API call
            //Don't display error
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return null;
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
            Timber.d("Verify: " + ensName);
            String address = "";
            if (!isValidEnsName(ensName)) return "";
            try
            {
                address = resolve(ensName);
            }
            catch (Exception e)
            {
                Timber.d("Verify: error: " + e.getMessage());
                // no action
            }
            return address;
        }).onErrorReturnItem("");
    }

    @Override
    public String resolve(String ensName)
    {
        if (!TextUtils.isEmpty(ensName) && ensName.endsWith(".bit"))
        {
            return resolveDAS(ensName);
        }
        else
        {
            return super.resolve(ensName);
        }
    }

    private String resolveDAS(String ensName)
    {
        String payload = DAS_PAYLOAD.replace(DAS_NAME, ensName);

        RequestBody requestBody = RequestBody.create(payload, HttpService.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                    .url(DAS_LOOKUP)
                    .post(requestBody)
                    .build();

        try (okhttp3.Response response = client.newCall(request).execute())
        {
            //get result
            String result = response.body() != null ? response.body().string() : "";

            DASBody dasResult = new Gson().fromJson(result, DASBody.class);
            dasResult.buildMap();

            //find ethereum entry
            DASRecord ethLookup = dasResult.records.get("address.eth");
            if (ethLookup != null)
            {
                return ethLookup.getAddress();
            }
            else
            {
                return dasResult.getEthOwner();
            }
        }
        catch (Exception e)
        {
            Timber.tag("ENS").d(e.getMessage());
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
