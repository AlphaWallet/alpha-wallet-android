package com.alphawallet.app.widget.homewidget;


import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

public class JobReceiver extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        try {
            int ordinal = CryptoUpdateService.LOCATION.UPDATE.ordinal();
            String xIntent = String.valueOf(ordinal);

            Intent service = new Intent(this, CryptoUpdateService.class);
            service.setAction(xIntent);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}