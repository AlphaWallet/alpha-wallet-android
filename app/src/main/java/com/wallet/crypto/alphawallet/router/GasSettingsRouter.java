package com.wallet.crypto.alphawallet.router;


import android.app.Activity;
import android.content.Intent;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.GasSettings;
import com.wallet.crypto.alphawallet.ui.GasSettingsActivity;
import com.wallet.crypto.alphawallet.viewmodel.GasSettingsViewModel;

public class GasSettingsRouter {
    public void open(Activity context, GasSettings gasSettings) {
        Intent intent = new Intent(context, GasSettingsActivity.class);
        intent.putExtra(C.EXTRA_GAS_PRICE, gasSettings.gasPrice.toString());
        intent.putExtra(C.EXTRA_GAS_LIMIT, gasSettings.gasLimit.toString());
        context.startActivityForResult(intent, GasSettingsViewModel.SET_GAS_SETTINGS);
    }
}
