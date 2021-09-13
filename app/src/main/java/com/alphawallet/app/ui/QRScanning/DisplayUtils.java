package com.alphawallet.app.ui.QRScanning;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class DisplayUtils
{
    public static Point getScreenResolution(Activity activity) {
        Point screenResolution = new Point();
        WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
        Insets insets = windowMetrics.getWindowInsets()
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());

        screenResolution.set(windowMetrics.getBounds().width() - insets.left - insets.right,
                windowMetrics.getBounds().height() - insets.top - insets.bottom);

        return screenResolution;
    }
}
