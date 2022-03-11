package com.alphawallet.app.ui;

import static androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.C.CHANGE_CURRENCY;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.C.SETTINGS_INSTANTIATED;
import static com.alphawallet.app.C.SHOW_BACKUP;
import static com.alphawallet.app.entity.WalletPage.ACTIVITY;
import static com.alphawallet.app.entity.WalletPage.DAPP_BROWSER;
import static com.alphawallet.app.entity.WalletPage.SETTINGS;
import static com.alphawallet.app.entity.WalletPage.WALLET;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

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
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.service.NotificationService;
import com.alphawallet.app.service.PriceAlertsService;
import com.alphawallet.app.ui.widget.entity.PagerCallback;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.UpdateUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseNavigationActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;
import com.github.florent37.tutoshowcase.TutoShowcase;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

import timber.log.Timber;

@AndroidEntryPoint
public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener, HomeCommsInterface,
        FragmentMessenger, Runnable, SignAuthenticationCallback, LifecycleObserver, PagerCallback
{
    private HomeViewModel viewModel;

    private Dialog dialog;
    private ViewPager2 viewPager;
    private final FragmentStateAdapter pager2Adapter;
    private LinearLayout successOverlay;
    private ImageView successImage;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HomeReceiver homeReceiver;
    private String buildVersion;
    private Fragment settingsFragment;
    private Fragment dappBrowserFragment;
    private Fragment walletFragment;
    private Fragment activityFragment;
    private String walletTitle;
    private static boolean updatePrompt = false;
    private TutoShowcase backupWalletDialog;
    private boolean isForeground;
    private volatile boolean tokenClicked = false;
    private String openLink;
    private boolean inWalletConnect;

    public static final int RC_DOWNLOAD_EXTERNAL_WRITE_PERM = 222;
    public static final int RC_ASSET_EXTERNAL_WRITE_PERM = 223;
    public static final int RC_ASSET_NOTIFICATION_PERM = 224;

    public static final int DAPP_BARCODE_READER_REQUEST_CODE = 1;
    public static final int DAPP_TRANSACTION_SEND_REQUEST = 2;
    public static final String STORED_PAGE = "currentPage";
    public static final String RESET_TOKEN_SERVICE = "HOME_reset_ts";
    public static final String AW_MAGICLINK = "aw.app/";
    public static final String AW_MAGICLINK_DIRECT = "openurl?url=";

    public HomeActivity()
    {
        // fragment creation is shifted to adapter
        pager2Adapter = new ScreenSlidePagerAdapter(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onMoveToForeground()
    {
        Timber.tag("LIFE").d("AlphaWallet into foreground");
        if (viewModel != null)
        {
            viewModel.checkTransactionEngine();
            viewModel.sendMsgPumpToWC(this);
        }
        isForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onMoveToBackground()
    {
        Timber.tag("LIFE").d("AlphaWallet into background");
        if (viewModel != null && !tokenClicked) viewModel.stopTransactionUpdate();
        if (viewModel != null) viewModel.outOfFocus();
        isForeground = false;
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (!isForeground)
        {
            onMoveToBackground();
        }
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            if (viewModel.fullScreenSelected())
            {
                hideSystemUI();
            }
            else
            {
                showSystemUI();
            }
        }
    }

    int fragCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        LocaleUtils.setDeviceLocale(getBaseContext());
        super.onCreate(savedInstanceState);
        LocaleUtils.setActiveLocale(this);
        getLifecycle().addObserver(this);
        isForeground = true;

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewModel = new ViewModelProvider(this)
                .get(HomeViewModel.class);
        viewModel.identify(this);
        viewModel.setWalletStartup();
        viewModel.setCurrencyAndLocale(this);
        viewModel.tryToShowWhatsNewDialog(this);
        setContentView(R.layout.activity_home);

        initViews();
        toolbar();

        viewPager = findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);      // i think this replicates lockPages(true)
        viewPager.setAdapter(pager2Adapter);
        viewPager.setOffscreenPageLimit(WalletPage.values().length);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        initBottomNavigation();
        dissableDisplayHomeAsUp();

        viewModel.error().observe(this, this::onError);
        viewModel.installIntent().observe(this, this::onInstallIntent);
        viewModel.walletName().observe(this, this::onWalletName);
        viewModel.backUpMessage().observe(this, this::onBackup);
        viewModel.splashReset().observe(this, this::onRequireInit);

        if (CustomViewSettings.hideDappBrowser())
        {
            removeDappBrowser();
        }

        KeyboardVisibilityEvent.setEventListener(
                this, isOpen -> {
                    if (isOpen)
                    {
                        setNavBarVisibility(View.GONE);
                        getFragment(WalletPage.values()[viewPager.getCurrentItem()]).softKeyboardVisible();
                    }
                    else
                    {
                        setNavBarVisibility(View.VISIBLE);
                        getFragment(WalletPage.values()[viewPager.getCurrentItem()]).softKeyboardGone();
                    }
                });

        viewModel.tryToShowRateAppDialog(this);
        viewModel.tryToShowEmailPrompt(this, successOverlay, handler, this);

        if (Utils.verifyInstallerId(this))
        {
            UpdateUtils.checkForUpdates(this, this);
        }
        else
        {
            //TODO: Check we are using latest version on github, since we're using a downloaded/manually installed version
            //First check that this the package name is "io.stormbird.wallet" - it could be a fork
        }

        setupFragmentListeners();

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (intent.hasExtra(C.FROM_HOME_ROUTER) && intent.getStringExtra(C.FROM_HOME_ROUTER).equals(C.FROM_HOME_ROUTER))
        {
            viewModel.storeCurrentFragmentId(-1);
        }

        if (data != null)
        {
            String importData = data.toString();
            String importPath = null;
            if (importData.startsWith("content://"))
            {
                importPath = data.getPath();
            }

            checkIntents(importData, importPath, intent);
        }
        
        Intent i = new Intent(this, PriceAlertsService.class);
        startService(i);
    }

    private void setupFragmentListeners()
    {
        //TODO: Move all fragment comms to this model - see all instances of ((HomeActivity)getActivity()).
        getSupportFragmentManager()
                .setFragmentResultListener(RESET_TOKEN_SERVICE, this, (requestKey, b) -> {
                    viewModel.restartTokensService();
                    //trigger wallet adapter reset
                    resetTokens();
                });

        getSupportFragmentManager()
                .setFragmentResultListener(RESET_WALLET, this, (requestKey, b) -> {
                    viewModel.restartTokensService();
                    resetTokens();
                    showPage(WALLET);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(CHANGE_CURRENCY, this, (k, b) -> {
                    resetTokens();
                    showPage(WALLET);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(RESET_TOOLBAR, this, (requestKey, b) -> invalidateOptionsMenu());

        getSupportFragmentManager()
                .setFragmentResultListener(ADDED_TOKEN, this, (requestKey, b) -> {
                    List<ContractLocator> contractList = b.getParcelableArrayList(ADDED_TOKEN);
                    if (contractList != null)
                    {
                        ((ActivityFragment) getFragment(ACTIVITY)).addedToken(contractList);
                    }
                });

        getSupportFragmentManager()
                .setFragmentResultListener(SHOW_BACKUP, this, (requestKey, b) -> showBackupWalletDialog(b.getBoolean(SHOW_BACKUP, false)));

        getSupportFragmentManager()
                .setFragmentResultListener(C.HANDLE_BACKUP, this, (requestKey, b) -> {
                    if (b.getBoolean(C.HANDLE_BACKUP))
                    {
                        backupWalletSuccess(b.getString("Key"));
                    }
                    else
                    {
                        backupWalletFail(b.getString("Key"), b.getBoolean("nolock"));
                    }
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.TOKEN_CLICK, this, (requestKey, b) -> {
                    tokenClicked = true;
                    handler.postDelayed(() -> tokenClicked = false, 10000);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(CHANGED_LOCALE, this, (requestKey, b) -> {
                    viewModel.restartHomeActivity(getApplicationContext());
                });

        getSupportFragmentManager()
                .setFragmentResultListener(SETTINGS_INSTANTIATED, this, (k, b) -> {
                    loadingComplete();
                });
    }

    @Override
    public void onNewIntent(Intent startIntent)
    {
        super.onNewIntent(startIntent);
        Uri data = startIntent.getData();
        String importPath = null;
        String importData = null;

        if (data != null)
        {
            importData = data.toString();
            if (importData.startsWith("content://"))
            {
                importPath = data.getPath();
            }

            checkIntents(importData, importPath, startIntent);
        }
    }

    //First time to use
    private void onRequireInit(Boolean aBoolean)
    {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    private void onBackup(String address)
    {
        if (Utils.isAddressValid(address))
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

    private void showBackupWalletDialog(boolean walletImported)
    {
        if (!viewModel.isFindWalletAddressDialogShown())
        {
            //check if wallet was imported - in which case no need to display
            if (!walletImported)
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
            }
            viewModel.setFindWalletAddressDialogShown(true);
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

        getFragment(WALLET).setToolbarTitle(walletTitle);
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
        viewModel.getWalletName(this);
        viewModel.setErrorCallback(this);
        if (homeReceiver == null)
        {
            homeReceiver = new HomeReceiver(this, this);
        }
        initViews();

        handler.post(() -> {
            //check clipboard
            String magicLink = ImportTokenActivity.getMagiclinkFromClipboard(this);
            if (magicLink != null)
            {
                viewModel.showImportLink(this, magicLink);
            }
        });
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
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(STORED_PAGE, viewPager.getCurrentItem());
        if (getSelectedItem() != null)
        {
            viewModel.storeCurrentFragmentId(getSelectedItem().ordinal());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        int oldPage = savedInstanceState.getInt(STORED_PAGE);
        if (oldPage >= 0 && oldPage < WalletPage.values().length)
        {
            showPage(WalletPage.values()[oldPage]);
        }
    }

    @Override
    public void onClick(View view)
    {

    }

    @Override
    public boolean onBottomNavigationItemSelected(WalletPage index)
    {
        switch (index)
        {
            case DAPP_BROWSER:
            {
                showPage(DAPP_BROWSER);
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

    public void onBrowserWithURL(String url)
    {
        showPage(DAPP_BROWSER);
        ((DappBrowserFragment) getFragment(DAPP_BROWSER)).onItemClick(url);
    }

    @Override
    public void onDestroy()
    {
        if (getSelectedItem() != null) viewModel.storeCurrentFragmentId(getSelectedItem().ordinal());
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
        WalletPage oldPage = WalletPage.values()[viewPager.getCurrentItem()];
        boolean enableDisplayAsHome = false;

        switch (page)
        {
            case DAPP_BROWSER:
                hideToolbar();
                setTitle(getString(R.string.toolbar_header_browser));
                selectNavigationItem(DAPP_BROWSER);
                enableDisplayAsHome = true;
                break;

            default:
                page = WALLET;
            case WALLET:
                showToolbar();
                if (walletTitle == null || walletTitle.isEmpty())
                {
                    setTitle(getString(R.string.toolbar_header_wallet));
                }
                else
                {
                    setTitle(walletTitle);
                }
                selectNavigationItem(WALLET);
                break;

            case SETTINGS:
                showToolbar();
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                break;

            case ACTIVITY:
                showToolbar();
                setTitle(getString(R.string.activity_label));
                selectNavigationItem(ACTIVITY);
                break;
        }

        enableDisplayHomeAsHome(enableDisplayAsHome);
        switchAdapterToPage(page);
        invalidateOptionsMenu();
        checkWarnings();

        signalPageVisibilityChange(oldPage, page);
    }

    //Switch from main looper
    private void switchAdapterToPage(WalletPage page)
    {
        handler.post(() -> viewPager.setCurrentItem(page.ordinal(), false));
    }

    private void signalPageVisibilityChange(WalletPage oldPage, WalletPage newPage)
    {
        BaseFragment inFocus = getFragment(newPage);
        inFocus.comeIntoFocus();

        if (oldPage != newPage)
        {
            BaseFragment leavingFocus = getFragment(oldPage);
            leavingFocus.leaveFocus();
        }
    }

    private void checkWarnings()
    {
        if (updatePrompt)
        {
            hideDialog();
            updatePrompt = false;
            int warns = viewModel.getUpdateWarnings() + 1;
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

            viewModel.setUpdateWarningCount(warns);
        }
    }

    @Override
    public void updateReady(int updateVersion)
    {
        //signal to WalletFragment an update is ready
        //display entry in the WalletView
        ((NewSettingsFragment) getFragment(SETTINGS)).signalUpdate(updateVersion);
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

    void backupWalletFail(String keyBackup, boolean hasNoLock)
    {
        //postpone backup until later
        ((NewSettingsFragment) getFragment(SETTINGS)).backupSeedSuccess(hasNoLock);
        if (keyBackup != null)
        {
            ((WalletFragment) getFragment(WALLET)).remindMeLater(new Wallet(keyBackup));
            viewModel.checkIsBackedUp(keyBackup);
        }
    }

    void backupWalletSuccess(String keyBackup)
    {
        ((NewSettingsFragment) getFragment(SETTINGS)).backupSeedSuccess(false);
        ((WalletFragment) getFragment(WALLET)).storeWalletBackupTime(keyBackup);
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
        Fragment fragment = getFragment(fragmentId);

        getSupportFragmentManager()
                .beginTransaction()
                .detach(fragment)
                .attach(fragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void loadingComplete()
    {
        int lastId = viewModel.getLastFragmentId();
        if (!TextUtils.isEmpty(openLink)) //delayed open link from intent - safe now that all fragments have been initialised
        {
            showPage(DAPP_BROWSER);
            DappBrowserFragment dappFrag = (DappBrowserFragment) getFragment(DAPP_BROWSER);
            if (!dappFrag.isDetached()) dappFrag.loadDirect(openLink);
            openLink = null;
            viewModel.storeCurrentFragmentId(-1);
        }
        else if (getIntent().getBooleanExtra(C.Key.FROM_SETTINGS, false))
        {
            showPage(SETTINGS);
        }
        else if (lastId >= 0 && lastId < WalletPage.values().length)
        {
            showPage(WalletPage.values()[lastId]);
            viewModel.storeCurrentFragmentId(-1);
        }
        else
        {
            showPage(WALLET);
            getFragment(WALLET).comeIntoFocus();
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter
    {
        public ScreenSlidePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (WalletPage.values()[position])
            {
                case WALLET:
                default:
                    walletFragment = new WalletFragment();
                    return walletFragment;
                case ACTIVITY:
                    activityFragment = new ActivityFragment();
                    return activityFragment;
                case DAPP_BROWSER:
                    if (CustomViewSettings.hideDappBrowser()) dappBrowserFragment = new Fragment();
                    else dappBrowserFragment = new DappBrowserFragment();
                    return dappBrowserFragment;
                case SETTINGS:
                    settingsFragment = new NewSettingsFragment();
                    return settingsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return WalletPage.values().length;
        }

    }

    private BaseFragment getFragment(WalletPage page)
    {
        //build map, return correct fragment.
        if (getSupportFragmentManager().getFragments().size() < page.ordinal())
        {
            switch (page)
            {
                default:
                case WALLET:
                    return (BaseFragment)walletFragment;
                case ACTIVITY:
                    return (BaseFragment)activityFragment;
                case DAPP_BROWSER:
                    return (BaseFragment)dappBrowserFragment;
                case SETTINGS:
                    return (BaseFragment)settingsFragment;
            }
        }
        else return (BaseFragment) getSupportFragmentManager().getFragments().get(page.ordinal());
    }

    @Override
    public void downloadReady(String build)
    {
        hideDialog();
        buildVersion = build;
        //display download ready popup
        //Possibly only show this once per day otherwise too annoying!
        int asks = viewModel.getUpdateAsks() + 1;
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
            viewModel.setUpdateAsksCount(asks);
            dialog.dismiss();
        });
        this.dialog = dialog;
        dialog.show();
    }

    @Override
    public void requestNotificationPermission()
    {
        checkNotificationPermission(RC_ASSET_NOTIFICATION_PERM);
    }

    @Override
    public void backupSuccess(String keyAddress)
    {
        if (Utils.isAddressValid(keyAddress)) backupWalletSuccess(keyAddress);
    }

    @Override
    public void resetTokens()
    {
        ((ActivityFragment) getFragment(ACTIVITY)).resetTokens();
        ((WalletFragment) getFragment(WALLET)).resetTokens();
    }

    @Override
    public void resetTransactions()
    {
        ((ActivityFragment) getFragment(ACTIVITY)).resetTransactions();
    }

    @Override
    public void openWalletConnect(String sessionId)
    {
        if (isForeground)
        {
            Intent intent = new Intent(getApplication(), WalletConnectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("session", sessionId);
            startActivity(intent);
        }
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
                Timber.tag("HomeActivity").w("Folder write permission is not granted. Requesting permission");
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
                Timber.tag("HomeActivity").w("Notification permission is not granted. Requesting permission");
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
            case DappBrowserFragment.REQUEST_CAMERA_ACCESS:
                ((DappBrowserFragment) getFragment(DAPP_BROWSER)).gotCameraAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FILE_ACCESS:
                ((DappBrowserFragment) getFragment(DAPP_BROWSER)).gotFileAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FINE_LOCATION:
                ((DappBrowserFragment) getFragment(DAPP_BROWSER)).gotGeoAccess(permissions, grantResults);
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
            if (grantResults[i] == -1) { hasPermission = false; break; }
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
        viewModel.setInstallTime(0);
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
                    ((DappBrowserFragment) getFragment(DAPP_BROWSER)).setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
                }
                break;
            case DAPP_BARCODE_READER_REQUEST_CODE:
                ((DappBrowserFragment) getFragment(DAPP_BROWSER)).handleQRCode(resultCode, data, this);
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
                        ((DappBrowserFragment) getFragment(DAPP_BROWSER)).pinAuthorisation(resultCode == RESULT_OK);
                        break;
                    default:
                        break;
                }
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
                break;
            case C.TOKEN_SEND_ACTIVITY:
                if (data != null && resultCode == Activity.RESULT_OK && data.hasExtra(C.DAPP_URL_LOAD))
                {
                    ((DappBrowserFragment) getFragment(DAPP_BROWSER)).switchNetworkAndLoadUrl(data.getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID),
                                data.getStringExtra(C.DAPP_URL_LOAD));
                    showPage(DAPP_BROWSER);
                }
                else if (data != null && resultCode == Activity.RESULT_OK && data.hasExtra(C.EXTRA_TXHASH))
                {
                    showPage(ACTIVITY);
                }
                break;
            case C.TERMINATE_ACTIVITY:
                if (data != null && resultCode == Activity.RESULT_OK)
                {
                    ((ActivityFragment) getFragment(ACTIVITY)).scrollToTop();
                    showPage(ACTIVITY);
                }
                break;
            case C.ADDED_TOKEN_RETURN:
                if (data != null && data.hasExtra(C.EXTRA_TOKENID_LIST))
                {
                    List<ContractLocator> tokenData = data.getParcelableArrayListExtra(C.EXTRA_TOKENID_LIST);
                    ((ActivityFragment) getFragment(ACTIVITY)).addedToken(tokenData);
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
                    Timber.e(e, "onMenuOpened...unable to set icons for overflow menu");
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
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

    @Override
    public void onBackPressed()
    {
        //Check if current page is WALLET or not
        if (viewPager.getCurrentItem() == DAPP_BROWSER.ordinal())
        {
            ((DappBrowserFragment) getFragment(DAPP_BROWSER)).backPressed();
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

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat inset = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        inset.setSystemBarsBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        inset.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
    }

    private void showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat inset = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        inset.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
    }

    private void checkIntents(String importData, String importPath, Intent startIntent)
    {
        try
        {
            if (importData != null) importData = URLDecoder.decode(importData, "UTF-8");
            DappBrowserFragment dappFrag = (DappBrowserFragment) getFragment(DAPP_BROWSER);
            if (importData != null && importData.startsWith(NotificationService.AWSTARTUP))
            {
                importData = importData.substring(NotificationService.AWSTARTUP.length());
                //move window to token if found
                ((WalletFragment) getFragment(WALLET)).setImportFilename(importData);
            }
            else if (startIntent.getStringExtra("url") != null)
            {
                String url = startIntent.getStringExtra("url");
                showPage(DAPP_BROWSER);
                if (!dappFrag.isDetached()) dappFrag.loadDirect(url);
            }
            else if (importData != null && importData.length() > 22 && importData.contains(AW_MAGICLINK))
            {
                int directLinkIndex = importData.indexOf(AW_MAGICLINK_DIRECT);
                if (directLinkIndex > 0)
                {
                    //get link
                    String link = importData.substring(directLinkIndex + AW_MAGICLINK_DIRECT.length());
                    if (getSupportFragmentManager().getFragments().size() >= DAPP_BROWSER.ordinal())
                    {
                        showPage(DAPP_BROWSER);
                        if (!dappFrag.isDetached()) dappFrag.loadDirect(link);
                    }
                    else
                    {
                        openLink = link; //open link once fragments are initialised
                    }
                }
                else
                {
                    ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
                    if (parser.parseUniversalLink(importData).chainId > 0)
                    {
                        new ImportTokenRouter().open(this, importData);
                        finish();
                    }
                }
            }
            else if (importData != null && importData.startsWith("wc:"))
            {
                WCSession session = WCSession.Companion.from(importData);
                String importPassData = WalletConnectActivity.WC_INTENT + importData;
                Intent intent = new Intent(this, WalletConnectActivity.class);
                intent.putExtra("qrCode", importPassData);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
            else if (importPath != null)
            {
                boolean useAppExternalDir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !viewModel.checkDebugDirectory();
                viewModel.importScriptFile(this, importData, useAppExternalDir);
            }
        }
        catch (SalesOrderMalformed s)
        {
            //No report, expected
        }
        catch (Exception e)
        {
            Timber.tag("Intent").w(e);
        }
    }
}