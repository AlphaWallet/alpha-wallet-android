package com.alphawallet.app.viewmodel;

import com.alphawallet.app.ui.BaseActivity;

import com.alphawallet.app.R;

import com.alphawallet.app.widget.AWalletBottomNavigationView;

public class BaseNavigationActivity extends BaseActivity implements AWalletBottomNavigationView.OnBottomNavigationItemSelectedListener {
    private AWalletBottomNavigationView nav;

    protected void initBottomNavigation() {
        nav = findViewById(R.id.nav);
        nav.setListener(this);
    }

    protected void selectNavigationItem(int position) {
        nav.setSelectedItem(position);
    }

    @Override
    public boolean onBottomNavigationItemSelected(int index) {
        nav.setSelectedItem(index);
        return false;
    }

    protected int getSelectedItem() {
        return nav.getSelectedItem();
    }

    public void setSettingsBadgeCount(int count) {
        nav.setSettingsBadgeCount(count);
    }

    public void addSettingsBadgeKey(String key) {
        nav.addSettingsBadgeKey(key);
    }

    public void removeSettingsBadgeKey(String key) {
        nav.removeSettingsBadgeKey(key);
    }
}