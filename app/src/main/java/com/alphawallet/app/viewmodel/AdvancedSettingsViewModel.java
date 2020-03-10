package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.alphawallet.app.entity.LocaleItem;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.GetDefaultWalletBalance;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.HelpRouter;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.LocaleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class AdvancedSettingsViewModel extends BaseViewModel {
    private final LocaleRepositoryType localeRepository;

    AdvancedSettingsViewModel(
            LocaleRepositoryType localeRepository) {
        this.localeRepository = localeRepository;
    }

    public String getDefaultLocale() {
        return localeRepository.getDefaultLocale();
    }

    public ArrayList<LocaleItem> getLocaleList(Context context) {
        return localeRepository.getLocaleList(context);
    }

    public void setLocale(Context activity) {
        String currentLocale = localeRepository.getDefaultLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public void updateLocale(String newLocale, Context context) {
        localeRepository.setDefaultLocale(context, newLocale);
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
