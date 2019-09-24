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

    private static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static void reflex(final TabLayout tabLayout){
        tabLayout.post(() -> {
            try
            {
                LinearLayout mTabStrip = (LinearLayout) tabLayout.getChildAt(0);
                int dp10 = (int) dipToPixels(tabLayout.getContext(), 10);
                for (int i = 0; i < mTabStrip.getChildCount(); i++)
                {
                    setTextMargins(mTabStrip.getChildAt(i), dp10);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private static void setTextMargins(View tabView, int margin) {
        TextView tv = (TextView) getTextView(tabView);
        if (tv != null)
        {
            tabView.setPadding(0, 0, 0, 0);

            int width = 0;
            width = tv.getWidth();
            if (width == 0)
            {
                tv.measure(0, 0);
                width = tv.getMeasuredWidth();
            }

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabView.getLayoutParams();
            params.width = width;
            params.leftMargin = margin;
            params.rightMargin = margin;
            tabView.setLayoutParams(params);

            tabView.invalidate();
        }
    }

    /**
     * Find the first textView in 'view'
     * @param view Parent view containing text
     * @return
     */
    private static View getTextView(View view)
    {
        View value = null;
        LinearLayout ll = (LinearLayout) view;
        for (int i = 0; i < ll.getChildCount(); i++)
        {
            View child = ll.getChildAt(i);
            if (child instanceof TextView)
            {
                return child;
            }
            if (child instanceof LinearLayout && ((LinearLayout)child).getChildCount() > 0)
            {
                return getTextView(child);
            }
        }

        return value;
    }
}
