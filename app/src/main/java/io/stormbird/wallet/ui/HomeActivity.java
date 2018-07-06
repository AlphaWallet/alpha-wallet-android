package io.stormbird.wallet.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.util.RootUtil;
import io.stormbird.wallet.viewmodel.BaseNavigationActivity;
import io.stormbird.wallet.viewmodel.HomeViewModel;
import io.stormbird.wallet.viewmodel.HomeViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.DepositView;
import io.stormbird.wallet.widget.SystemView;

import static io.stormbird.wallet.widget.AWalletBottomNavigationView.MARKETPLACE;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.SETTINGS;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.TRANSACTIONS;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.WALLET;

public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener {
    @Inject
    HomeViewModelFactory homeViewModelFactory;
    private HomeViewModel viewModel;

    public static final int RC_HANDLE_EXTERNAL_WRITE_PERM = 22;

    private SystemView systemView;
    private Dialog dialog;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        toolbar();

        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                showPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        initBottomNavigation();
        dissableDisplayHomeAsUp();

        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView = findViewById(R.id.system_view);

        RecyclerView list = findViewById(R.id.list);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, homeViewModelFactory)
                .get(HomeViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.setLocale(this);

        if (getIntent().getBooleanExtra(C.Key.FROM_SETTINGS, false)) {
            showPage(SETTINGS);
        } else {
            showPage(WALLET);
        }

        requestWritePermission();
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {

    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
        checkRoot();
        //check clipboard
        String importData = ImportTokenActivity.getMagiclinkFromClipboard(this);
        if (importData != null)
        {
            //let's try to import the link
            viewModel.showImportLink(this, importData);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                viewModel.showSettings(this);
            }
            break;
            case R.id.action_deposit: {
                openExchangeDialog();
            }
            break;
            case R.id.action_add: {
                viewModel.showAddToken(this);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {

            }
            break;
            case R.id.action_buy: {
                openExchangeDialog();
            }
        }
    }

    @Override
    public boolean onBottomNavigationItemSelected(int index) {
        switch (index) {
            case TRANSACTIONS: {
                showPage(TRANSACTIONS);
                return true;
            }
            case MARKETPLACE: {
                showPage(MARKETPLACE);
                return true;
            }
            case WALLET: {
                showPage(WALLET);
                return true;
            }
            case SETTINGS: {
                showPage(SETTINGS);
                return true;
            }
        }
        return false;
    }

    private void checkRoot() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (RootUtil.isDeviceRooted() && pref.getBoolean("should_show_root_warning", true)) {
            pref.edit().putBoolean("should_show_root_warning", false).apply();
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setTitle(R.string.root_title);
            dialog.setMessage(R.string.root_body);
            dialog.setButtonText(R.string.ok);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    private void openExchangeDialog() {
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet == null) {
            Toast.makeText(this, getString(R.string.error_wallet_not_selected), Toast.LENGTH_SHORT)
                    .show();
        } else {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            DepositView view = new DepositView(this, wallet);
            view.setOnDepositClickListener(this::onDepositClick);
            dialog.setContentView(view);
            dialog.show();
            this.dialog = dialog;
        }
    }

    private void onDepositClick(View view, Uri uri) {
        viewModel.openDeposit(view.getContext(), uri);
    }

    private void showPage(int page) {
        switch (page) {
            case MARKETPLACE: {
                viewPager.setCurrentItem(MARKETPLACE);
                setTitle(getString(R.string.toolbar_header_marketplace));
                selectNavigationItem(MARKETPLACE);
                break;
            }
            case WALLET: {
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                break;
            }
            case SETTINGS: {
                viewPager.setCurrentItem(SETTINGS);
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                break;
            }
            case TRANSACTIONS: {
                viewPager.setCurrentItem(TRANSACTIONS);
                setTitle(getString(R.string.toolbar_header_transactions));
                selectNavigationItem(TRANSACTIONS);
                break;
            }
            default:
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                break;
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case MARKETPLACE:
                    return new MarketplaceFragment();
                case WALLET:
                    return new WalletFragment();
                case SETTINGS:
                    return new NewSettingsFragment();
                case TRANSACTIONS:
                    return new TransactionsFragment();
                default:
                    return new WalletFragment();
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    private void requestWritePermission()
    {
        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                 Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.w("Asset service", "Folder write permission is not granted. Requesting permission");
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_EXTERNAL_WRITE_PERM);
        }
        else
        {
            viewModel.createXMLDirectory();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_HANDLE_EXTERNAL_WRITE_PERM)
        {
            viewModel.createXMLDirectory();
        }
    }
}
