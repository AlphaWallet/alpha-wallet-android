package com.alphawallet.app.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.alphawallet.app.entity.FragmentMessenger;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;

public class UpdateUtils {
    public static void checkForUpdates(Activity context, FragmentMessenger messenger) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
            {
                messenger.updateReady(appUpdateInfo.availableVersionCode());
            }
        });
    }

    public static void pushUpdateDialog(Activity activity)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        activity.startActivity(intent);

        /*AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
            {
                appUpdateManager.startUpdateFlow(appUpdateInfo, context, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build());
            }
        });*/
    }
}
