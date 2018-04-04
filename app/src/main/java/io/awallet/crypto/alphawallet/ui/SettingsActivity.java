package io.awallet.crypto.alphawallet.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.router.HomeRouter;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasFragmentInjector;

public class SettingsActivity extends BaseActivity implements HasFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();
        setTitle("Settings");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
//                new TransactionsRouter().open(this, true);
                new HomeRouter().open(this, true);
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
//        new TransactionsRouter().open(this, true);
        new HomeRouter().open(this, true);
        finish();
    }

    @Override
    public AndroidInjector<android.app.Fragment> fragmentInjector() {
        return fragmentInjector;
    }
}
