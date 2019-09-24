package com.alphawallet.app.ui;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;


public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitle(getTitle());
        }
        enableDisplayHomeAsUp();
        return toolbar;
    }

    protected void setTitle(String title)
    {
        ActionBar actionBar = getSupportActionBar();
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.empty);
            }
            toolbarTitle.setText(title);
        }
    }

    protected void setSubtitle(String subtitle) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    protected void enableDisplayHomeAsUp() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void enableDisplayHomeAsHome(boolean active) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(active);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_browser_home);
        }
    }

    protected void dissableDisplayHomeAsUp() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    protected void hideToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    protected void showToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public void displayToast(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            BaseViewModel.onPushToast(null);
        }
    }

    public void displayMarketQueueSuccessDialog(Integer resId) {
        if (resId != null) {
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setIcon(AWalletAlertDialog.SUCCESS);
            dialog.setTitle(resId);
            dialog.setButtonText(R.string.dialog_go_to_listings);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
            BaseViewModel.onMarketQueueSuccess(null);
        }
    }

    public void displayMarketQueueErrorDialog(Integer resId) {
        if (resId != null) {
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(resId);
            dialog.setButtonText(R.string.dialog_ok);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
            BaseViewModel.onMarketQueueError(null);
        }
    }
}
