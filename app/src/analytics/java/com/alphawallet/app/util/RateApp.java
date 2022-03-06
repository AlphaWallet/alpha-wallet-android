package com.alphawallet.app.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;

import androidx.appcompat.app.AlertDialog;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

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
                    .setNegativeButton(R.string.rate_no_thanks, (dialogInterface, i) -> {
                        preferenceRepository.setRateAppShown();
                    });

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

    private static void startRateFlow(Activity context, PreferenceRepositoryType preferenceRepository) {
        ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(context, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {

                });
            }
        });
        // save rate ui shown
        preferenceRepository.setRateAppShown();
    }
}
