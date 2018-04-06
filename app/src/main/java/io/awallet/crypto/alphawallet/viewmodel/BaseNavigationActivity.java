package io.awallet.crypto.alphawallet.viewmodel;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.ui.BaseActivity;
import io.awallet.crypto.alphawallet.widget.AWalletBottomNavigationView;

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
}