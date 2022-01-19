package com.alphawallet.app.widget.homewidget;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinList
{
    private static final Map<String, CoinData> mCoins = new HashMap<>();
    private static final SparseArray<CoinData> mCoinRank = new SparseArray();

    public static void resetCryptoState()
    {
        mCoins.clear();
    }
    public static void addCryptoState(CoinData coin)
    {
        mCoins.put(coin.symbol, coin) ;
        mCoinRank.put(coin.rank, coin);
    }

    public static CoinData[] getCoinData()
    {
        return mCoins.values().toArray(new CoinData[0]);
    }

    public static CoinData[] populateCryptoList(Context ctx, CoinData[] coinList)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        mCoinRank.clear();
        //now populate crypto list
        int index = 0;
        for (CoinData c : coinList)
        {
            editor.putString("cN" + index, c.name);
            editor.putString("cS" + index, c.symbol);
            editor.putInt("cR" + index, c.rank);
            editor.putFloat("cP" + index, c.price);
            editor.putFloat("c1h" + index, c.change_1h);
            editor.putFloat("c24h" + index, c.change_24h);
            editor.putFloat("c7d" + index, c.change_7d);
            mCoinRank.put(c.rank, c);
            index++;
        }
        editor.putInt("cSz", index);
        editor.putLong("lTime", System.currentTimeMillis());
        editor.apply();

        return coinList;
    }

    public static long getLastAPIReturnDiff(Context ctx)
    {
        long currentTime = System.currentTimeMillis();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        long lastRCV = sp.getLong("lTime", 0);
        if (lastRCV == 0)
        {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("lTime", currentTime);
            lastRCV = currentTime;
            editor.apply();
        }

        return (currentTime - lastRCV) / 1000;
    }

    public static boolean loadCryptoList(Context ctx)
    {
        boolean gotCryptos = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        int size = sp.getInt("cSz", 0);

        mCoins.clear();

        for (int i = 0; i < size; i++)
        {
            String name = sp.getString("cN" + i, "");
            String code = sp.getString("cS" + i, "");
            CoinData c = new CoinData(name, code);
            c.rank = sp.getInt("cR" + i, 0);
            c.price = sp.getFloat("cP" + i, 0);
            c.change_1h = sp.getFloat("c1h" + i, 0);
            c.change_24h = sp.getFloat("c24h" + i, 0);
            c.change_7d = sp.getFloat("c7d" + i, 0);
            mCoins.put(code, c);
            mCoinRank.put(c.rank, c);
            gotCryptos = true;
        }

        return gotCryptos;
    }

    public static List<String> getNamesCryptos()
    {
        List<String> cryptoCodes = new ArrayList<>();
        for (int i = 1; i <= mCoinRank.size(); i++)
        {
            CoinData c = mCoinRank.get(i);
            if (c != null)
            {
                String display = c.name + " : " + c.symbol;
                cryptoCodes.add(display);
            }
        }
        return cryptoCodes;
    }

    public static int getCryptoIndex(String cryptoStr)
    {
        //first get rank
        CoinData c = mCoins.get(cryptoStr);
        if (c != null)
        {
            return (c.rank - 1);
        }
        else
        {
            return 0;
        }
    }

    public static void initCoins(Context ctx)
    {
        if (mCoins.size() == 0)
        {
            loadCryptoList(ctx);
        }
    }

    public static float getCryptoValue(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        if (cr != null)
        {
            return cr.price;
        }
        else
        {
            return -1;
        }
    }

    public static float getCrypto1hChange(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        return cr.change_1h;
    }

    public static float getCrypto24hChange(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        return cr.change_24h;
    }

    public static float getCrypto7dChange(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        return cr.change_7d;
    }

    public static String getCryptoCode(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        return cr.symbol;
    }

    public static String getCryptoName(String cryptoCode)
    {
        CoinData cr = mCoins.get(cryptoCode);
        return cr.name;
    }
}
