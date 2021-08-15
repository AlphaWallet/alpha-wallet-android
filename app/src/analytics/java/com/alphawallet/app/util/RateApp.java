package com.alphawallet.app.util;

import android.app.Activity;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class RateApp {
    // should be shown on 5th run or after the first transaction (afterTransaction == true)
    static public void showRateTheApp(Activity context, PreferenceRepositoryType preferenceRepository, boolean afterTransaction) {
        if (true || (preferenceRepository.getLaunchCount() == 6 || afterTransaction) && !preferenceRepository.getRateAppShown()) {
            ReviewManager manager = ReviewManagerFactory.create(context);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    Task<Void> flow = manager.launchReviewFlow(context, reviewInfo);
                    flow.addOnCompleteListener(flowTask -> {
                        preferenceRepository.setRateAppShown();
                    });
                }
            });
        }
    }
}
