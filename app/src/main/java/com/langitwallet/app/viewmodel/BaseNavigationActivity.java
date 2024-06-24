package com.langitwallet.app.viewmodel;

import android.view.View;

import com.langitwallet.app.R;
import com.langitwallet.app.entity.WalletPage;
import com.langitwallet.app.ui.BaseActivity;
import com.langitwallet.app.widget.AWalletBottomNavigationView;

public class BaseNavigationActivity extends BaseActivity implements AWalletBottomNavigationView.OnBottomNavigationItemSelectedListener {
    private AWalletBottomNavigationView nav;

    protected void initBottomNavigation() {
        nav = findViewById(R.id.nav);
        nav.setListener(this);
    }

    protected void selectNavigationItem(WalletPage position) {
        nav.setSelectedItem(position);
    }

    @Override
    public boolean onBottomNavigationItemSelected(WalletPage index) {
        nav.setSelectedItem(index);
        return false;
    }

    protected WalletPage getSelectedItem() {
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

    public void removeDappBrowser()
    {
        nav.hideBrowserTab();
    }

    public void hideNavBar() { nav.setVisibility(View.GONE); }

    public int getNavBarHeight() { return nav.getHeight(); }

    public boolean isNavBarVisible() { return nav.getVisibility() == View.VISIBLE; }

    public void setNavBarVisibility(int view)
    {
        if (nav == null) nav = findViewById(R.id.nav);
        if (nav != null) nav.setVisibility(view);
    }
}
