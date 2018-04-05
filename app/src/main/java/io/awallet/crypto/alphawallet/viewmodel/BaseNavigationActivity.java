package io.awallet.crypto.alphawallet.viewmodel;

import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.ui.BaseActivity;

public class BaseNavigationActivity extends BaseActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView navigation;

    protected void initBottomNavigation() {
        navigation = findViewById(R.id.bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(this);
    }

    protected void setBottomMenu(@MenuRes int menuRes) {
        navigation.getMenu().clear();
        navigation.inflateMenu(menuRes);
    }

    protected void selectNavigationItem(int position) {
        navigation.getMenu().getItem(position).setChecked(true);
    }

    protected int getSelectedNavigationItem() {
        return navigation.getSelectedItemId();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navigation.setSelectedItemId(item.getItemId());
        return false;
    }
}