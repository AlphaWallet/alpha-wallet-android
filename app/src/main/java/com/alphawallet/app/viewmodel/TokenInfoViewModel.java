package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.service.TickerService.chainPairs;
import static com.alphawallet.app.service.TickerService.coinGeckoChainIdToAPIName;

import android.text.format.DateUtils;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenPerformance;
import com.alphawallet.app.entity.tokens.TokenPortfolio;
import com.alphawallet.app.entity.tokens.TokenStats;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.tools.Convert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

@HiltViewModel
public class TokenInfoViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final OkHttpClient okHttpClient;
    private final TokensService tokensService;

    private final MutableLiveData<List<Double>> stats = new MutableLiveData<>();

    private Disposable disposable;

    @Inject
    public TokenInfoViewModel(AssetDefinitionService assetDefinitionService,
                              OkHttpClient okHttpClient,
                              TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.okHttpClient = okHttpClient;
    }

    public LiveData<List<Double>> stats()
    {
        return stats;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void getStats(Token token)
    {
        disposable = populateStats(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onStats, this::onStatsError);
    }

    private void onStats(List<Double> stats)
    {
        this.stats.postValue(stats);
    }

    private void onStatsError(Throwable throwable)
    {

    }

    public Single<List<Double>> populateStats(Token token)
    {
        List<Double> values = new ArrayList<>();
        if (!TickerService.validateCoinGeckoAPI(token)) return Single.fromCallable(() -> values);

        return Single.fromCallable(() -> {

            String coinGeckotokenId = token.isEthereum() ? chainPairs.get(token.tokenInfo.chainId)
                    : coinGeckoChainIdToAPIName.get(token.tokenInfo.chainId) + "/contract/" + token.getAddress().toLowerCase();

            Request request = new Request.Builder()
                    .url("https://api.coingecko.com/api/v3/coins/" + coinGeckotokenId + "/market_chart?vs_currency=" + TickerService.getCurrencySymbolTxt() + "&days=365")
                    .get()
                    .build();

            try (okhttp3.Response response = okHttpClient.newCall(request)
                    .execute())
            {
                String result = response.body().string();
                //build mapping
                JSONArray prices = new JSONObject(result).getJSONArray("prices");
                JSONArray marketCaps = new JSONObject(result).getJSONArray("market_caps");
                JSONArray totalVolumes = new JSONObject(result).getJSONArray("total_volumes");

                long yesterdayTime = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
                long oneWeekTime = System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS;
                long oneMonthTime = System.currentTimeMillis() - 30 * DateUtils.DAY_IN_MILLIS;

                //Add performance stats. This is the variance
                double currentPrice = getDoubleValue(prices, prices.length() - 1);

                BigDecimal correctedBalance = token.getCorrectedBalance(Convert.Unit.ETHER.getFactor());

                BigDecimal yesterdayDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(yesterdayTime))).multiply(correctedBalance);
                BigDecimal oneWeekDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(oneWeekTime))).multiply(correctedBalance);
                BigDecimal oneMonthDiff = BigDecimal.valueOf(currentPrice - findValue(prices, getMidnightDateFromTimestamp(oneMonthTime))).multiply(correctedBalance);
                BigDecimal oneYearDiff = BigDecimal.valueOf(currentPrice - getDoubleValue(prices, 0)).multiply(correctedBalance);

                values.add(yesterdayDiff.doubleValue());
                values.add(oneWeekDiff.doubleValue());
                values.add(oneMonthDiff.doubleValue());
                values.add(oneYearDiff.doubleValue());

                //add market cap
                values.add(getDoubleValue(marketCaps, marketCaps.length() - 1));

                //add total volume
                values.add(getDoubleValue(totalVolumes, marketCaps.length() - 1));

                //get trading volume high
                Pair<Double, Double> minMax = getMinMax(totalVolumes);
                values.add(minMax.second);

                //get highs and lows
                minMax = getMinMax(prices);
                values.add(minMax.first);
                values.add(minMax.second);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return values;
        });
    }

    private Pair<Double, Double> getMinMax(JSONArray valueArray)
    {
        double min = Double.MAX_VALUE;
        double max = 0.0;
        try
        {
            for (int i = 0; i < valueArray.length(); i++)
            {
                JSONArray valueElement = valueArray.getJSONArray(i);
                double value = valueElement.getDouble(1);
                if (value < min) min = value;
                if (value > max) max = value;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        if (min == Double.MAX_VALUE) min = 0.0;

        return new Pair<>(min, max);
    }

    private double findValue(JSONArray prices, Date targetDate)
    {
        try
        {
            long lastDate = System.currentTimeMillis();
            long targetTime = targetDate.getTime();
            for (int i = prices.length() - 2; i >= 0; i--)
            {
                JSONArray thisPrice = prices.getJSONArray(i);
                long timeStamp = thisPrice.getLong(0);
                if (lastDate > targetTime && targetTime >= timeStamp)
                {
                    //got it
                    return thisPrice.getDouble(1);
                }
                lastDate = timeStamp;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return 0.0;
    }

    private double getDoubleValue(JSONArray prices, int i)
    {
        try
        {
            JSONArray thisPrice = prices.getJSONArray(i);
            return thisPrice.getDouble(1);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return 0.0;
    }

    private Date getMidnightDateFromTimestamp(long timeStampInMillis)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(timeStampInMillis);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }

    @Override
    protected void onCleared()
    {
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
        }
        super.onCleared();
    }
}
