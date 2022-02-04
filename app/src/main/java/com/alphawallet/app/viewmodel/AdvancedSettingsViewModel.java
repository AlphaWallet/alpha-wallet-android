package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.LocaleItem;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.LocaleUtils;

import java.io.File;
import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.realm.Realm;

@HiltViewModel
public class AdvancedSettingsViewModel extends BaseViewModel {
    private final LocaleRepositoryType localeRepository;
    private final CurrencyRepositoryType currencyRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final PreferenceRepositoryType preferenceRepository;
    private final TransactionsService transactionsService;

    @Inject
    AdvancedSettingsViewModel(
            LocaleRepositoryType localeRepository,
            CurrencyRepositoryType currencyRepository,
            AssetDefinitionService assetDefinitionService,
            PreferenceRepositoryType preferenceRepository,
            TransactionsService transactionsService) {
        this.localeRepository = localeRepository;
        this.currencyRepository = currencyRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.preferenceRepository = preferenceRepository;
        this.transactionsService = transactionsService;
    }

    public ArrayList<LocaleItem> getLocaleList(Context context) {
        return localeRepository.getLocaleList(context);
    }

    public void setLocale(Context activity) {
        String currentLocale = localeRepository.getActiveLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public void updateLocale(String newLocale, Context context) {
        localeRepository.setUserPreferenceLocale(newLocale);
        localeRepository.setLocale(context, newLocale);
    }

    public String getDefaultCurrency(){
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList() {
        return currencyRepository.getCurrencyList();
    }

    public Single<Boolean> updateCurrency(String currencyCode){
        currencyRepository.setDefaultCurrency(currencyCode);
        //delete tickers from realm
        return transactionsService.wipeTickerData();
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

    public void startFileListeners()
    {
        assetDefinitionService.startAlphaWalletListener();
    }

    public String getActiveLocale()
    {
        return localeRepository.getActiveLocale();
    }

    public void setFullScreenState(boolean state)
    {
        preferenceRepository.setFullScreenState(state);
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
}
