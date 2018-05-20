package io.stormbird.wallet.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Utils {
    public static int dp2px(Context context, int dp) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }
}
