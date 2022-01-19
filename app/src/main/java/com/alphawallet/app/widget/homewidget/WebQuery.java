package com.alphawallet.app.widget.homewidget;


import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class WebQuery
{
    private static final String LIMIT_TOKEN = "[COUNT]";
    private static final String COINMARKET_TICKER_API = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?symbol=";
    private static final String COINMARKET_LIST_API = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/map?limit=" + LIMIT_TOKEN + "&sort=cmc_rank";
    //private static final String COINMARKET_API_KEY = "d697e749-59d1-4549-8566-c9ad2d5384cb";

    static {
        System.loadLibrary("keys");
    }

    public static native String getCMCKey();
    public static native String getSecondaryCMCKey();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    public Single<CoinData[]> getCoinList(int limit)
    {
        String query = COINMARKET_LIST_API.replace(LIMIT_TOKEN, String.valueOf(limit));

        return runQuery(query)
                .map(this::APIsuccess)
                .subscribeOn(Schedulers.io());
    }

    public Single<CoinData[]> updateCryptoTickers(String currency, final CoinData[] coins)
    {
        String key = getSecondaryCMCKey();
        String query = COINMARKET_TICKER_API + getTokenPairList(coins);
        return convertPair("USD", currency)
                .flatMap(rate -> {
                    return runQuery(query)
                            .map(result -> processCoins(result, coins, rate));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(mainThread());
    }

    private CoinData[] processCoins(String result, CoinData[] coins, Double rate)
    {
        try
        {
            JSONObject stateData = new JSONObject(result);
            JSONObject orders = stateData.getJSONObject("data");

            for (CoinData c : coins)
            {
                JSONObject tickerData = (JSONObject) orders.get(c.symbol);
                JSONObject quote = (JSONObject)tickerData.get("quote");
                JSONObject usdData = (JSONObject)quote.get("USD");

                c.change_1h = ((Double)usdData.get("percent_change_1h")).floatValue();
                c.change_7d = ((Double)usdData.get("percent_change_7d")).floatValue();
                c.change_24h = ((Double)usdData.get("percent_change_24h")).floatValue();
                double dd = (Double)usdData.get("price");
                c.price = (float) (dd * rate);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return coins;
    }

    private String getTokenPairList(CoinData[] coins)
    {
        StringBuilder sb = new StringBuilder();
        boolean f = true;
        for (CoinData c : coins)
        {
            if (!f)
                sb.append(",");
            sb.append(c.symbol);
            f = false;
        }
        return sb.toString();
    }

    private CoinData[] APIsuccess(String result)
    {
        List<CoinData> coinList = new ArrayList<>();
        try
        {
            JSONObject res = new JSONObject(result);
            JSONArray docArray = res.getJSONArray("data");
            int length = docArray.length();
            for (int i = 0; i < length; i++)
            {
                JSONObject stateData = docArray.getJSONObject(i);
                CoinData rtn = new CoinData(stateData);
                coinList.add(rtn);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return coinList.toArray(new CoinData[0]);
    }

    //IMPORTANT!! This can only be run from within a thread!
    private Single<String> runQuery(String query)
    {
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url(query)
                        .get()
                        .addHeader("X-CMC_PRO_API_KEY", getSecondaryCMCKey())
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                return response.body().string();
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
                throw new MalformedURLException("Malformed");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new IOException("IO");
            }
        });
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            if (currency1 == null || currency2 == null || currency1.equals(currency2)) return 1.0;
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;
            okhttp3.Response response = null;

            try
            {
                Request request = new Request.Builder()
                        .url(conversionURL)
                        .addHeader("Connection","close")
                        .get()
                        .build();
                response = httpClient.newCall(request)
                        .execute();

                int resultCode = response.code();
                if ((resultCode / 100) == 2 && response.body() != null)
                {
                    String responseBody = response.body().string();
                    double rate = Double.parseDouble(responseBody);
                    return rate;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (response != null) response.close();
            }

            return 1.0;
        });
    }
}
