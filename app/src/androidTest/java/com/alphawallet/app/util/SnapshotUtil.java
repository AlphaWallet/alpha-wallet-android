package com.alphawallet.app.util;

import android.os.Build;
import android.os.Environment;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;

import timber.log.Timber;

public class SnapshotUtil
{
    public static String SNAPSHOT_DIR = "";

    public static void take(String testName)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        {
            Timber.tag("SnapshotUtil").d("Skipping snapshot for API < 30");
            return;
        }
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + SNAPSHOT_DIR);
        if (!path.exists())
        {
            path.mkdirs();
        }

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.takeScreenshot(new File(path, testName + "." + Build.VERSION.SDK_INT + ".png"));
    }
}
