package com.alphawallet.app.util;


import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
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
        if (tabLayout.getTabCount() <= 3) {
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        } else {
            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        }

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
}
