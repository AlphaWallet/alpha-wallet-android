package com.alphawallet.app.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.UnableToResolveENS;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.util.das.DASBody;
import com.alphawallet.app.util.das.DASRecord;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.ens.NameHash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.Arrays;
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
public class AWEnsResolver extends com.alphawallet.app.web3j.ens.EnsResolver
{
    private static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;
    private static final String DAS_LOOKUP = "https://indexer.da.systems/";
    private static final String DAS_NAME = "[DAS_NAME]";
    private static final String DAS_PAYLOAD = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"das_searchAccount\",\"params\":[\"" + DAS_NAME + "\"]}";
    private static final String OPENSEA_IMAGE_PREVIEW = "image_preview_url";
    private static final String OPENSEA_IMAGE_ORIGINAL = "image_original_url"; //in case of SVG; Opensea breaks SVG compression
    public static final String CRYPTO_RESOLVER = "0xD1E5b0FF1287aA9f9A268759062E4Ab08b9Dacbe";
    public static final String CRYPTO_ETH_KEY = "crypto.ETH.address";
    private final Context context;
    private final OkHttpClient client;
    
    public AWEnsResolver(Web3j web3j, Context context)
    {
        super(web3j);
        this.context = context;
        this.client = setupClient();
    }

    public AWEnsResolver(Web3j web3j)
    {
        super(web3j);
        this.context = null;
        this.client = setupClient();
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
        if (context == null) return ensName;
        //try previously resolved names
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.ENS_HISTORY_PAIR, "");
        if (historyJson.length() > 0)
        {
            HashMap<String, String> history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>()
            {
            }.getType());
            if (history.containsKey(address.toLowerCase()))
            {
                String previouslyUsedDomain = history.get(address.toLowerCase());
                //perform an additional check, to ensure this ENS name is still valid, try this ENS name to see if it resolves to the address
                ensName = resolveENSAddress(previouslyUsedDomain, true)
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
    public Single<String> resolveENSAddress(String ensName, boolean performNodeSync)
    {
        return Single.fromCallable(() ->
        {
            Timber.d("Verify: %s", ensName);
            String address = "";
            if (!isValidEnsName(ensName)) return "";
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

    @Override
    public String resolve(String ensName) throws Exception
    {
        if (TextUtils.isEmpty(ensName))
        {
            return "";
        }
        if (ensName.endsWith(".bit"))
        {
            return resolveDAS(ensName);
        }
        else if (ensName.endsWith(".crypto")) //check crypto namespace
        {
            byte[] nameHash = NameHash.nameHashAsBytes(ensName);
            BigInteger nameId = new BigInteger(nameHash);
            String resolverAddress = getContractData(CRYPTO_RESOLVER, getResolverOf(nameId), "");
            if (!TextUtils.isEmpty(resolverAddress))
            {
                return getContractData(resolverAddress, get(nameId), "");
            }
            else
            {
                return "";
            }
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
            Timber.tag("ENS").e(e);
        }

        return "";
    }

    private Function getResolverOf(BigInteger nameId)
    {
        return new Function("resolverOf",
                Arrays.asList(new org.web3j.abi.datatypes.Uint(nameId)),
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

    private Function getResolver(byte[] nameHash)
    {
        return new Function("resolver",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(nameHash)),
                Arrays.asList(new TypeReference<Address>()
                {
                }));
    }

    private Function get(BigInteger nameId)
    {
        return new Function("get",
                Arrays.asList(new org.web3j.abi.datatypes.Utf8String(CRYPTO_ETH_KEY), new org.web3j.abi.datatypes.generated.Uint256(nameId)),
                Arrays.asList(new TypeReference<Utf8String>()
                {
                }));
    }

    public String resolveAvatar(String ensName)
    {
        if (isValidEnsName(ensName, addressLength))
        {
            try
            {
                String resolverAddress = getResolverAddress(ensName);
                if (!TextUtils.isEmpty(resolverAddress))
                {
                    byte[] nameHash = NameHash.nameHashAsBytes(ensName);
                    //now attempt to get the address of this ENS
                    return getContractData(resolverAddress, getAvatar(nameHash), "");
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
            try
            {
                String ensName = reverseResolve(address);
                return resolveAvatar(ensName);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            /*String reverseName = org.web3j.utils.Numeric.cleanHexPrefix(address.toLowerCase()) + REVERSE_NAME_SUFFIX;
            try
            {
                String resolverAddress = getResolverAddress(reverseName);
                byte[] nameHash = NameHash.nameHashAsBytes(reverseName);
                String avatar = getContractData(resolverAddress, getAvatar(nameHash), "");
                return avatar != null ? avatar : "";
            }
            catch (Exception e)
            {
                Timber.e(e);
                //throw new RuntimeException("Unable to execute Ethereum request", e);
            }*/
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
