package com.alphawallet.app.util;

import android.os.Build;
import android.os.Environment;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;

public class SnapshotUtil {
    public static void take(String testName) {
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        if (!path.exists()) {
            path.mkdirs();
        }

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.takeScreenshot(new File(path, testName + "." + Build.VERSION.SDK_INT + ".png"));
    }
}
