package com.alphawallet.app.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;

import androidx.appcompat.app.AlertDialog;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RateApp {
    // should be shown on 5th run or after the first transaction (afterTransaction == true)
    static public void showRateTheApp(Activity context, PreferenceRepositoryType preferenceRepository, boolean afterTransaction) {
        if (!Utils.verifyInstallerId(context)) return;
        if ((preferenceRepository.getLaunchCount() == 6 || afterTransaction) && !preferenceRepository.getRateAppShown()) {
            View contentView = LayoutInflater.from(context).inflate(R.layout.layout_rate_dialog, null, false);
            final RatingBar ratingBar = contentView.findViewById(R.id.rating_bar);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.rate_title, context.getString(R.string.app_name)))
                    .setView(contentView)
                    .setMessage(context.getString(R.string.rate_prompt, context.getString(R.string.app_name)))
                    .setNeutralButton(R.string.rate_later, (dialogInterface, i) -> {
                        // reset launch count
                        preferenceRepository.resetLaunchCount();
                    })
                    .setPositiveButton(R.string.rate_rate, (dialogInterface, i) -> {
                        if (ratingBar.getRating() > 4) {
                            startRateFlow(context, preferenceRepository);
                        } else {
                            preferenceRepository.setRateAppShown();
                        }
                    })
                    .setNegativeButton(R.string.rate_no_thanks, (dialogInterface, i) -> preferenceRepository.setRateAppShown());

            AlertDialog dialog = builder.show();
            ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
                if (dialog.isShowing()) {
                    ratingBar.setNumStars((int)rating);
                    dialog.dismiss();
                    startRateFlow(context, preferenceRepository);
                }
            });
        }
    }

    private static void startRateFlow(Activity activity, PreferenceRepositoryType preferenceRepository) {
        //simply take them to play store for now, until the current situation with play store libraries not being allowed is resolved
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        // save rate ui shown
        preferenceRepository.setRateAppShown();
    }
}
