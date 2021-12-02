package com.alphawallet.app.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
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

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

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

            tokenInfoHeaderView = new TokenInfoHeaderView(getContext(), token);
            tokenInfoHeaderLayout.addView(tokenInfoHeaderView);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Portfolio"));
            tokenInfoLayout.addView(portfolioBalance);
            tokenInfoLayout.addView(portfolioProfit24Hr);
            tokenInfoLayout.addView(portfolioProfitTotal);
            tokenInfoLayout.addView(portfolioShare);
            tokenInfoLayout.addView(portfolioAverageCost);
            tokenInfoLayout.addView(portfolioPaidFees);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Performance"));
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



            viewModel.marketPrice().observe(getViewLifecycleOwner(), this::onMarketPriceChanged);
            // TODO: Create entity for chart data
            // viewModel.chartData().observe(getViewLifecycleOwner(), this::onChartDataFetched);
            viewModel.portfolio().observe(getViewLifecycleOwner(), this::onPortfolioUpdated);
            viewModel.performance().observe(getViewLifecycleOwner(), this::onPerformanceUpdated);
            viewModel.stats().observe(getViewLifecycleOwner(), this::onStatsUpdated);

            historyChart.fetchHistory(token.tokenInfo, HistoryChart.Range.Day);
        }
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
        tokenInfoHeaderView.setMarketValue(value);
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
