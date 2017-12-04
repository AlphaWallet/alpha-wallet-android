package com.wallet.crypto.trustapp.views;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.OnTaskCompleted;
import com.wallet.crypto.trustapp.controller.TaskResult;
import com.wallet.crypto.trustapp.controller.TaskStatus;

public class ImportAccountActivity extends AppCompatActivity {

    private final int KEYSTORE_POSITION = 0;
    private final int PRIVATE_KEY_POSITION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_import));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ViewPager viewPager = findViewById(R.id.viewPager);

        //set adapter to your ViewPager
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = findViewById(R.id.tabLayout);

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(KEYSTORE_POSITION);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class TabPagerAdapter extends FragmentPagerAdapter {

        public TabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // Return fragment with respect to position.
        @Override
        public Fragment getItem(int position) {
            switch (position) {

                case KEYSTORE_POSITION: {
                    return new ImportKeystoreFragment();
                }

                case PRIVATE_KEY_POSITION: {
                    return new ImportPrivateKeyFragment();
                }

            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        // This method returns the title of the tab according to its position.
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {

                case KEYSTORE_POSITION: {
                    String keystore = getResources().getString(R.string.tab_keystore);

                    return keystore;
                }

                case PRIVATE_KEY_POSITION: {
                    String privateKey = getResources().getString(R.string.tab_private_key);

                    return privateKey;
                }

            }

            return null;
        }

    }
}
