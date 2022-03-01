package com.alphawallet.app.util;


import android.content.Context;
import android.graphics.Typeface;
import com.google.android.material.tabs.TabLayout;

import androidx.core.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

public class TabUtils {
    public static void setSelectedTabFont(TabLayout tabLayout, TabLayout.Tab tab, Typeface typeface) {
        LinearLayout layout = (LinearLayout) ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(tab.getPosition());
        TextView tabTextView = (TextView) layout.getChildAt(1);
        if (tabTextView != null) {
            tabTextView.setTypeface(typeface);
        }
    }

    public static void decorateTabLayout(Context context, TabLayout tabLayout) {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setSelectedTabFont(tabLayout, tab, ResourcesCompat.getFont(context, R.font.font_semibold));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                setSelectedTabFont(tabLayout, tab, ResourcesCompat.getFont(context, R.font.font_regular));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        TabLayout.Tab firstTab = tabLayout.getTabAt(0);
        if (firstTab != null) {
            TabUtils.setSelectedTabFont(tabLayout, firstTab, ResourcesCompat.getFont(context, R.font.font_semibold));
        }
    }

    public static void setHighlightedTabColor(Context context, TabLayout tabLayout) {
        int tabCount = tabLayout.getTabCount();

        if (tabCount > 3) {
            View tab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(tabCount - 1);
            ViewGroup.MarginLayoutParams tabParams = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
            tabParams.rightMargin = Utils.dp2px(context, 12);
            tab.requestLayout();
        }
    }
}
