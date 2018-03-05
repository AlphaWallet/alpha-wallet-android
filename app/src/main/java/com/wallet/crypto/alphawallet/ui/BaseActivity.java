package com.wallet.crypto.alphawallet.ui;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;


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

	protected void setTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setTitle(title);
//        }
		TextView toolbarTitle = findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			if (actionBar != null) {
				actionBar.setTitle("");
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
		if (message != null)
		{
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			BaseViewModel.onPushToast(null);
		}
	}
}
