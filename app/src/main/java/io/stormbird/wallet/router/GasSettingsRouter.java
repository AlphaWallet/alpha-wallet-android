package io.stormbird.wallet.router;


import android.app.Activity;
import android.content.Intent;

import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.ui.GasSettingsActivity;
import io.stormbird.wallet.viewmodel.GasSettingsViewModel;

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
