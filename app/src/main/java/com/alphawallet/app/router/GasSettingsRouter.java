package com.alphawallet.app.router;


import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.GasSettingsActivity;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.entity.GasSettings;

public class GasSettingsRouter {
    public void open(Activity context, GasSettings gasSettings, int chainId) {
        Intent intent = new Intent(context, GasSettingsActivity.class);
        intent.putExtra(C.EXTRA_GAS_PRICE, gasSettings.gasPrice.toString());
        intent.putExtra(C.EXTRA_GAS_LIMIT, gasSettings.gasLimit.toString());
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, GasSettingsViewModel.SET_GAS_SETTINGS);
    }
}
