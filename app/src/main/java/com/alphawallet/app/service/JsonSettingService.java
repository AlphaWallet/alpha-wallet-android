package com.alphawallet.app.service;

import android.content.Context;

import com.alphawallet.app.entity.ContractType;
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

public class JsonSettingService
{
    public static final String CUSTOM_SETTINGS_FILENAME = "custom_view_settings.json";
    private final Context context;
    private final String chainName;

    public JsonSettingService(Context ctx, String chainName){
        context = ctx;
        this.chainName = chainName;
        loadJsonSetting();
    }

    private void loadJsonSetting(){

        getChainsFromJsonFile(chainName);
        getLockedTokensFromJsonFile(chainName);
        getDarkModeValueFromJsonFile(chainName);
    }


    public ArrayList<Long> getChainsFromJsonFile(String chainName)
    {
        ArrayList<Long> chains = new ArrayList<>();
        try {
            String lockedChains = loadJSONStringFromAsset(CUSTOM_SETTINGS_FILENAME);
            if(lockedChains != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
                JSONArray chainsArray = customSettingsJsonObject.getJSONArray(chainName);
                if (chainsArray.length() > 0)
                {
                    for(int i=0; i < chainsArray.length(); i++)
                    {
                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        Long chain = chainObject.getLong("chain");
                        chains.add(chain);
                    }
                }
            }
        }catch (JSONException err){
            err.printStackTrace();
        }

        return chains;
    }


    public ArrayList<TokenInfo> getLockedTokensFromJsonFile(String chainName)
    {
        ArrayList<TokenInfo> chains = new ArrayList<>();
        try {
            String lockedTokens = loadJSONStringFromAsset(CUSTOM_SETTINGS_FILENAME);
            if(lockedTokens != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedTokens);
                JSONArray chainsArray = customSettingsJsonObject.getJSONArray(chainName);
                if (chainsArray.length() > 0)
                {
                    for(int i=0; i < chainsArray.length(); i++)
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

        }catch (JSONException err){
            err.printStackTrace();
        }

        return chains;
    }

    public Boolean getDarkModeValueFromJsonFile(String chainName)
    {
        boolean darkModeValue = false;
        try {
            String darkMode = loadJSONStringFromAsset(CUSTOM_SETTINGS_FILENAME);
            if(darkMode != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(darkMode);
                darkModeValue = customSettingsJsonObject.getBoolean(chainName);
            }


        }catch (JSONException err){
            err.printStackTrace();
        }
        return darkModeValue;
    }

    public  String loadJSONStringFromAsset(String fileName) {
        String returnString = null;
        try{
            Reader reader= new InputStreamReader(context.getAssets().open(fileName));
            JsonElement json=new Gson().fromJson(reader,JsonElement.class);
            returnString = json.toString();
        }catch (IOException ex){
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
            if (tInfo.chainId == chainId && tInfo.address.equalsIgnoreCase(contractAddress)) return true;
        }

        return false;
    }
}
