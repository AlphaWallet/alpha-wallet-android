package com.alphawallet.app.util;

import static com.alphawallet.app.repository.EthereumNetworkBase.isWithinHomePage;
import static com.alphawallet.app.util.Utils.isValidUrl;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.DApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DappBrowserUtils {
    private static final String DAPPS_LIST_FILENAME = "dapps_list.json";
    private static final String MY_DAPPS_FILE = "mydapps";
    private static final String DAPPS_HISTORY_FILE = "dappshistory";

    //TODO: Move to database
    public static void saveToPrefs(Context context, List<DApp> myDapps) {
        if (context != null)
        {
            //don't store custom dapps
            List<DApp> primaryDapps = getPrimarySites(context);
            Map<String, DApp> dappMap = new HashMap<>();
            for (DApp d : myDapps)
            {
                if (isValidUrl(d.getUrl()))
                    dappMap.put(d.getUrl(), d);
            }
            for (DApp d : primaryDapps)
            {
                dappMap.remove(d.getUrl());
            }

            String myDappsJson = new Gson().toJson(dappMap.values());
            storeJsonData(MY_DAPPS_FILE, myDappsJson, context);
        }
    }

    private static List<DApp> getPrimarySites(Context context)
    {
        return new ArrayList<>();
    }

    //TODO: Move to database
    public static List<DApp> getMyDapps(Context context) {
        if (context == null) return new ArrayList<>();

        String myDappsJson = loadJsonData(MY_DAPPS_FILE, context);
        if (TextUtils.isEmpty(myDappsJson)) myDappsJson = loadFromPrefsLegacy(context, "my_dapps", MY_DAPPS_FILE); //load legacy

        List<DApp> dapps = getPrimarySites(context);

        if (!TextUtils.isEmpty(myDappsJson)) {
            dapps.addAll(new Gson().fromJson(myDappsJson, new TypeToken<ArrayList<DApp>>() {
            }.getType()));
        }

        return dapps;
    }

    //TODO: Remove this from prefs, add to database
    public static List<DApp> getBrowserHistory(Context context) {
        if (context == null) return new ArrayList<>();
        String historyJson = loadJsonData(DAPPS_HISTORY_FILE, context);
        if (TextUtils.isEmpty(historyJson)) blankPrefEntry(context, C.DAPP_BROWSER_HISTORY); //blank legacy

        List<DApp> history;
        if (historyJson.isEmpty()) {
            history = new ArrayList<>();
        } else {
            history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<DApp>>() {
            }.getType());
        }

        int historyLen = history.size();

        history = parseDappHistory(history);

        if (historyLen != history.size())
        {
            String dappHistory = new Gson().toJson(history);
            storeJsonData(DAPPS_HISTORY_FILE, dappHistory, context);
        }

        return history;
    }

    private static List<DApp> parseDappHistory(List<DApp> history)
    {
        boolean requireRefresh = false;
        List<DApp> recodeHistory = new ArrayList<>();
        for (DApp dapp : history)
        {
            if (TextUtils.isEmpty(dapp.getUrl())) { continue; }
            if (isValidUrl(dapp.getUrl()))
            {
                recodeHistory.add(dapp);
            }
            else
            {
                requireRefresh = true;
            }
        }

        if (requireRefresh)
        {
            return recodeHistory;
        }
        else
        {
            return history;
        }
    }

    private static void storeJsonData(String fName, String json, Context context)
    {
        File file = new File(context.getFilesDir(), fName);
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(json);
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            //
        }
    }

    public static String loadJsonData(String fName, Context context)
    {
        StringBuilder sb = new StringBuilder();
        File file = new File(context.getFilesDir(), fName);
        try (FileInputStream fis = new FileInputStream(file))
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        }
        catch (Exception e)
        {
            //
        }

        return sb.toString();
    }

    public static void clearHistory(Context context) {
        if (context != null)
        {
            storeJsonData(DAPPS_HISTORY_FILE, "", context);
        }
    }

    public static void addToHistory(Context context, DApp dapp) {
        if (dapp == null || isWithinHomePage(dapp.getUrl())) return;
        List<DApp> history = new ArrayList<>(Collections.singletonList(dapp));
        history.addAll(getBrowserHistory(context));
        if (history.size() > 1)
        {
            for (int i = 1; i < history.size(); i++)
            {
                if (history.get(i).getUrl().equals(dapp.getUrl()))
                {
                    history.remove(i); //remove older item
                    break;
                }
            }
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
        return "https://www.google.com/s2/favicons?sz=128&domain=" + url;
    }

    public static List<DApp> getDappsList(Context context) {
        ArrayList<DApp> dapps;
        dapps = new Gson().fromJson(Utils.loadJSONFromAsset(context, DAPPS_LIST_FILENAME),
                new TypeToken<List<DApp>>() {
                }.getType());
        return dapps;
    }

    private static void saveHistory(Context context, List<DApp> history) {
        if (context != null)
        {
            String dappHistory = new Gson().toJson(history);
            storeJsonData(DAPPS_HISTORY_FILE, dappHistory, context);
        }
    }

    //Legacy data, blanked after first restore. TODO: Remove after a few version updates
    private static String loadFromPrefsLegacy(Context context, String key, String fileName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String data = prefs.getString(key, "");
        if (!TextUtils.isEmpty(data))
        {
            blankPrefEntry(context, key);
            storeJsonData(fileName, data, context); //move existing data to file
        }
        else
        {
            data = null;
        }

        return data;
    }

    //TODO: Remove after a few version updates
    private static void blankPrefEntry(Context context, String key)
    {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(key, "")
                .apply();
    }
}
