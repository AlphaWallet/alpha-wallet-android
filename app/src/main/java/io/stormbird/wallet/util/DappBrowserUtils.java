package io.stormbird.wallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.DApp;

public class DappBrowserUtils {
    public static void saveToPrefs(Context context, List<DApp> myDapps) {
        String myDappsJson = new Gson().toJson(myDapps);
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString("my_dapps", myDappsJson)
                .apply();
    }

    public static void saveHistory(Context context, List<String> history) {
        String myDappsJson = new Gson().toJson(history);
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(C.DAPP_BROWSER_HISTORY, myDappsJson)
                .apply();
    }

    public static List<DApp> getMyDapps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String myDappsJson = prefs.getString("my_dapps", "");

        if (!myDappsJson.isEmpty()) {
            return new Gson().fromJson(myDappsJson, new TypeToken<ArrayList<DApp>>() {
            }.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DApp> getBrowserHistory(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String historyJson = prefs.getString(C.DAPP_BROWSER_HISTORY, "");

        List<String> history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>() {
        }.getType());
        List<DApp> adaptedHistory = new ArrayList<>();

        if (!historyJson.isEmpty()) {
            for (String s : history) {
                adaptedHistory.add(new DApp(s, s));
            }
        }
        return adaptedHistory;
    }

    public static void clearHistory(Context context) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(C.DAPP_BROWSER_HISTORY, "")
                .apply();
    }

    public static void removeFromHistory(Context context, String item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String historyJson = prefs.getString(C.DAPP_BROWSER_HISTORY, "");

        List<String> history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>() {
        }.getType());

        for (String s : history) {
            if (s.equals(item)) {
                history.remove(s);
                break;
            }
        }

        saveHistory(context, history);
    }

    public static String getIconUrl(String url) {
        return "https://api.faviconkit.com/" + url + "/144";
    }
}
