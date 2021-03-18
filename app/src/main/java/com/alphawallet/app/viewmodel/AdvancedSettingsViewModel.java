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
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.LocaleUtils;

import java.io.File;
import java.util.ArrayList;

public class AdvancedSettingsViewModel extends BaseViewModel {
    private final LocaleRepositoryType localeRepository;
    private final CurrencyRepositoryType currencyRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final PreferenceRepositoryType preferenceRepository;

    AdvancedSettingsViewModel(
            LocaleRepositoryType localeRepository,
            CurrencyRepositoryType currencyRepository,
            AssetDefinitionService assetDefinitionService,
            PreferenceRepositoryType preferenceRepository) {
        this.localeRepository = localeRepository;
        this.currencyRepository = currencyRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.preferenceRepository = preferenceRepository;
    }

    public String getUserPreferenceLocale()
    {
        return localeRepository.getUserPreferenceLocale();
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
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public String getDefaultCurrency(){
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList() {
        return currencyRepository.getCurrencyList();
    }

    public void updateCurrency(String currencyCode){
        currencyRepository.setDefaultCurrency(currencyCode);
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
}
