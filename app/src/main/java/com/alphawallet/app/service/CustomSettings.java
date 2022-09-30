package com.alphawallet.app.service;

import static com.alphawallet.app.repository.TokensRealmSource.IMAGES_DB;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import io.realm.Realm;

public class CustomSettings
{
    public final String CUSTOM_SETTINGS_FILENAME = "custom_view_settings.json";
    public static final long primaryChain = MAINNET_ID;
    private final Context context;

    public CustomSettings(Context ctx)
    {
        context = ctx;
    }

    public ArrayList<Long> getChainsFromJsonFile(String chainName)
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
                }
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return chains;
    }

    public ArrayList<TokenInfo> getLockedTokensFromJsonFile(String chainName)
    {
        ArrayList<TokenInfo> chains = new ArrayList<>();
        try
        {
            String lockedTokens = loadJSONStringFromAsset();
            if (lockedTokens != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedTokens);
                JSONArray chainsArray = customSettingsJsonObject.getJSONArray(chainName);
                if (chainsArray.length() > 0)
                {
                    for (int i = 0; i < chainsArray.length(); i++)
                    {
                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        String tokenAddress = chainObject.getString("tokenAddress");
                        String tokenName = chainObject.getString("tokenName");
                        String tokenSymbol = chainObject.getString("tokenSymbol");
                        int tokenDecimals = chainObject.getInt("tokenDecimals");
                        boolean isEnabled = chainObject.getBoolean("isEnabled");
                        long chainId = chainObject.getLong("chainId");
                        TokenInfo tokenInfo = new TokenInfo(tokenAddress, tokenName, tokenSymbol, tokenDecimals, isEnabled, chainId);
                        chains.add(tokenInfo);
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


    public JSONArray getChainsArrayJsonFile(String chainName)
    {
        JSONArray chainsArray = new JSONArray();
        try
        {
            String lockedChains = loadJSONStringFromAsset();
            JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
            chainsArray = customSettingsJsonObject.getJSONArray(chainName);
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }

        return chainsArray;
    }

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

    public Boolean alwaysShow(long chainId)
    {
        ArrayList<Long> exclusiveChains = getChainsFromJsonFile("exclusive_chains");
        return exclusiveChains.contains(chainId);
    }

    public Boolean tokenCanBeDisplayed(TokenCardMeta token)
    {
        return token.type == ContractType.ETHEREUM || token.isEnabled || isLockedToken(token.getChain(), token.getAddress());
    }

    public Boolean isLockedToken(long chainId, String contractAddress)
    {
        ArrayList<TokenInfo> lockedTokens = getLockedTokensFromJsonFile("locked_tokens");
        for (TokenInfo tInfo : lockedTokens)
        {
            if (tInfo.chainId == chainId && tInfo.address.equalsIgnoreCase(contractAddress))
                return true;
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
    public boolean inputAmountFiatDefault()
    {
        return false;
    }
}
