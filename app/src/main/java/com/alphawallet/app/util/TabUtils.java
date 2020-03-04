package com.alphawallet.app.util;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.alphawallet.app.R;

public class TabUtils {
    public static void changeTabsFont(Context context, TabLayout tabLayout) {
        try {
            Typeface typeface = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                typeface = context.getResources().getFont(R.font.font_regular);
            } else {
                typeface = ResourcesCompat.getFont(context, R.font.font_regular);
            }
            ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
            int tabsCount = vg.getChildCount();
            for (int j = 0; j < tabsCount; j++) {
                ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
                int tabChildsCount = vgTab.getChildCount();
                for (int i = 0; i < tabChildsCount; i++) {
                    View tabViewChild = vgTab.getChildAt(i);
                    if (tabViewChild instanceof TextView) {
                        ((TextView) tabViewChild).setTypeface(typeface);
                    }
                }
            }
        } catch (Resources.NotFoundException nfe) {
            Log.e(TabUtils.class.getSimpleName(), nfe.getMessage(), nfe);
        } catch (NullPointerException npe) {
            Log.e(TabUtils.class.getSimpleName(), npe.getMessage(), npe);
        }
    }
}
