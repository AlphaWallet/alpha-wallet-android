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
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.entity.HistoryChart;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.TokenInfoViewModel;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoHeaderView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TokenInfoFragment extends BaseFragment
{
    private TokenInfoViewModel viewModel;
    private Token token;
    private LinearLayout tokenInfoHeaderLayout;
    private LinearLayout tokenInfoLayout;
    private LinearLayout historyLayout;
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
            getIntentData();

            initViewModel();

            initViews(view);

            initTabLayout(view);


            historyChart.fetchHistory(token, HistoryChart.Range.Day);

            viewModel.getStats(token);
        }
    }

    private void initViews(View view)
    {
        historyChart = view.findViewById(R.id.history_chart);
        tokenInfoHeaderLayout = view.findViewById(R.id.layout_token_header);
        tokenInfoLayout = view.findViewById(R.id.layout_token_info);
        historyLayout = view.findViewById(R.id.layout_history);

        if (!token.hasRealValue())
        {
            historyLayout.setVisibility(View.GONE);
            // TODO: Show faucet button if available
        }

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
    }

    private void getIntentData()
    {
        long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(TokenInfoViewModel.class);
        viewModel.stats().observe(getViewLifecycleOwner(), this::onStats);
    }

    private void onStats(List<Double> stats)
    {
        processStats(stats);
    }

    private void processStats(List<Double> stats)
    {
        if (stats.size() == 0)
        {
            hideStats();
            return;
        }

        int index = 0;

        performance1D.setCurrencyValue(stats.get(index++));
        performance1W.setCurrencyValue(stats.get(index++));
        performance1M.setCurrencyValue(stats.get(index++));
        performance1Y.setCurrencyValue(stats.get(index++));

        statsMarketCap.setValue(TickerService.getFullCurrencyString(stats.get(index++)));
        statsTradingVolume.setValue(TickerService.getFullCurrencyString(stats.get(index++)));
        statsMaxVolume.setValue(TickerService.getFullCurrencyString(stats.get(index++)));
        stats1YearLow.setValue(TickerService.getFullCurrencyString(stats.get(index++)));
        stats1YearHigh.setValue(TickerService.getFullCurrencyString(stats.get(index++)));
    }

    private void hideStats()
    {
        tokenInfoLayout.removeAllViews();
        tokenInfoLayout.invalidate();
    }

    private void initTabLayout(View view)
    {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("1D"));
        tabLayout.addTab(tabLayout.newTab().setText("1W"));
        tabLayout.addTab(tabLayout.newTab().setText("1M"));
        tabLayout.addTab(tabLayout.newTab().setText("3M"));
        tabLayout.addTab(tabLayout.newTab().setText("1Y"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
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
}
