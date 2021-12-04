package com.alphawallet.app.ui;


import static com.alphawallet.app.service.TickerService.coinGeckoChainIdToAPIName;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateUtils;
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
import com.alphawallet.app.entity.CoinGeckoTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenPerformance;
import com.alphawallet.app.entity.tokens.TokenPortfolio;
import com.alphawallet.app.entity.tokens.TokenStats;
import com.alphawallet.app.ui.widget.entity.HistoryChart;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.TokenInfoViewModel;
import com.alphawallet.app.viewmodel.TokenInfoViewModelFactory;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoHeaderView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TokenInfoFragment extends BaseFragment {
    public static final int CHART_1D = 0;
    public static final int CHART_1W = 1;
    public static final int CHART_1M = 2;
    public static final int CHART_3M = 3;
    public static final int CHART_1Y = 4;

    @Inject
    TokenInfoViewModelFactory viewModelFactory;
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
    private TokenInfoView statsTotalSupply;
    private TokenInfoView statsMaxSupply;
    private TokenInfoView stats1YearLow;
    private TokenInfoView stats1YearHigh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        return inflater.inflate(R.layout.fragment_token_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(TokenInfoViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));

            initTabLayout(view);
            historyChart = view.findViewById(R.id.history_chart);
            tokenInfoHeaderLayout = view.findViewById(R.id.layout_token_header);
            tokenInfoLayout = view.findViewById(R.id.layout_token_info);

            portfolioBalance = new TokenInfoView(getContext(), "Balance");
            portfolioProfit24Hr = new TokenInfoView(getContext(), "24-H Return");
            portfolioProfitTotal = new TokenInfoView(getContext(), "Profit/Loss");
            portfolioShare = new TokenInfoView(getContext(), "Portfolio Share");
            portfolioAverageCost = new TokenInfoView(getContext(), "Average Cost");
            portfolioPaidFees = new TokenInfoView(getContext(), "Paid Fees");
            performance1D = new TokenInfoView(getContext(), "1 Day");
            performance1W = new TokenInfoView(getContext(), "1 Week");
            performance1M = new TokenInfoView(getContext(), "1 Month");
            performance1Y = new TokenInfoView(getContext(), "1 Year");
            statsMarketCap = new TokenInfoView(getContext(), "Market Cap");
            statsTotalSupply = new TokenInfoView(getContext(), "Total Supply");
            statsMaxSupply = new TokenInfoView(getContext(), "Max Supply");
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
            tokenInfoLayout.addView(statsTotalSupply);
            tokenInfoLayout.addView(statsMaxSupply);
            tokenInfoLayout.addView(stats1YearLow);
            tokenInfoLayout.addView(stats1YearHigh);

            //viewModel.marketPrice().observe(getViewLifecycleOwner(), this::onMarketPriceChanged);
            // TODO: Create entity for chart data
            // viewModel.chartData().observe(getViewLifecycleOwner(), this::onChartDataFetched);
            //viewModel.portfolio().observe(getViewLifecycleOwner(), this::onPortfolioUpdated);
            //viewModel.performance().observe(getViewLifecycleOwner(), this::onPerformanceUpdated);
            //viewModel.stats().observe(getViewLifecycleOwner(), this::onStatsUpdated);

            historyChart.fetchHistory(token.tokenInfo, HistoryChart.Range.Day);
            populateStats(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleValues, e -> { /*TODO: Hide stats*/ })
                    .isDisposed();

            long yesterdayTime = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
            long oneWeekTime = System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS;
            long oneMonthTime = System.currentTimeMillis() - 30 * DateUtils.DAY_IN_MILLIS;
            long oneYearTime = System.currentTimeMillis() - DateUtils.YEAR_IN_MILLIS;

            Date yesterday = getMidnightDateFromTimestamp(yesterdayTime);
            Date oneWeek = getMidnightDateFromTimestamp(oneWeekTime);
            Date oneMonth = getMidnightDateFromTimestamp(oneMonthTime);
            Date oneYear = getMidnightDateFromTimestamp(oneYearTime);
            System.out.println("YOLESS: " + yesterday.getTime());
            System.out.println("YOLESS: " + oneWeek.getTime());
            System.out.println("YOLESS: " + oneMonth.getTime());
            System.out.println("YOLESS: " + oneYear.getTime());
            System.out.println("YOLESS: " + oneYear.getTime());
        }
    }

    private void handleValues(List<Long> values)
    {
        //display values
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
                historyChart.fetchHistory(token.tokenInfo, HistoryChart.Range.values()[tab.getPosition()]);
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

    private void onMarketPriceChanged(String value)
    {
        //tokenInfoHeaderView.setMarketValue(value);
        // TODO: Compute price change
        // tokenInfoHeaderView.setPriceChange();
    }

    private void onChartDataFetched()
    {

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

    private Single<List<Long>> populateStats(Token token)
    {
        List<Long> values = new ArrayList<>();
        if (!coinGeckoChainIdToAPIName.containsKey(token.tokenInfo.chainId))
        {
            //hide info layout
            return Single.fromCallable(() -> values);
        }

        return Single.fromCallable(() -> {

            //TODO: Allow for L2 chain tokens, eg Arbitrum
            String coinGeckotokenId = token.isEthereum() ? coinGeckoChainIdToAPIName.get(token.tokenInfo.chainId)
                    : coinGeckoChainIdToAPIName.get(token.tokenInfo.chainId) + "/contract/" + token.getAddress().toLowerCase();

            Request request = new Request.Builder()
                    .url("https://api.coingecko.com/api/v3/coins/" + coinGeckotokenId + "/market_chart?vs_currency=usd&days=365")
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
                long oneYearTime = System.currentTimeMillis() - DateUtils.YEAR_IN_MILLIS;

                Date yesterday = getMidnightDateFromTimestamp(yesterdayTime);
                Date oneWeek = getMidnightDateFromTimestamp(oneWeekTime);
                Date oneMonth = getMidnightDateFromTimestamp(oneMonthTime);
                Date oneYear = getMidnightDateFromTimestamp(oneYearTime);

                int size = prices.length();
                //yesterday = index 3;

                double aa = findValue(prices, yesterday);
                double ab = findValue(prices, oneWeek);
                double ac = findValue(prices, oneMonth);
                double ad = findValue(prices, oneYear);

                //long a = prices.getLong(size-3);
                //long b = prices.getLong(0);
                Double ap = getDoubleValue(prices, size-3);// (new JSONArray(prices.get(size-3))  prices.getDouble(size-3);
                Double bp = getDoubleValue(prices,size-9);
                //long c = prices.getLong(size-9);
                Double cp = getDoubleValue(prices,size-32);
                Double dp = getDoubleValue(prices,0);

                values.add(prices.getLong(size-3));



            /*
                        tokenInfoLayout.addView(performance1D);
            tokenInfoLayout.addView(performance1W);
            tokenInfoLayout.addView(performance1M);
            tokenInfoLayout.addView(performance1Y);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Stats"));
            tokenInfoLayout.addView(statsMarketCap);
            tokenInfoLayout.addView(statsTotalSupply);
            tokenInfoLayout.addView(statsMaxSupply);
            tokenInfoLayout.addView(stats1YearLow);
            tokenInfoLayout.addView(stats1YearHigh);
             */

            }
            catch (Exception e)
            {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }

            return values;
        });

        //fetch stats

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
                if (lastDate > targetTime && timeStamp <= targetTime)
                {
                    //got it
                    return thisPrice.getDouble(1);
                }
                lastDate = timeStamp;
            }
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG) e.printStackTrace();
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
            if (BuildConfig.DEBUG) e.printStackTrace();
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

    private void onPerformanceUpdated(TokenPerformance tokenPerformance)
    {
        performance1D.setValue(tokenPerformance.getDay());
        performance1W.setValue(tokenPerformance.getWeek());
        performance1M.setValue(tokenPerformance.getMonth());
        performance1Y.setValue(tokenPerformance.getYear());
    }

    private void onStatsUpdated(TokenStats tokenStats)
    {
        statsMarketCap.setValue(tokenStats.getMarketCap());
        statsTotalSupply.setValue(tokenStats.getTotalSupply());
        statsMaxSupply.setValue(tokenStats.getMaxSupply());
        stats1YearLow.setValue(tokenStats.getYearLow());
        stats1YearHigh.setValue(tokenStats.getYearHigh());
    }
}
