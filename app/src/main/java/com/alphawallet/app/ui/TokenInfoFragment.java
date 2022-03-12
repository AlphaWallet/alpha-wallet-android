package com.alphawallet.app.ui;


import static com.alphawallet.app.service.TickerService.chainPairs;
import static com.alphawallet.app.service.TickerService.coinGeckoChainIdToAPIName;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenPortfolio;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.entity.HistoryChart;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.TokenInfoViewModel;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoHeaderView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.tools.Convert;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

@AndroidEntryPoint
public class TokenInfoFragment extends BaseFragment {
    public static final int CHART_1D = 0;
    public static final int CHART_1W = 1;
    public static final int CHART_1M = 2;
    public static final int CHART_3M = 3;
    public static final int CHART_1Y = 4;

    private TokenInfoViewModel viewModel;

    private Token token;
    private LinearLayout tokenInfoHeaderLayout;
    private LinearLayout tokenInfoLayout;
    private HistoryChart historyChart;

    private TokenInfoHeaderView tokenInfoHeaderView;
    private TokenInfoView portfolioBalance;
    private TokenInfoView portfolioProfit24Hr;
    private TokenInfoView portfolioProfitTotal;
    private TokenInfoView portfolioShare;
    private TokenInfoView portfolioAverageCost;
    private TokenInfoView portfolioPaidFees;
    private TokenInfoView performance1D;
    private TokenInfoView performance1W;
    private TokenInfoView performance1M;
    private TokenInfoView performance1Y;
    private TokenInfoView statsMarketCap;
    private TokenInfoView statsTradingVolume;
    private TokenInfoView statsMaxVolume;
    private TokenInfoView stats1YearLow;
    private TokenInfoView stats1YearHigh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_token_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(TokenInfoViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));

            initTabLayout(view);
            historyChart = view.findViewById(R.id.history_chart);
            tokenInfoHeaderLayout = view.findViewById(R.id.layout_token_header);
            tokenInfoLayout = view.findViewById(R.id.layout_token_info);

            //TODO: Work out how to source these
            //portfolioBalance = new TokenInfoView(getContext(), "Balance");
            //portfolioProfit24Hr = new TokenInfoView(getContext(), "24-H Return");
            //portfolioProfitTotal = new TokenInfoView(getContext(), "Profit/Loss");
            //portfolioShare = new TokenInfoView(getContext(), "Portfolio Share");
            //portfolioAverageCost = new TokenInfoView(getContext(), "Average Cost");
            //portfolioPaidFees = new TokenInfoView(getContext(), "Paid Fees");
            performance1D = new TokenInfoView(getContext(), "1 Day");
            performance1D.setHasPrefix(true);
            performance1W = new TokenInfoView(getContext(), "1 Week");
            performance1W.setHasPrefix(true);
            performance1M = new TokenInfoView(getContext(), "1 Month");
            performance1M.setHasPrefix(true);
            performance1Y = new TokenInfoView(getContext(), "1 Year");
            performance1Y.setHasPrefix(true);
            statsMarketCap = new TokenInfoView(getContext(), "Market Cap");
            statsTradingVolume = new TokenInfoView(getContext(), "Current Volume");
            statsMaxVolume = new TokenInfoView(getContext(), "Max Volume");
            stats1YearLow = new TokenInfoView(getContext(), "1 Year Low");
            stats1YearHigh = new TokenInfoView(getContext(), "1 Year High");

            tokenInfoHeaderView = new TokenInfoHeaderView(getContext(), token, viewModel.getTokensService());
            tokenInfoHeaderLayout.addView(tokenInfoHeaderView);

            /*tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Portfolio"));
            tokenInfoLayout.addView(portfolioBalance);
            tokenInfoLayout.addView(portfolioProfit24Hr);
            tokenInfoLayout.addView(portfolioProfitTotal);
            tokenInfoLayout.addView(portfolioShare);
            tokenInfoLayout.addView(portfolioAverageCost);
            tokenInfoLayout.addView(portfolioPaidFees);*/

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), getString(R.string.performance)));
            tokenInfoLayout.addView(performance1D);
            tokenInfoLayout.addView(performance1W);
            tokenInfoLayout.addView(performance1M);
            tokenInfoLayout.addView(performance1Y);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Stats"));
            tokenInfoLayout.addView(statsMarketCap);
            tokenInfoLayout.addView(statsTradingVolume);
            tokenInfoLayout.addView(statsMaxVolume);
            tokenInfoLayout.addView(stats1YearLow);
            tokenInfoLayout.addView(stats1YearHigh);

            historyChart.fetchHistory(token, HistoryChart.Range.Day);
            populateStats(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleValues, e -> { /*TODO: Hide stats*/ })
                    .isDisposed();
        }
    }

    private void hideStats()
    {
        tokenInfoLayout.removeAllViews();
        tokenInfoLayout.invalidate();
    }

    private void handleValues(List<Double> values)
    {
        if (values.size() == 0)
        {
            hideStats();
            return;
        }

        int index = 0;

        performance1D.setCurrencyValue(values.get(index++));
        performance1W.setCurrencyValue(values.get(index++));
        performance1M.setCurrencyValue(values.get(index++));
        performance1Y.setCurrencyValue(values.get(index++));

        statsMarketCap.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        statsTradingVolume.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        statsMaxVolume.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        stats1YearLow.setValue(TickerService.getFullCurrencyString(values.get(index++)));
        stats1YearHigh.setValue(TickerService.getFullCurrencyString(values.get(index++)));
    }

    private void initTabLayout(View view)
    {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("1D"));
        tabLayout.addTab(tabLayout.newTab().setText("1W"));
        tabLayout.addTab(tabLayout.newTab().setText("1M"));
        tabLayout.addTab(tabLayout.newTab().setText("3M"));
        tabLayout.addTab(tabLayout.newTab().setText("1Y"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                historyChart.fetchHistory(token, HistoryChart.Range.values()[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {
            }
        });

        TabUtils.setHighlightedTabColor(getContext(), tabLayout);
    }

    private void onPortfolioUpdated(TokenPortfolio tokenPortfolio)
    {
        portfolioBalance.setValue(tokenPortfolio.getBalance());
        portfolioProfit24Hr.setValue(tokenPortfolio.getProfit24Hrs());
        portfolioProfitTotal.setValue(tokenPortfolio.getProfitTotal());
        portfolioShare.setValue(tokenPortfolio.getShare());
        portfolioAverageCost.setValue(tokenPortfolio.getAverageCost());
        portfolioPaidFees.setValue(tokenPortfolio.getFees());
    }

    static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    private Single<List<Double>> populateStats(Token token)
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

            try (okhttp3.Response response = httpClient.newCall(request)
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
                values.add(getDoubleValue(marketCaps,marketCaps.length() - 1));

                //add total volume
                values.add(getDoubleValue(totalVolumes,marketCaps.length() - 1));

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

    private Date getMidnightDateFromTimestamp(long timeStampInMillis) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(timeStampInMillis);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }
}
