package com.alphawallet.app.service;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CustomSettings
{
    public final String CUSTOM_SETTINGS_FILENAME = "custom_view_settings.json";
    public static final long primaryChain = MAINNET_ID;
    private final Context context;
    ConcurrentLinkedQueue<Long> loadLockedCachedChains = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Long> loadExclusiveCachedChains = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<TokenInfo> loadLockedTokens = new ConcurrentLinkedQueue<>();

    public CustomSettings(Context ctx)
    {
        context = ctx;
    }

    public ArrayList<Long> loadChains(String chainName)
    {
        ArrayList<Long> chains = new ArrayList<>();
        try
        {
            String lockedChains = loadJSONStringFromAsset();
            if (lockedChains != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
                JSONArray chainsArray = customSettingsJsonObject.getJSONArray(chainName);
                if (chainsArray.length() > 0)
                {
                    for (int i = 0; i < chainsArray.length(); i++)
                    {
                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        Long chain = chainObject.getLong("chain");
                        chains.add(chain);
                    }
                    if (chainName.equals("locked_chains"))
                    {
                        loadLockedCachedChains.clear();
                        loadLockedCachedChains.addAll(chains);
                    }
                    else
                    {
                        loadExclusiveCachedChains.clear();
                        loadExclusiveCachedChains.addAll(chains);
                    }
                }
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }

        return chains;
    }

    //TODO: Cache locked chains & Tokens in an ConcurrentLinkedQueue / ConcurrentHashMap
    //    : The approach I would take is to have a 'load' function which first checks if there's already
    //    : data in the two cache mappings, if there's not then check the 'loaded' flag. If that is
    //    : set false then load and populate the Queue/Map
    //    : then, just use the mappings from there. Add a call to the load function each time you use the data.
    //    : Android can memory scavenge those mappings at any time if the wallet is paged out,
    //    : Then they'll be empty when the wallet is paged back in.

    public ArrayList<Long> getChainsFromJsonFile() //<-- TODO: chainName is redundant
    {
        ArrayList<Long> chains = new ArrayList<>();
        if (loadLockedCachedChains.size() > 0)
        {
            chains.addAll(loadLockedCachedChains);
        }
        else
        {
            chains.addAll(loadChains("locked_chains"));
        }
        return chains;
    }

    public ArrayList<TokenInfo> getLockedTokensFromJsonFile() //<-- TODO: chainName is redundant
    {
        ArrayList<TokenInfo> chains = new ArrayList<>();
        Gson gson = new Gson();
        try
        {
            String lockedTokens = loadJSONStringFromAsset();
            if (lockedTokens != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedTokens);
                JSONArray chainsArray = customSettingsJsonObject.getJSONArray("locked_tokens");
                if (chainsArray.length() > 0)
                {
                    for (int i = 0; i < chainsArray.length(); i++)
                    {
                        //TODO: use GSON class array load (see "private EtherscanTransaction[] getEtherscanTransactions" for how-to)
                        //    : this will need a static class within this class
                        //    : you can then traverse (for x : y) that list and have cleaner code
                        //    : esp if you add a getTokenInfo from that static internal class.

                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        TokenInfo[] lockedTokenInfo = gson.fromJson(chainObject.toString(), TokenInfo[].class);
                        for (TokenInfo tokenInfo : lockedTokenInfo)
                        {
                            chains.add(tokenInfo);
                        }
                    }
                }
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return chains;
    }

    public JSONArray getChainsArrayJsonFile() //<--- TODO: Redundant
    {
        JSONArray chainsArray = new JSONArray();
        try
        {
            String lockedChains = loadJSONStringFromAsset();
            JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
            chainsArray = customSettingsJsonObject.getJSONArray("locked_chains");
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }

        return chainsArray;
    }

    //TODO: Doesn't need caching (no action)
    public Boolean getDarkModeValueFromJsonFile(String chainName)
    {
        boolean darkModeValue = false;
        try
        {
            String darkMode = loadJSONStringFromAsset();
            if (darkMode != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(darkMode);
                darkModeValue = customSettingsJsonObject.getBoolean(chainName);
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return darkModeValue;
    }

    public String loadJSONStringFromAsset()
    {
        String returnString;
        try
        {
            Reader reader = new InputStreamReader(context.getAssets().open(CUSTOM_SETTINGS_FILENAME));
            JsonElement json = new Gson().fromJson(reader, JsonElement.class);
            returnString = json.toString();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
        return returnString;
    }

    //TODO: Caching
    public Boolean alwaysShow(long chainId)
    {
        ArrayList<Long> exclusiveChains = new ArrayList<>();
        if (loadExclusiveCachedChains.size() > 0)
        {
            exclusiveChains.addAll(loadExclusiveCachedChains);
        }
        else
        {
            exclusiveChains.addAll(loadChains("exclusive_chains"));
        }
        return exclusiveChains.contains(chainId);
    }

    //TODO: Requires caching, since this will be called very frequently
    //    : Use a (final) mapping of locked tokens, from a load.
    //    : You'll need to check if the list is empty and if so flag a 'loaded', so we don't spam this list
    public Boolean tokenCanBeDisplayed(TokenCardMeta token)
    {
        return token.type == ContractType.ETHEREUM || token.isEnabled || isLockedToken(token.getChain(), token.getAddress());
    }

    //TODO: Caching
    public Boolean isLockedToken(long chainId, String contractAddress)
    {

        if (loadLockedTokens.size() > 0)
        {
            return true;
        }
        else
        {
            ArrayList<TokenInfo> lockedTokens = getLockedTokensFromJsonFile();
            loadLockedTokens.clear();
            loadLockedTokens.addAll(lockedTokens);
            for (TokenInfo tInfo : lockedTokens)
            {
                if (tInfo.chainId == chainId && tInfo.address.equalsIgnoreCase(contractAddress))
                    return true;
            }
        }

        return false;
    }

    public ContractType checkKnownTokens(TokenInfo tokenInfo)
    {
        return ContractType.OTHER;
    }

    public boolean showContractAddress(Token token)
    {
        return true;
    }

    public long startupDelay()
    {
        return 0;
    }

    public int getImageOverride()
    {
        return 0;
    }

    //Switch off dapp browser
    public boolean hideDappBrowser()
    {
        return false;
    }

    //Hides the filter tab bar at the top of the wallet screen (ALL/CURRENCY/COLLECTIBLES)
    public boolean hideTabBar()
    {
        return false;
    }

    //Use to switch off direct transfer, only use magiclink transfer
    public boolean hasDirectTransfer()
    {
        return true;
    }

    //Allow multiple wallets (true) or single wallet mode (false)
    public boolean canChangeWallets()
    {
        return true;
    }

    //Hide EIP681 generation (Payment request, generates a QR code another wallet user can scan to have all payment fields filled in)
    public boolean hideEIP681()
    {
        return false;
    }

    //In main wallet menu, if wallet allows adding new tokens
    public boolean canAddTokens()
    {
        return true;
    }

    //Implement minimal dappbrowser with no URL bar. You may want this if you want your browser to point to a specific website and only
    // allow navigation within that website
    // use this setting in conjunction with changing DEFAULT_HOMEPAGE in class EthereumNetworkBase
    public static boolean minimiseBrowserURLBar()
    {
        return false;
    }

    //Allow showing token management view
    public boolean showManageTokens()
    {
        return true;
    }

    //Show all networks in Select Network screen. Set to `true` to show only filtered networks.
    public boolean showAllNetworks()
    {
        return false;
    }

    public String getDecimalFormat()
    {
        return "0.####E0";
    }

    public int getDecimalPlaces()
    {
        return 5;
    }

    //set if the Input Amount defaults to Fiat or Crypto
    public static boolean inputAmountFiatDefault()
    {
        return false;
    }
}
