package com.alphawallet.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alphawallet.app.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.DApp;

public class DappBrowserUtils {
    private static final String DAPPS_LIST_FILENAME = "dapps_list.json";

    public static void saveToPrefs(Context context, List<DApp> myDapps) {
        //don't store custom dapps
        List<DApp> primaryDapps = getPrimarySites(context);
        Map<String, DApp> dappMap = new HashMap<>();
        for (DApp d : myDapps) dappMap.put(d.getUrl(), d);
        for (DApp d : primaryDapps) dappMap.remove(d.getUrl());

        String myDappsJson = new Gson().toJson(dappMap.values());
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString("my_dapps", myDappsJson)
                .apply();
    }

    private static List<DApp> getPrimarySites(Context context)
    {
        StringBuilder myDappsJson = new StringBuilder("[");
        String[] customItems = context.getResources().getStringArray(R.array.PrimarySites);
        boolean firstItem = true;

        for (String s : customItems)
        {
            if (!firstItem) myDappsJson.append(",");
            myDappsJson.append(s);
            firstItem = false;
        }

        myDappsJson.append("]");

        if (myDappsJson.length() > 0) {
            return new Gson().fromJson(myDappsJson.toString(), new TypeToken<ArrayList<DApp>>() {
            }.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DApp> getMyDapps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String myDappsJson = prefs.getString("my_dapps", "");

        List<DApp> dapps = getPrimarySites(context);

        if (!myDappsJson.isEmpty()) {
            dapps.addAll(new Gson().fromJson(myDappsJson, new TypeToken<ArrayList<DApp>>() {
            }.getType()));
        }

        return dapps;
    }

    public static List<DApp> getBrowserHistory(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String historyJson = prefs.getString(C.DAPP_BROWSER_HISTORY, "");

        List<DApp> history;
        if (historyJson.isEmpty()) {
            history = new ArrayList<>();
        } else {
            history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<DApp>>() {
            }.getType());
        }
        return history;
    }

    public static void clearHistory(Context context) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(C.DAPP_BROWSER_HISTORY, "")
                .apply();
    }

    public static void addToHistory(Context context, DApp dapp) {
        List<DApp> history = getBrowserHistory(context);
        if (!history.contains(dapp)) {
            history.add(dapp);
        }
        saveHistory(context, history);
    }

    public static void removeFromHistory(Context context, DApp dapp) {
        List<DApp> history = getBrowserHistory(context);

        for (DApp d : history) {
            if (d.getName().equals(dapp.getName())) {
                history.remove(d);
                break;
            }
        }

        saveHistory(context, history);
    }

    public static String getIconUrl(String url) {
        return "https://api.faviconkit.com/" + url + "/144";
    }

    public static List<DApp> getDappsList(Context context) {
        ArrayList<DApp> dapps;
        dapps = new Gson().fromJson(Utils.loadJSONFromAsset(context, DAPPS_LIST_FILENAME),
                new TypeToken<List<DApp>>() {
                }.getType());
        return dapps;
    }

    private static void saveHistory(Context context, List<DApp> history) {
        String myDappsJson = new Gson().toJson(history);
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(C.DAPP_BROWSER_HISTORY, myDappsJson)
                .apply();
    }
}
