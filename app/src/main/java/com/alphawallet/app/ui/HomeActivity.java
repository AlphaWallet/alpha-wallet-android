package com.alphawallet.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.HomeCommsInterface;
import com.alphawallet.app.entity.HomeReceiver;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.NotificationService;
import com.alphawallet.app.ui.widget.entity.ScrollControlViewPager;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.RootUtil;
import com.alphawallet.app.viewmodel.BaseNavigationActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.viewmodel.HomeViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.DepositView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.tools.ParseMagicLink;
import com.github.florent37.tutoshowcase.TutoShowcase;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.entity.WalletPage.ACTIVITY;
import static com.alphawallet.app.entity.WalletPage.DAPP_BROWSER;
import static com.alphawallet.app.entity.WalletPage.SETTINGS;
import static com.alphawallet.app.entity.WalletPage.WALLET;

public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener, HomeCommsInterface,
        FragmentMessenger, Runnable, SignAuthenticationCallback, LifecycleObserver
{
    @Inject
    HomeViewModelFactory homeViewModelFactory;
    private HomeViewModel viewModel;

    private Dialog dialog;
    private ScrollControlViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private LinearLayout successOverlay;
    private ImageView successImage;
    private final Handler handler = new Handler();
    private HomeReceiver homeReceiver;
    private String buildVersion;
    private final Fragment settingsFragment;
    private final Fragment dappBrowserFragment;
    private final Fragment walletFragment;
    private final Fragment activityFragment;
    private String walletTitle;
    private static boolean updatePrompt = false;
    private TutoShowcase backupWalletDialog;
    private PinAuthenticationCallbackInterface authInterface;
    private int navBarHeight;

    public static final int RC_DOWNLOAD_EXTERNAL_WRITE_PERM = 222;
    public static final int RC_ASSET_EXTERNAL_WRITE_PERM = 223;
    public static final int RC_ASSET_NOTIFICATION_PERM = 224;

    public static final int DAPP_BARCODE_READER_REQUEST_CODE = 1;
    public static final int DAPP_TRANSACTION_SEND_REQUEST = 2;
    public static final String STORED_PAGE = "currentPage";

    public HomeActivity()
    {
        if (CustomViewSettings.hideDappBrowser()) dappBrowserFragment = new Fragment();
        else dappBrowserFragment = new DappBrowserFragment();
        settingsFragment = new NewSettingsFragment();
        walletFragment = new WalletFragment();
        activityFragment = new ActivityFragment();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onMoveToForeground()
    {
        Log.d("LIFE", "AlphaWallet into foreground");
        ((WalletFragment) walletFragment).walletInFocus();
        if (viewModel != null) viewModel.startTransactionUpdate();
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        Log.d("LIFE", "AlphaWallet into background");
        ((WalletFragment) walletFragment).walletOutOfFocus();
        if (viewModel != null) viewModel.stopTransactionUpdate();
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        LocaleUtils.setActiveLocale(this);
        getLifecycle().addObserver(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        Bundle bundle = null;
        Intent intent = getIntent();
        if (intent != null)
        {
            bundle = intent.getExtras();
            Uri data = intent.getData();
            if (data != null)
            {
                String flags = data.toString();
                if (flags.startsWith(NotificationService.AWSTARTUP))
                {
                    flags = flags.substring(NotificationService.AWSTARTUP.length());
                    //move window to token if found
                    ((WalletFragment) walletFragment).setImportFilename(flags);
                }
            }
        }

        viewModel = new ViewModelProvider(this, homeViewModelFactory)
                .get(HomeViewModel.class);
        viewModel.identify(this);

        setContentView(R.layout.activity_home);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        initViews();
        toolbar();

        viewPager = findViewById(R.id.view_pager);
        viewPager.lockPages(true);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(WalletPage.values().length);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {

            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });

        initBottomNavigation();
        dissableDisplayHomeAsUp();

        viewModel.error().observe(this, this::onError);
        viewModel.installIntent().observe(this, this::onInstallIntent);
        viewModel.walletName().observe(this, this::onWalletName);
        viewModel.backUpMessage().observe(this, this::onBackup);

        if (getIntent().getBooleanExtra(C.Key.FROM_SETTINGS, false))
        {
            showPage(SETTINGS);
        }
        else
        {
            WalletPage previousPage = savedInstanceState == null ? WALLET : WalletPage.values()[savedInstanceState.getInt(STORED_PAGE, WALLET.ordinal())];
            showPage(previousPage);
        }

        if (CustomViewSettings.hideDappBrowser())
        {
            removeDappBrowser();
        }
        else if (intent != null && intent.getStringExtra("url") != null)
        {
            String url = getIntent().getStringExtra("url");

            bundle = new Bundle();
            bundle.putString("url", url);
            dappBrowserFragment.setArguments(bundle);
            showPage(DAPP_BROWSER);
            //remove navbar if running as pure browser. clicking back will send you back to the Action/click that took you there
            boolean isNavBarShown = intent.getBooleanExtra("showNavBar", false);
            if (!isNavBarShown) {
                setNavBarVisibility(View.GONE);
                //now remove the bottom margin
                ((DappBrowserFragment)dappBrowserFragment).softKeyboardVisible();
            }
        }

        if (bundle != null)
        {
            getIntent().getExtras();
            String startIntent = bundle.getString("startIntent");
            if (!TextUtils.isEmpty(startIntent) && startIntent.startsWith(WalletConnectActivity.WC_INTENT))
            {
                intent = new Intent(this, WalletConnectActivity.class);
                intent.putExtra("qrCode", startIntent);
                startActivity(intent);
            }
        }

        //Ensure when keyboard appears the webview gets squashed into remaining view. Also remove navigation bar to increase screen size
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        KeyboardVisibilityEvent.setEventListener(
                this, isOpen -> {
                    if (isOpen)
                    {
                        navBarHeight = getNavBarHeight();
                        setNavBarVisibility(View.GONE);
                        ((DappBrowserFragment)dappBrowserFragment).softKeyboardVisible();
                    }
                    else
                    {
                        setNavBarVisibility(View.VISIBLE);
                        ((DappBrowserFragment)dappBrowserFragment).softKeyboardGone(navBarHeight);
                    }
                });
    }

    private void onBackup(String address)
    {
        if (address != null && WalletUtils.isValidAddress(address))
        {
            Toast.makeText(this, getString(R.string.postponed_backup_warning), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews()
    {
        successOverlay = findViewById(R.id.layout_success_overlay);
        successImage = findViewById(R.id.success_image);

        successOverlay.setOnClickListener(view -> {
            //dismiss big green tick
            successOverlay.setVisibility(View.GONE);
        });
    }

    public void showBackupWalletDialog(boolean walletImported)
    {
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

    private void onWalletName(String name)
    {
        if (name != null && !name.isEmpty())
        {
            walletTitle = name;
        }
        else
        {
            walletTitle = getString(R.string.toolbar_header_wallet);
        }

        if (viewPager.getCurrentItem() == WALLET.ordinal())
        {
            setTitle(walletTitle);
        }
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {

    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare();
        viewModel.getWalletName();
        viewModel.setErrorCallback(this);
        if (homeReceiver == null)
        {
            homeReceiver = new HomeReceiver(this, this);
        }
        checkRoot();
        initViews();

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
                            //valid link, remove from clipboard
                            ClipData clipData = ClipData.newPlainText("", "");
                            clipboard.setPrimaryClip(clipData);
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
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putInt(STORED_PAGE, viewPager.getCurrentItem());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.try_again)
        {
            //What is try again?
        }
        else if (view.getId() == R.id.action_buy)
        {
            openExchangeDialog();
        }
    }

    @Override
    public boolean onBottomNavigationItemSelected(WalletPage index)
    {
        switch (index)
        {
            case DAPP_BROWSER:
            {
                if (getSelectedItem() == DAPP_BROWSER)
                {
                    ((DappBrowserFragment) dappBrowserFragment).homePressed();
                }
                else
                {
                    showPage(DAPP_BROWSER);
                }
                ((DappBrowserFragment) dappBrowserFragment).selected();
                return true;
            }
            case WALLET:
            {
                showPage(WALLET);
                return true;
            }
            case SETTINGS:
            {
                showPage(SETTINGS);
                return true;
            }
            case ACTIVITY:
            {
                showPage(ACTIVITY);
                return true;
            }
        }
        return false;
    }

    private void checkRoot()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (RootUtil.isDeviceRooted() && pref.getBoolean("should_show_root_warning", true))
        {
            pref.edit().putBoolean("should_show_root_warning", false).apply();
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setTitle(R.string.root_title);
            dialog.setMessage(R.string.root_body);
            dialog.setButtonText(R.string.ok);
            dialog.setIcon(AWalletAlertDialog.ERROR);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    public void openExchangeDialog()
    {
        Wallet wallet = ((WalletFragment) walletFragment).getCurrentWallet();
        if (wallet == null)
        {
            Toast.makeText(this, getString(R.string.error_wallet_not_selected), Toast.LENGTH_SHORT)
                    .show();
        }
        else
        {
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
        ((DappBrowserFragment) dappBrowserFragment).onItemClick(url);
        dialog.dismiss();
        dialog = null;
    }

    public void onBrowserWithURL(String url)
    {
        showPage(DAPP_BROWSER);
        ((DappBrowserFragment) dappBrowserFragment).onItemClick(url);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onClean();
        if (homeReceiver != null)
        {
            unregisterReceiver(homeReceiver);
            homeReceiver = null;
        }
    }

    private void showPage(WalletPage page)
    {
        switch (page)
        {
            case DAPP_BROWSER:
            {
                hideToolbar();
                viewPager.setCurrentItem(DAPP_BROWSER.ordinal());
                setTitle(getString(R.string.toolbar_header_browser));
                selectNavigationItem(DAPP_BROWSER);
                enableDisplayHomeAsHome(true);
                invalidateOptionsMenu();
                break;
            }
            case WALLET:
            {
                showToolbar();
                viewPager.setCurrentItem(WALLET.ordinal());
                if (walletTitle == null || walletTitle.isEmpty())
                {
                    setTitle(getString(R.string.toolbar_header_wallet));
                }
                else
                {
                    setTitle(walletTitle);
                }
                selectNavigationItem(WALLET);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case SETTINGS:
            {
                showToolbar();
                viewPager.setCurrentItem(SETTINGS.ordinal());
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            case ACTIVITY:
            {
                showToolbar();
                viewPager.setCurrentItem(ACTIVITY.ordinal());
                setTitle(getString(R.string.activity_label));
                selectNavigationItem(ACTIVITY);
                enableDisplayHomeAsHome(false);
                invalidateOptionsMenu();
                break;
            }
            default:
                showToolbar();
                viewPager.setCurrentItem(WALLET.ordinal());
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
                AWalletConfirmationDialog cDialog = new AWalletConfirmationDialog(this);
                cDialog.setTitle(R.string.alphawallet_update);
                cDialog.setCancelable(true);
                cDialog.setSmallText("Using an old version of Alphawallet. Please update from the Play Store or Alphawallet website.");
                cDialog.setPrimaryButtonText(R.string.ok);
                cDialog.setPrimaryButtonListener(v -> {
                    cDialog.dismiss();
                });
                dialog = cDialog;
                dialog.show();
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
        ((ActivityFragment) activityFragment).resetTokens();
    }

    @Override
    public void AddToken(String address)
    {
        viewModel.showAddToken(this, address);
    }

    @Override
    public void tokenScriptError(String message)
    {
        handler.removeCallbacksAndMessages(null); //remove any previous error call, only use final error
        // This is in a runnable because the error will come from non main thread process
        handler.postDelayed(() -> {
            hideDialog();
            AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
            aDialog.setTitle(getString(R.string.tokenscript_file_error));
            aDialog.setMessage(message);
            aDialog.setIcon(AWalletAlertDialog.ERROR);
            aDialog.setButtonText(R.string.button_ok);
            aDialog.setButtonListener(v -> {
                aDialog.dismiss();
            });
            dialog = aDialog;
            dialog.show();
        }, 500);
    }

    private void backupWalletFail(String keyBackup, boolean hasNoLock)
    {
        //postpone backup until later
        ((NewSettingsFragment) settingsFragment).backupSeedSuccess(hasNoLock);
        if (keyBackup != null)
        {
            ((WalletFragment) walletFragment).remindMeLater(new Wallet(keyBackup));
            viewModel.checkIsBackedUp(keyBackup);
        }
    }

    private void backupWalletSuccess(String keyBackup)
    {
        ((NewSettingsFragment) settingsFragment).backupSeedSuccess(false);
        ((WalletFragment) walletFragment).storeWalletBackupTime(keyBackup);
        removeSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
        if (successImage != null) successImage.setImageResource(R.drawable.big_green_tick);
        if (successOverlay != null) successOverlay.setVisibility(View.VISIBLE);
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
        }
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {

    }

    @Override
    public void cancelAuthentication()
    {

    }

    @Override
    public void createdKey(String keyAddress)
    {
        //Key was upgraded
        //viewModel.upgradeWallet(keyAddress);
    }

    /**
     * On restarting the wallet, all fragments check they have their viewModels
     * If they do not, then the onResume override will call this resetFragment method for that fragment
     * Which rebuilds the view and repopulates all the view members required for operation
     *
     * @param fragmentId
     */
    public void resetFragment(WalletPage fragmentId)
    {
        Fragment fragment = getFragment(fragmentId.ordinal());

        getSupportFragmentManager()
                .beginTransaction()
                .detach(fragment)
                .attach(fragment)
                .commitAllowingStateLoss();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter
    {
        public ScreenSlidePagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return getFragment(position);
        }

        @Override
        public int getCount()
        {
            return WalletPage.values().length;
        }
    }

    private Fragment getFragment(int item)
    {
        switch (WalletPage.values()[item])
        {
            case DAPP_BROWSER:
                return dappBrowserFragment;
            case WALLET:
                return walletFragment;
            case SETTINGS:
                return settingsFragment;
            case ACTIVITY:
                return activityFragment;
            default:
                return walletFragment;
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
        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.new_version_title);
        dialog.setSmallText(R.string.new_version);
        String newBuild = "New version: " + build;
        dialog.setMediumText(newBuild);
        dialog.setPrimaryButtonText(R.string.confirm_update);
        dialog.setPrimaryButtonListener(v -> {
            if (checkWritePermission(RC_DOWNLOAD_EXTERNAL_WRITE_PERM))
            {
                viewModel.downloadAndInstall(build, this);
            }
            dialog.dismiss();
        });
        if (asks > 1)
        {
            dialog.setSecondaryButtonText(R.string.dialog_not_again);
        }
        else
        {
            dialog.setSecondaryButtonText(R.string.dialog_later);
        }
        dialog.setSecondaryButtonListener(v -> {
            //only dismiss twice before we stop warning.
            pref.edit().putInt("update_asks", asks).apply();
            dialog.dismiss();
        });
        this.dialog = dialog;
        dialog.show();
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

    @Override
    public void changeCurrency()
    {
        ((WalletFragment) walletFragment).indicateFetch();
        viewModel.updateTickers();
    }

    @Override
    public void resetTokens()
    {
        ((ActivityFragment) activityFragment).resetTokens();
        ((WalletFragment) walletFragment).resetTokens();
    }

    @Override
    public void addedToken(List<ContractLocator> tokenContracts)
    {
        ((ActivityFragment) activityFragment).addedToken(tokenContracts);
    }

    @Override
    public void changedLocale()
    {
        ((WalletFragment) walletFragment).changedLocale();
    }

    @Override
    public void resetTransactions()
    {
        ((ActivityFragment) activityFragment).resetTransactions();
    }

    @Override
    public void refreshTokens()
    {
        ((WalletFragment) walletFragment).refreshTokens();
    }

    @Override
    public void openWalletConnect(String sessionId)
    {
        Intent intent = new Intent(getApplication(), WalletConnectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("session", sessionId);
        startActivity(intent);
    }

    private void hideDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
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
                ((DappBrowserFragment) dappBrowserFragment).gotFileAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FINE_LOCATION:
                ((DappBrowserFragment) dappBrowserFragment).gotGeoAccess(permissions, grantResults);
                break;
            case RC_DOWNLOAD_EXTERNAL_WRITE_PERM:
                if (hasPermission(permissions, grantResults))
                {
                    viewModel.downloadAndInstall(buildVersion, this);
                }
                else
                {
                    showRequirePermissionError();
                }
                break;
            case RC_ASSET_EXTERNAL_WRITE_PERM:
                //Can't get here
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
            case C.SET_GAS_SETTINGS:
                if (data != null)
                {
                    int gasSelectionIndex = data.getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
                    long customNonce = data.getLongExtra(C.EXTRA_NONCE, -1);
                    BigDecimal customGasPrice = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_PRICE));
                    BigDecimal customGasLimit = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_LIMIT));
                    long expectedTxTime = data.getLongExtra(C.EXTRA_AMOUNT, 0);
                    ((DappBrowserFragment) dappBrowserFragment).setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
                }
                break;
            case DAPP_BARCODE_READER_REQUEST_CODE:
                ((DappBrowserFragment) dappBrowserFragment).handleQRCode(resultCode, data, this);
                break;
            case C.REQUEST_SELECT_NETWORK:
                ((DappBrowserFragment) dappBrowserFragment).handleSelectNetwork(resultCode, data);
                break;
            case C.REQUEST_BACKUP_WALLET:
                String keyBackup = null;
                boolean noLockScreen = false;
                if (data != null) keyBackup = data.getStringExtra("Key");
                if (data != null) noLockScreen = data.getBooleanExtra("nolock", false);
                if (resultCode == RESULT_OK) backupWalletSuccess(keyBackup);
                else backupWalletFail(keyBackup, noLockScreen);
                break;
            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                switch (getSelectedItem())
                {
                    case DAPP_BROWSER:
                        ((DappBrowserFragment) dappBrowserFragment).pinAuthorisation(resultCode == RESULT_OK);
                        break;
                    default:
                        //continue with generating the authenticated key - NB currently no flow reaches this code but in future it could
                        if (resultCode == RESULT_OK) authInterface.completeAuthentication(taskCode);
                        else
                        {
                            authInterface.failedAuthentication(taskCode);
                            gotAuthorisation(false);
                        }
                        break;
                }
                break;
            case C.UPDATE_LOCALE:
                updateLocale(data);
                break;
            case C.REQUEST_UNIVERSAL_SCAN:
                if (data != null && resultCode == Activity.RESULT_OK)
                {
                    if (data.hasExtra(C.EXTRA_QR_CODE))
                    {
                        String qrCode = data.getStringExtra(C.EXTRA_QR_CODE);
                        viewModel.handleQRCode(this, qrCode);
                    }
                    else if (data.hasExtra(C.EXTRA_ACTION_NAME))
                    {
                        String action = data.getStringExtra(C.EXTRA_ACTION_NAME);

                        if (action.equalsIgnoreCase(C.ACTION_MY_ADDRESS_SCREEN))
                        {
                            viewModel.showMyAddress(this);
                        }
                    }
                }
            case C.TOKEN_SEND_ACTIVITY:
                if (data != null && resultCode == Activity.RESULT_OK && data.hasExtra(C.DAPP_URL_LOAD))
                {
                    String url = data.getStringExtra(C.DAPP_URL_LOAD);
                    ((DappBrowserFragment)dappBrowserFragment).loadDirect(url);
                    showPage(DAPP_BROWSER);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu)
    {
        if (menu != null)
        {
            if (menu.getClass().getSimpleName().equals("MenuBuilder"))
            {
                try
                {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                }
                catch (Exception e)
                {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }

    public class DepthPageTransformer implements ViewPager.PageTransformer
    {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position)
        {
            int pageWidth = view.getWidth();

            if (position < -1)
            {
                view.setAlpha(0);
            }
            else if (position <= 0)
            {
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);
            }
            else if (position <= 1)
            {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
            else
            {
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

    @Override
    public void onBackPressed()
    {
        //Check if current page is WALLET or not
        if (viewPager.getCurrentItem() == DAPP_BROWSER.ordinal())
        {
            ((DappBrowserFragment)dappBrowserFragment).backPressed();
        }
        else if (viewPager.getCurrentItem() != WALLET.ordinal() && isNavBarVisible())
        {
            showPage(WALLET);
        }
        else
        {
            super.onBackPressed();
        }
    }

    public void useActionSheet(String mode)
    {
        viewModel.actionSheetConfirm(mode);
    }
}