package com.alphawallet.app.viewmodel;

import com.alphawallet.app.service.AnalyticsServiceType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditDappViewModel extends BaseViewModel
{
    @Inject
    AddEditDappViewModel(AnalyticsServiceType analyticsService)
    {
        setAnalyticsService(analyticsService);
    }
}
