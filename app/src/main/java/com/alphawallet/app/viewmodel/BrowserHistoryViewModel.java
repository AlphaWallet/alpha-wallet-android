package com.alphawallet.app.viewmodel;

import com.alphawallet.app.service.AnalyticsServiceType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class BrowserHistoryViewModel extends BaseViewModel
{
    @Inject
    BrowserHistoryViewModel(AnalyticsServiceType analyticsService)
    {
        setAnalyticsService(analyticsService);
    }
}
