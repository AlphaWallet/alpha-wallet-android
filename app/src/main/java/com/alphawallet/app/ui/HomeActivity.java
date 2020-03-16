package com.alphawallet.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.NotificationService;
import com.github.florent37.tutoshowcase.TutoShowcase;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.HomeCommsInterface;
import com.alphawallet.app.entity.HomeReceiver;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.entity.ScrollControlViewPager;
import com.alphawallet.app.util.RootUtil;

import dagger.android.AndroidInjection;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.BaseNavigationActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.viewmodel.HomeViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.DepositView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;

import org.web3j.crypto.WalletUtils;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Method;

import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.widget.AWalletBottomNavigationView.*;

public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener, HomeCommsInterface, FragmentMessenger, Runnable, SignAuthenticationCallback
{
    @Inject
    HomeViewModelFactory homeViewModelFactory;
    private HomeViewModel viewModel;

    private SystemView systemView;
    private Dialog dialog;
    private ScrollControlViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private LinearLayout successOverlay;
    private ImageView successImage;
    private Handler handler;
    private HomeReceiver homeReceiver;
    private AWalletConfirmationDialog cDialog;
    private String buildVersion;
    private final Fragment settingsFragment;
    private final Fragment dappBrowserFragment;
    private final Fragment transactionsFragment;
    private final Fragment walletFragment;
    private String walletTitle;
    private final LifecycleObserver lifeCycle;
    private static boolean updatePrompt = false;
    private TutoShowcase backupWalletDialog;
    private TutoShowcase findWalletAddressDialog;
    private PinAuthenticationCallbackInterface authInterface;
    private String importFileName;

    public static final int RC_DOWNLOAD_EXTERNAL_WRITE_PERM = 222;
    public static final int RC_ASSET_EXTERNAL_WRITE_PERM = 223;
    public static final int RC_ASSET_NOTIFICATION_PERM = 224;

    public static final int DAPP_BARCODE_READER_REQUEST_CODE = 1;
    public static final int DAPP_TRANSACTION_SEND_REQUEST = 2;

    public HomeActivity()
    {
        importFileName = null;
        if (VisibilityFilter.hideDappBrowser()) dappBrowserFragment = new Fragment();
        else dappBrowserFragment = new DappBrowserFragment();
        transactionsFragment = new TransactionsFragment();
        settingsFragment = new NewSettingsFragment();
        walletFragment = new WalletFragment();
        lifeCycle = new LifecycleObserver()
        {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void onMoveToForeground()
            {
                Log.d("LIFE", "AlphaWallet into foreground");
                ((WalletFragment)walletFragment).walletInFocus();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            private void onMoveToBackground()
            {
                Log.d("LIFE", "AlphaWallet into background");
                ((WalletFragment)walletFragment).walletOutOfFocus();
            }

            @Override
            public int hashCode()
            {
                return super.hashCode();
            }
        };

        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifeCycle);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null)
        {
            String flags = data.toString();
            if (flags.startsWith(NotificationService.AWSTARTUP))
            {
                flags = flags.substring(NotificationService.AWSTARTUP.length());
                //move window to token if found
                ((WalletFragment)walletFragment).setImportFilename(flags);
            }
        }

        viewModel = ViewModelProviders.of(this, homeViewModelFactory)
                .get(HomeViewModel.class);
        viewModel.setLocale(this);

        setContentView(R.layout.activity_home);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        toolbar();

        viewPager = findViewById(R.id.view_pager);
        viewPager.lockPages(true);
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
        findViewById(R.id.toolbar).setBackgroundResource(R.color.colorPrimary);

        RecyclerView list = findViewById(R.id.list);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        systemView.showProgress(false);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.installIntent().observe(this, this::onInstallIntent);
        viewModel.walletName().observe(this, this::onWalletName);
        viewModel.backUpMessage().observe(this, this::onBackup);

        if (getIntent().getBooleanExtra(C.Key.FROM_SETTINGS, false)) {
            showPage(SETTINGS);
        } else {
            showPage(WALLET);
        }

        if (VisibilityFilter.hideDappBrowser())
        {
            removeDappBrowser();
        }
        else if (getIntent() != null && getIntent().getStringExtra("url") != null) {
            String url = getIntent().getStringExtra("url");

            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            dappBrowserFragment.setArguments(bundle);
            showPage(DAPP_BROWSER);
        }

        viewModel.cleanDatabases(this);
    }

    private void onBackup(String address)
    {
        if (address != null && WalletUtils.isValidAddress(address))
        {
            Toast.makeText(this, getString(R.string.postponed_backup_warning), Toast.LENGTH_LONG).show();
        }
    }

    public void showBackupWalletDialog(boolean walletImported) {
        if (!viewModel.isFindWalletAddressDialogShown())
        {
            //check if wallet was imported - in which case no need to display
            if (walletImported)
            {
                viewModel.setFindWalletAddressDialogShown(true);
            }
            else
            {
                int lighterBackground = Color.argb(102, 0, 0, 0); //40% opacity
                backupWalletDialog = TutoShowcase.from(this);
                backupWalletDialog.setContentView(R.layout.showcase_backup_wallet)
                        .setBackgroundColor(lighterBackground)
                        .onClickContentView(R.id.btn_close, view -> {
                            backupWalletDialog.dismiss();
                        })
                        .on(R.id.settings_tab)
                        .addCircle()
                        .onClick(v -> {
                            backupWalletDialog.dismiss();
                            showPage(SETTINGS);
                        })
                        .show();
                viewModel.setFindWalletAddressDialogShown(true);
            }
        }
    }

    private void onWalletName(String name) {
        if (name != null && !name.isEmpty()) {
            walletTitle = name;
        } else {
            walletTitle = getString(R.string.toolbar_header_wallet);
        }

        if (viewPager.getCurrentItem() == WALLET) {
                setTitle(walletTitle);
        }
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {

    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
        viewModel.getWalletName();
        checkRoot();
        successOverlay = findViewById(R.id.layout_success_overlay);
        successImage = findViewById(R.id.success_image);

        successOverlay.setOnClickListener(view -> {
            //dismiss big green tick
            successOverlay.setVisibility(View.GONE);
        });

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
                    if (clipText != null && clipText.length() > 60 && clipText.length() < 400)
                    {
                        ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
                        if (parser.parseUniversalLink(clipText.toString()).chainId > 0) //see if it's a valid link
                        {
                            //let's try to import the link
                            viewModel.showImportLink(this, clipText.toString());
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (homeReceiver == null) homeReceiver = new HomeReceiver(this, this);
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
            case WALLET:
                if (VisibilityFilter.canAddTokens()) getMenuInflater().inflate(R.menu.menu_add, menu);
                break;
            default:
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
                if (getSelectedItem() == DAPP_BROWSER) {
                    ((DappBrowserFragment)dappBrowserFragment).homePressed();
                } else {
                    showPage(DAPP_BROWSER);
                }
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
        Wallet wallet = ((WalletFragment)walletFragment).getCurrentWallet();
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

    private void onDepositClick(View view, String url)
    {
        showPage(DAPP_BROWSER);
        ((DappBrowserFragment)dappBrowserFragment).onItemClick(url);
        dialog.dismiss();
        dialog = null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onClean();
        unregisterReceiver(homeReceiver);
    }

    private void showPage(int page) {
        switch (page) {
            case DAPP_BROWSER: {
                hideToolbar();
                viewPager.setCurrentItem(DAPP_BROWSER);
                setTitle(getString(R.string.toolbar_header_browser));
                selectNavigationItem(DAPP_BROWSER);
                enableDisplayHomeAsHome(true);
                invalidateOptionsMenu();
                break;
            }
            case WALLET: {
                showToolbar();
                viewPager.setCurrentItem(WALLET);
                if (walletTitle == null || walletTitle.isEmpty()) {
                    setTitle(getString(R.string.toolbar_header_wallet));
                }
                else {
                    setTitle(walletTitle);
                }
                selectNavigationItem(WALLET);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case SETTINGS: {
                showToolbar();
                viewPager.setCurrentItem(SETTINGS);
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case TRANSACTIONS: {
                showToolbar();
                viewPager.setCurrentItem(TRANSACTIONS);
                setTitle(getString(R.string.toolbar_header_transactions));
                selectNavigationItem(TRANSACTIONS);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                ((TransactionsFragment)transactionsFragment).transactionsShowing();
                break;
            }
            default:
                showToolbar();
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
        }
        checkWarnings();
    }

    private void checkWarnings()
    {
        if (updatePrompt)
        {
            hideDialog();
            updatePrompt = false;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            int warns = pref.getInt("update_warns", 0) + 1;
            if (warns < 3)
            {
                cDialog = new AWalletConfirmationDialog(this);
                cDialog.setTitle(R.string.alphawallet_update);
                cDialog.setCancelable(true);
                cDialog.setSmallText("Using an old version of Alphawallet. Please update from the Play Store or Alphawallet website.");
                cDialog.setPrimaryButtonText(R.string.ok);
                cDialog.setPrimaryButtonListener(v -> {
                    cDialog.dismiss();
                });
                cDialog.show();
            }
            else if (warns > 10)
            {
                warns = 0;
            }

            pref.edit().putInt("update_warns", warns).apply();
        }
    }

    @Override
    public void TokensReady()
    {
        if (transactionsFragment != null) ((TransactionsFragment)transactionsFragment).resetTokens();
    }

    @Override
    public void AddToken(String address)
    {
        viewModel.showAddToken(this, address);
    }

    private void backupWalletFail(String keyBackup)
    {
        //postpone backup until later
        ((NewSettingsFragment)settingsFragment).backupSeedSuccess();
        if (keyBackup != null)
        {
            ((WalletFragment)walletFragment).remindMeLater(new Wallet(keyBackup));
            viewModel.checkIsBackedUp(keyBackup);
        }
    }

    private void backupWalletSuccess(String keyBackup)
    {
        ((NewSettingsFragment)settingsFragment).backupSeedSuccess();
        ((WalletFragment)walletFragment).storeWalletBackupTime(keyBackup);
        removeSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
        successImage.setImageResource(R.drawable.big_green_tick);
        successOverlay.setVisibility(View.VISIBLE);

        handler = new Handler();
        handler.postDelayed(this, 1000);
    }

    @Override
    public void run()
    {
        if (successOverlay.getAlpha() > 0)
        {
            successOverlay.animate().alpha(0.0f).setDuration(500);
            handler.postDelayed(this, 750);
        }
        else
        {
            successOverlay.setVisibility(View.GONE);
            successOverlay.setAlpha(1.0f);
            handler = null;
        }
    }

    @Override
    public void GotAuthorisation(boolean gotAuth)
    {

    }

    @Override
    public void CreatedKey(String keyAddress)
    {
        //Key was upgraded
        //viewModel.upgradeWallet(keyAddress);
    }

    public void ResetDappBrowser()
    {
        getSupportFragmentManager()
                    .beginTransaction()
                    .detach(dappBrowserFragment)
                    .attach(dappBrowserFragment)
                    .commit();
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

    @Override
    public void requestNotificationPermission()
    {
        checkNotificationPermission(RC_ASSET_NOTIFICATION_PERM);
    }

    @Override
    public void backupSuccess(String keyAddress)
    {
        if (WalletUtils.isValidAddress(keyAddress)) backupWalletSuccess(keyAddress);
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

    private boolean checkNotificationPermission(int permissionTag)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            final String[] permissions = new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                     Manifest.permission.ACCESS_NOTIFICATION_POLICY))
            {
                Log.w("HomeActivity", "Notification permission is not granted. Requesting permission");
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
        switch (requestCode)
        {
            case DappBrowserFragment.REQUEST_FILE_ACCESS:
                ((DappBrowserFragment)dappBrowserFragment).gotFileAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FINE_LOCATION:
                ((DappBrowserFragment)dappBrowserFragment).gotGeoAccess(permissions, grantResults);
                break;
            case RC_DOWNLOAD_EXTERNAL_WRITE_PERM:
                if (hasPermission(permissions, grantResults))
                {
                    ((NewSettingsFragment)settingsFragment).refresh();
                }
                break;
            case RC_ASSET_EXTERNAL_WRITE_PERM:
                if (hasPermission(permissions, grantResults))
                {
                    viewModel.downloadAndInstall(buildVersion, this);
                }
                else
                {
                    showRequirePermissionError();
                }
                break;
        }
    }

    private boolean hasPermission(String[] permissions, int[] grantResults)
    {
        boolean hasPermission = true;
        for (int i = 0; i < permissions.length; i++)
        {
            if (grantResults[i] == -1) hasPermission = false;
        }

        return hasPermission;
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
        Operation taskCode = null;
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case DAPP_BARCODE_READER_REQUEST_CODE:
                ((DappBrowserFragment)dappBrowserFragment).handleQRCode(resultCode, data, this);
                break;
            case C.REQUEST_SELECT_NETWORK:
                ((DappBrowserFragment)dappBrowserFragment).handleSelectNetwork(resultCode, data);
                break;
            case C.REQUEST_BACKUP_WALLET:
                String keyBackup = null;
                if (data != null) keyBackup = data.getStringExtra("Key");
                if (resultCode == RESULT_OK) backupWalletSuccess(keyBackup);
                else backupWalletFail(keyBackup);
                break;
            case C.REQUEST_TRANSACTION_CALLBACK:
                ((DappBrowserFragment)dappBrowserFragment).handleTransactionCallback(resultCode, data);
                break;
            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                switch (getSelectedItem())
                {
                    case DAPP_BROWSER:
                        ((DappBrowserFragment)dappBrowserFragment).GotAuthorisation(resultCode == RESULT_OK);
                        break;
                    default:
                        break;
                }
                break;
            case C.UPDATE_LOCALE:
                updateLocale(data);
                break;
            case C.UPDATE_CURRENCY:
                updateCurrency(data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
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

    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) {
                view.setAlpha(0);
            } else if (position <= 0) {
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);
            } else if (position <= 1) {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0);
            }
        }
    }

    public static void setUpdatePrompt()
    {
        //TODO: periodically check this value (eg during page flipping)
        //Set alert to user to update their app
        updatePrompt = true;
    }

    void postponeWalletBackupWarning(String walletAddress)
    {
        removeSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
    }

    public void updateLocale(Intent data)
    {
        if (data == null) return;
        String newLocale = data.getStringExtra(C.EXTRA_LOCALE);
        sendBroadcast(new Intent(CHANGED_LOCALE));
        viewModel.updateLocale(newLocale, this);
    }

    public void updateCurrency(Intent data)
    {
        if (data == null) return;
        String currencyCode = data.getStringExtra(C.EXTRA_CURRENCY);

        //Check if selected currency code is previous selected one then don't update
        if(viewModel.getDefaultCurrency().equals(currencyCode)) return;

        viewModel.updateCurrency(currencyCode);
        ((WalletFragment)walletFragment).indicateFetch();
    }
}