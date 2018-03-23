package io.awallet.crypto.alphawallet.router;


import android.app.Activity;
import android.content.Intent;

import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.ui.GasSettingsActivity;
import io.awallet.crypto.alphawallet.viewmodel.GasSettingsViewModel;

public class GasSettingsRouter {
    public void open(Activity context, GasSettings gasSettings) {
        Intent intent = new Intent(context, GasSettingsActivity.class);
        intent.putExtra(C.EXTRA_GAS_PRICE, gasSettings.gasPrice.toString());
        intent.putExtra(C.EXTRA_GAS_LIMIT, gasSettings.gasLimit.toString());
        context.startActivityForResult(intent, GasSettingsViewModel.SET_GAS_SETTINGS);
    }
}
