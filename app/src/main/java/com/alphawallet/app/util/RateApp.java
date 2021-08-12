package com.alphawallet.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.widget.AWalletAlertDialog;

public class RateApp {
    // should be shown on 5th run or after the first transaction (afterTransaction == true)
    static public void showRateTheApp(Activity context, PreferenceRepositoryType preferenceRepository, boolean afterTransaction) {
        if ((preferenceRepository.getLaunchCount() == 6 || afterTransaction) && !preferenceRepository.getRateAppShown()) {

            AWalletAlertDialog aDialog = new AWalletAlertDialog(context);
            aDialog.setIcon(AWalletAlertDialog.SUCCESS);
            aDialog.setMessage(R.string.would_you_like_to_rate_the_app);
            aDialog.setButtonText(R.string.ok);
            aDialog.setButtonListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
                context.startActivity(intent);
                preferenceRepository.setRateAppShown();
                aDialog.dismiss();
            });
            aDialog.setSecondaryButtonText(R.string.action_cancel);
            aDialog.setSecondaryButtonListener(v -> {
                aDialog.dismiss();
            });
            aDialog.show();
        }
    }
}
