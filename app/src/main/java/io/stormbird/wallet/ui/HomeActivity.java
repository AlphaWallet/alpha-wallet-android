package io.stormbird.wallet.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.ui.zxing.QRScanningActivity;
import io.stormbird.wallet.util.RootUtil;
import io.stormbird.wallet.viewmodel.BaseNavigationActivity;
import io.stormbird.wallet.viewmodel.HomeViewModel;
import io.stormbird.wallet.viewmodel.HomeViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.AWalletConfirmationDialog;
import io.stormbird.wallet.widget.DepositView;
import io.stormbird.wallet.widget.SystemView;

import static io.stormbird.wallet.widget.AWalletBottomNavigationView.DAPP_BROWSER;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.MARKETPLACE;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.SETTINGS;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.TRANSACTIONS;
import static io.stormbird.wallet.widget.AWalletBottomNavigationView.WALLET;

public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener, DownloadInterface, FragmentMessenger
{
    @Inject
    HomeViewModelFactory homeViewModelFactory;
    private HomeViewModel viewModel;

    private SystemView systemView;
    private Dialog dialog;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private DownloadReceiver downloadReceiver;
    private AWalletConfirmationDialog cDialog;
    private String buildVersion;
    private final NewSettingsFragment settingsFragment;
    private final DappBrowserFragment dappBrowserFragment;
    private final TransactionsFragment transactionsFragment;
    private final WalletFragment walletFragment;

    public static final int RC_DOWNLOAD_EXTERNAL_WRITE_PERM = 222;
    public static final int RC_ASSET_EXTERNAL_WRITE_PERM = 223;

    public static final int DAPP_BARCODE_READER_REQUEST_CODE = 1;

    public HomeActivity()
    {
        dappBrowserFragment = new DappBrowserFragment();
        transactionsFragment = new TransactionsFragment();
        settingsFragment = new NewSettingsFragment();
        walletFragment = new WalletFragment(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

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
        viewModel.wallets().observe(this, this::onWallets);
        viewModel.setLocale(this);
        viewModel.installIntent().observe(this, this::onInstallIntent);

        if (getIntent().getBooleanExtra(C.Key.FROM_SETTINGS, false)) {
            showPage(SETTINGS);
        } else {
            showPage(WALLET);
        }

        viewModel.loadExternalXMLContracts();
        downloadReceiver = new DownloadReceiver(this, this);

        if (getIntent() != null && getIntent().getStringExtra("url") != null) {
            String url = getIntent().getStringExtra("url");

            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            dappBrowserFragment.setArguments(bundle);
            showPage(DAPP_BROWSER);
        }

        viewModel.refreshWallets();
    }

    private void onWallets(Wallet[] wallets)
    {

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
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        try
        {
            if (clipboard != null && clipboard.getPrimaryClip() != null)
            {
                ClipData.Item clipItem = clipboard.getPrimaryClip().getItemAt(0);
                if (clipItem != null)
                {
                    CharSequence clipText = clipItem.getText();
                    //String importData = ImportTokenActivity.getMagiclinkFromClipboard(this);
                    if (clipText != null && clipText.length() > 60 && clipText.length() < 300)
                    {
                        //let's try to import the link
                        viewModel.showImportLink(this, clipText.toString());
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        switch (viewPager.getCurrentItem())
        {
            case DAPP_BROWSER:
                if (dappBrowserFragment.getUrlIsBookmark())
                {
                    getMenuInflater().inflate(R.menu.menu_added, menu);
                }
                else
                {
                    getMenuInflater().inflate(R.menu.menu_add_bookmark, menu);
                }
                getMenuInflater().inflate(R.menu.menu_bookmarks, menu);
                break;
            default:
                getMenuInflater().inflate(R.menu.menu_add, menu);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                viewModel.showAddToken(this, null);
            }
            break;
            case android.R.id.home: {
                dappBrowserFragment.homePressed();
                return true;
            }
            case R.id.action_add_bookmark: {
                dappBrowserFragment.addBookmark();
                invalidateOptionsMenu();
                return true;
            }
            case R.id.action_bookmarks: {
                dappBrowserFragment.viewBookmarks();
                return true;
            }
            case R.id.action_added: {
                dappBrowserFragment.removeBookmark();
                invalidateOptionsMenu();
                return true;
            }
            case R.id.action_reload: {
                dappBrowserFragment.reloadPage();
                return true;
            }
            case R.id.action_share: {
                dappBrowserFragment.share();
                return true;
            }
            case R.id.action_scan: {
                Intent intent = new Intent(this, QRScanningActivity.class);
                startActivityForResult(intent, DAPP_BARCODE_READER_REQUEST_CODE);
                return true;
            }
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
            case DAPP_BROWSER: {
                showPage(DAPP_BROWSER);
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
            case MARKETPLACE: {
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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }

    private void showPage(int page) {
        switch (page) {
            case DAPP_BROWSER: {
                viewPager.setCurrentItem(DAPP_BROWSER);
                setTitle(getString(R.string.toolbar_header_browser));
                selectNavigationItem(DAPP_BROWSER);
                enableDisplayHomeAsHome(true);
                invalidateOptionsMenu();
                break;
            }
            case WALLET: {
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case SETTINGS: {
                viewPager.setCurrentItem(SETTINGS);
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case TRANSACTIONS: {
                viewPager.setCurrentItem(TRANSACTIONS);
                setTitle(getString(R.string.toolbar_header_transactions));
                selectNavigationItem(TRANSACTIONS);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            default:
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void TokensReady()
    {
        transactionsFragment.tokensReady();
    }

    @Override
    public void AddToken(String address)
    {
        viewModel.showAddToken(this, address);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case DAPP_BROWSER:
                    return dappBrowserFragment;
                case WALLET:
                    return walletFragment;
                case SETTINGS:
                    return settingsFragment;
                case TRANSACTIONS:
                    return transactionsFragment;
                default:
                    return walletFragment;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    @Override
    public void downloadReady(String build)
    {
        hideDialog();
        buildVersion = build;
        //display download ready popup
        //Possibly only show this once per day otherwise too annoying!
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int asks = pref.getInt("update_asks", 0) + 1;
        cDialog = new AWalletConfirmationDialog(this);
        cDialog.setTitle(R.string.new_version_title);
        cDialog.setSmallText(R.string.new_version);
        String newBuild = "New version: " + build;
        cDialog.setMediumText(newBuild);
        cDialog.setPrimaryButtonText(R.string.confirm_update);
        cDialog.setPrimaryButtonListener(v -> {
            if (checkWritePermission(RC_DOWNLOAD_EXTERNAL_WRITE_PERM))
            {
                viewModel.downloadAndInstall(build, this);
            }
            cDialog.dismiss();
        });
        if (asks > 1)
        {
            cDialog.setSecondaryButtonText(R.string.dialog_not_again);
        }
        else
        {
            cDialog.setSecondaryButtonText(R.string.dialog_later);
        }
        cDialog.setSecondaryButtonListener(v -> {
            //only dismiss twice before we stop warning.
            pref.edit().putInt("update_asks", asks).apply();
            cDialog.dismiss();
        });
        cDialog.show();
    }

    @Override
    public void resetToolbar()
    {
        invalidateOptionsMenu();
    }

    private void hideDialog()
    {
        if (cDialog != null && cDialog.isShowing()) {
            cDialog.dismiss();
        }
    }

    private boolean checkWritePermission(int permissionTag)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                     Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                Log.w("HomeActivity", "Folder write permission is not granted. Requesting permission");
                ActivityCompat.requestPermissions(this, permissions, permissionTag);
                return false;
            }
            else
            {
                return true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_DOWNLOAD_EXTERNAL_WRITE_PERM || requestCode == RC_ASSET_EXTERNAL_WRITE_PERM)
        {
            //check permission is granted
            for (int i = 0; i < permissions.length; i++)
            {
                String p = permissions[i];
                if (p.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    if (grantResults[i] != -1)
                    {
                        switch (requestCode)
                        {
                            case RC_ASSET_EXTERNAL_WRITE_PERM:
                                viewModel.loadExternalXMLContracts();
                                settingsFragment.refresh();
                                break;
                            case RC_DOWNLOAD_EXTERNAL_WRITE_PERM:
                                viewModel.downloadAndInstall(buildVersion, this);
                                break;
                        }
                    }
                    else
                    {
                        switch (requestCode)
                        {
                            case RC_ASSET_EXTERNAL_WRITE_PERM:
                                //no warning
                                break;
                            case RC_DOWNLOAD_EXTERNAL_WRITE_PERM:
                                showRequirePermissionError();
                                break;
                        }
                    }
                }
            }
        }
    }

    private void showRequirePermissionError()
    {
        AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setTitle(R.string.install_error);
        aDialog.setMessage(R.string.require_write_permission);
        aDialog.setButtonText(R.string.action_cancel);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
    }

    private void onInstallIntent(File installFile)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            String authority = BuildConfig.APPLICATION_ID + ".fileprovider";
            Uri apkUri = FileProvider.getUriForFile(getApplicationContext(), authority, installFile);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
        else
        {
            Uri apkUri = Uri.fromFile(installFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        //Blank install time here so that next time the app runs the install time will be correctly set up
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.edit().putLong("install_time", 0).apply();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case DAPP_BARCODE_READER_REQUEST_CODE:
                dappBrowserFragment.handleQRCode(resultCode, data, this);
                break;
            default:
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }
}