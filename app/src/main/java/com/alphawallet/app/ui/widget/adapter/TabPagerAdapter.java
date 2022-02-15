package com.alphawallet.app.ui.widget.adapter;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class TabPagerAdapter extends FragmentStateAdapter {

    private final List<Pair<String, Fragment>> pages;

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Pair<String, Fragment>> pages) {
        super(fragmentActivity);
        this.pages = pages;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // don't forget to set 'offScreenPageLimit' of viewPager as we are not creating fragments here, just returning existing instances
        return pages.get(position).second;
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }
}
