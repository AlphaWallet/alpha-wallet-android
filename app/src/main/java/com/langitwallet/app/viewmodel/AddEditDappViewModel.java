package com.langitwallet.app.viewmodel;

import com.langitwallet.app.service.AnalyticsServiceType;

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
