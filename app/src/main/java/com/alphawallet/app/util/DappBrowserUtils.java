package com.alphawallet.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.DApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DappBrowserUtils {
    private static final String DAPPS_LIST_FILENAME = "dapps_list.json";
    private static final String MY_DAPPS_FILE = "mydapps";
    private static final String DAPPS_HISTORY_FILE = "dappshistory";

    public static void saveToPrefs(Context context, List<DApp> myDapps) {
        if (context != null)
        {
            //don't store custom dapps
            List<DApp> primaryDapps = getPrimarySites(context);
            Map<String, DApp> dappMap = new HashMap<>();
            for (DApp d : myDapps)
                dappMap.put(d.getUrl(), d);
            for (DApp d : primaryDapps)
                dappMap.remove(d.getUrl());

            String myDappsJson = new Gson().toJson(dappMap.values());
            storeJsonData(MY_DAPPS_FILE, myDappsJson, context);
        }
    }

    private static List<DApp> getPrimarySites(Context context)
    {
        return new ArrayList<>();
    }

    public static List<DApp> getMyDapps(Context context) {
        if (context == null) return new ArrayList<>();
        //load legacy
        String myDappsJson = loadFromPrefsLegacy(context, "my_dapps", MY_DAPPS_FILE);
        myDappsJson = myDappsJson != null ? myDappsJson : loadJsonData(MY_DAPPS_FILE, context);

        List<DApp> dapps = getPrimarySites(context);

        if (!myDappsJson.isEmpty()) {
            dapps.addAll(new Gson().fromJson(myDappsJson, new TypeToken<ArrayList<DApp>>() {
            }.getType()));
        }

        return dapps;
    }

    public static List<DApp> getBrowserHistory(Context context) {
        if (context == null) return new ArrayList<>();
        String historyJson = loadFromPrefsLegacy(context, C.DAPP_BROWSER_HISTORY, DAPPS_HISTORY_FILE); //try legacy data first
        historyJson = historyJson != null ? historyJson : loadJsonData(DAPPS_HISTORY_FILE, context);

        List<DApp> history;
        if (historyJson.isEmpty()) {
            history = new ArrayList<>();
        } else {
            history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<DApp>>() {
            }.getType());
            Collections.reverse(history);
        }
        return history;
    }

    public static void storeJsonData(String fName, String json, Context context)
    {
        File file = new File(context.getFilesDir(), fName);
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            OutputStream os = new BufferedOutputStream(fos);
            os.write(json.getBytes());
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
        if (context != null)
        {
            String dappHistory = new Gson().toJson(history);
            storeJsonData(DAPPS_HISTORY_FILE, dappHistory, context);
        }
    }

    //Legacy data, blanked after first restore
    private static String loadFromPrefsLegacy(Context context, String key, String fileName)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String data = prefs.getString("my_dapps", "");
        if (!TextUtils.isEmpty(data))
        {
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit()
                    .putString(key, "")
                    .apply();

            storeJsonData(fileName, data, context); //move existing data to file
        }
        else
        {
            data = null;
        }

        return data;
    }
}
