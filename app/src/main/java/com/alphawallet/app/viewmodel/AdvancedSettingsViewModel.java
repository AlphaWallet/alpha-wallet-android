package com.alphawallet.app.viewmodel;

import android.os.Environment;

import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TransactionsService;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;

@HiltViewModel
public class AdvancedSettingsViewModel extends BaseViewModel {
    private final PreferenceRepositoryType preferenceRepository;
    private final TransactionsService transactionsService;

    @Inject
    AdvancedSettingsViewModel(
            PreferenceRepositoryType preferenceRepository,
            TransactionsService transactionsService) {
        this.preferenceRepository = preferenceRepository;
        this.transactionsService = transactionsService;
    }

    public boolean createDirectory() {
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + HomeViewModel.ALPHAWALLET_DIR);

        if (!directory.exists()) {
            return directory.mkdir();
        }
        else
        {
            return directory.exists();
        }
    }

    public void setFullScreenState(boolean state)
    {
        preferenceRepository.setFullScreenState(state);
    }

    public void toggle1559Transactions(boolean toggleState)
    {
        preferenceRepository.setUse1559Transactions(toggleState);
    }

    public boolean get1559TransactionsState()
    {
        return preferenceRepository.getUse1559Transactions();
    }

    public boolean getDeveloperOverrideState()
    {
        return preferenceRepository.getDeveloperOverride();
    }

    public void toggleDeveloperOverride(boolean toggleState)
    {
        preferenceRepository.setDeveloperOverride(toggleState);
    }

    public boolean getFullScreenState()
    {
        return preferenceRepository.getFullScreenState();
    }

    public void blankFilterSettings()
    {
        preferenceRepository.blankHasSetNetworkFilters();
    }

    public Single<Boolean> resetTokenData()
    {
        return transactionsService.wipeDataForWallet();
    }

    public void stopChainActivity()
    {
        transactionsService.stopActivity();
    }

    public void toggleUseViewer(boolean state)
    {
        preferenceRepository.setUseTSViewer(state);
    }

    public boolean getTokenScriptViewerState()
    {
        return preferenceRepository.getUseTSViewer();
    }
}
