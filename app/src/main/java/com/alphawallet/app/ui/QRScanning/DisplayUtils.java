package com.alphawallet.app.ui.QRScanning;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import java.lang.reflect.Method;

public class DisplayUtils
{
    @SuppressWarnings("deprecation")
    public static Point getScreenResolution(Activity activity) {
        Point screenResolution = new Point();
        WindowManager windowManager = activity.getWindowManager();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
        {
            try
            {
                Method m = windowManager.getClass().getDeclaredMethod(
                        "getCurrentWindowMetrics"); //Use reflection to see if method is available

                WindowMetrics windowMetrics = (WindowMetrics) m.invoke(windowManager);
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());

                screenResolution.set(windowMetrics.getBounds().width() - insets.left - insets.right,
                        windowMetrics.getBounds().height() - insets.top - insets.bottom);
            }
            catch (Exception e)
            {
                screenResolution = useLegacyMethod(activity);
            }
        }
        else
        {
            screenResolution = useLegacyMethod(activity);
        }

        return screenResolution;
    }

    private static Point useLegacyMethod(Activity activity)
    {
        Point screenResolution = new Point();
        //Use legacy method
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenResolution.x = displayMetrics.widthPixels;
        screenResolution.y = displayMetrics.heightPixels;
        return screenResolution;
    }
}
