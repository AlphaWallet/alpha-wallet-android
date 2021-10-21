package com.alphawallet.app.viewmodel;

import android.app.Activity;

import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.util.RateApp;

public class TransactionSuccessViewModel extends BaseViewModel {

    private final PreferenceRepositoryType preferenceRepository;

    public TransactionSuccessViewModel(
            PreferenceRepositoryType preferenceRepository
    ) {
        this.preferenceRepository = preferenceRepository;
    }

    public void tryToShowRateAppDialog(Activity context) {
        RateApp.showRateTheApp(context, preferenceRepository, true);
    }
}
