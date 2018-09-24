package io.stormbird.wallet.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.webkit.URLUtil;

public class Utils {
    public static int dp2px(Context context, int dp) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }

    public static String formatUrl(String url) {
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
            return url;
        } else {
            return "http://" + url;
        }
    }
}
