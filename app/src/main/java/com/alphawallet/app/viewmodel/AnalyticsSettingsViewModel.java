package com.alphawallet.app.viewmodel;

import com.alphawallet.app.repository.PreferenceRepositoryType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AnalyticsSettingsViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;

    @Inject
    AnalyticsSettingsViewModel(PreferenceRepositoryType preferenceRepository)
    {
        this.preferenceRepository = preferenceRepository;
//        setAnalyticsService(analyticsService);
    }

    public boolean isAnalyticsEnabled()
    {
        return preferenceRepository.isAnalyticsEnabled();
    }

    public boolean isCrashReportingEnabled()
    {
        return preferenceRepository.isCrashReportingEnabled();
    }

    public void toggleAnalytics(boolean isEnabled)
    {
        preferenceRepository.setAnalyticsEnabled(isEnabled);
    }

    public void toggleCrashReporting(boolean isEnabled)
    {
        preferenceRepository.setCrashReportingEnabled(isEnabled);
    }
}
