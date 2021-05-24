package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.tokendata.TokenPerformance;
import com.alphawallet.app.entity.tokendata.TokenPortfolio;
import com.alphawallet.app.entity.tokendata.TokenStats;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.TokenInfoFragment;

public class TokenInfoViewModel extends BaseViewModel {
    private final MutableLiveData<String> marketPrice = new MutableLiveData<>();
    private final MutableLiveData<TokenPortfolio> portfolio = new MutableLiveData<>();
    private final MutableLiveData<TokenPerformance> performance = new MutableLiveData<>();
    private final MutableLiveData<TokenStats> stats = new MutableLiveData<>();

    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public TokenInfoViewModel(AssetDefinitionService assetDefinitionService,
                              TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<String> marketPrice()
    {
        return marketPrice;
    }

    public LiveData<TokenPortfolio> portfolio()
    {
        return portfolio;
    }

    public LiveData<TokenPerformance> performance()
    {
        return performance;
    }

    public LiveData<TokenStats> stats()
    {
        return stats;
    }

    public void fetchPortfolio()
    {
        TokenPortfolio tokenPortfolio = new TokenPortfolio();

        // TODO: Do calculations here

        portfolio.postValue(tokenPortfolio);
    }

    public void fetchPerformance()
    {
        TokenPerformance tokenPerformance = new TokenPerformance();

        // TODO: Do calculations here

        performance.postValue(tokenPerformance);
    }

    public void fetchStats()
    {
        TokenStats tokenStats = new TokenStats();

        // TODO: Do calculations here

        stats.postValue(tokenStats);
    }

    public void fetchChartData(int position)
    {
        switch (position)
        {
            case TokenInfoFragment.CHART_1D:
            {
                // TODO
                break;
            }
            case TokenInfoFragment.CHART_1W:
            {
                // TODO
                break;
            }
            case TokenInfoFragment.CHART_1M:
            {
                // TODO
                break;
            }
            case TokenInfoFragment.CHART_3M:
            {
                // TODO
                break;
            }
            case TokenInfoFragment.CHART_1Y:
            {
                // TODO
                break;
            }
            default:
                break;
        }
    }
}
