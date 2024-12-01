package com.alphawallet.app.ui;

import static androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.C.RESET_WALLET;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.DeepLinkRequest;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.HomeCommsInterface;
import com.alphawallet.app.entity.HomeReceiver;
import com.alphawallet.app.entity.MediaLinks;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.attestation.AttestationImportInterface;
import com.alphawallet.app.entity.attestation.SmartPassReturn;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.service.DeepLinkService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.PriceAlertsService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.PagerCallback;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.UpdateUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseNavigationActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.Signable;
import com.github.florent37.tutoshowcase.TutoShowcase;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.web3j.utils.Numeric;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener, HomeCommsInterface,
        FragmentMessenger, Runnable, ActionSheetCallback, LifecycleEventObserver, PagerCallback, AttestationImportInterface
{
    @Inject
    AWWalletConnectClient awWalletConnectClient;
    public static final int RC_ASSET_EXTERNAL_WRITE_PERM = 223;
    public static final int RC_ASSET_NOTIFICATION_PERM = 224;
    public static final int DAPP_BARCODE_READER_REQUEST_CODE = 1;
    public static final String STORED_PAGE = "currentPage";
    public static final String RESET_TOKEN_SERVICE = "HOME_reset_ts";
    public static final String AW_MAGICLINK_DIRECT = "openurl?url=";
    private static boolean updatePrompt = false;
    private final FragmentStateAdapter pager2Adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> networkSettingsHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> getSupportFragmentManager().setFragmentResult(RESET_TOKEN_SERVICE, new Bundle()));

    private HomeViewModel viewModel;
    private WalletConnectViewModel viewModelWC;
    private Dialog dialog;
    private ViewPager2 viewPager;
    private LinearLayout successOverlay;
    private ImageView successImage;
    private HomeReceiver homeReceiver;
    private String walletTitle;
    private TutoShowcase backupWalletDialog;
    private boolean isForeground;
    private volatile boolean tokenClicked = false;
    private String openLink = null;
    private String openToken = null;
    private AWalletAlertDialog wcProgressDialog;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted)
            {
                // FCM SDK (and your app) can post notifications.
                viewModel.subscribeToNotifications();
            }
            else
            {
                Toast.makeText(this, getString(R.string.message_deny_request_post_notifications_permission), Toast.LENGTH_LONG).show();
            }
        });

    public HomeActivity()
    {
        // fragment creation is shifted to adapter
        pager2Adapter = new ScreenSlidePagerAdapter(this);
    }

    public static void setUpdatePrompt()
    {
        //TODO: periodically check this value (eg during page flipping)
        //Set alert to user to update their app
        updatePrompt = true;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event)
    {
        switch (event)
        {
            case ON_CREATE:
                break;
            case ON_START:
                Timber.tag("LIFE").d("AlphaWallet into foreground");
                handler.postDelayed(() -> {
                    if (viewModel != null)
                    {
                        viewModel.checkTransactionEngine();
                    }}, 5000);
                isForeground = true;
                break;
            case ON_RESUME:
                break;
            case ON_PAUSE:
                break;
            case ON_STOP:
                Timber.tag("LIFE").d("AlphaWallet into background");
                if (viewModel != null && !tokenClicked) viewModel.stopTransactionUpdate();
                if (viewModel != null) viewModel.outOfFocus();
                isForeground = false;
                break;
            case ON_DESTROY:
                break;
            case ON_ANY:
                break;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        LocaleUtils.setDeviceLocale(getBaseContext());
        super.onCreate(savedInstanceState);
        LocaleUtils.setActiveLocale(this);
        getLifecycle().addObserver(this);
        isForeground = true;
        setWCConnect();

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewModel = new ViewModelProvider(this)
                .get(HomeViewModel.class);
        viewModelWC = new ViewModelProvider(this)
                .get(WalletConnectViewModel.class);

        viewModel.identify();
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
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position)
            {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                super.onPageScrollStateChanged(state);
            }
        });

        initBottomNavigation();
        disableDisplayHomeAsUp();

        viewModel.error().observe(this, this::onError);
        viewModel.walletName().observe(this, this::onWalletName);
        viewModel.backUpMessage().observe(this, this::onBackup);
        viewModel.splashReset().observe(this, this::onRequireInit);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.updateAvailable().observe(this, this::onUpdateAvailable);

        if (CustomViewSettings.hideDappBrowser())
        {
            removeDappBrowser();
        }

        KeyboardVisibilityEvent.setEventListener(
                this, isOpen ->
                {
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
            if (MediaLinks.isMediaTargeted(getApplicationContext()))
            {
                viewModel.checkLatestGithubRelease();
            }
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
            handleDeeplink(data.toString(), intent);
        }

        Intent i = new Intent(this, PriceAlertsService.class);
        try
        {
            startService(i); //cannot start the service while the app is in background mode.
                             //TODO: Improve this patch using JobScheduler
        }
        catch (Exception e)
        {
            Timber.w(e);
        }
    }

    private void onUpdateAvailable(String availableVersion)
    {
        externalUpdateReady(availableVersion);
    }

    private void setWCConnect()
    {
        try
        {
            awWalletConnectClient.setCallback(this);
        }
        catch (Exception e)
        {
            Timber.tag("WalletConnect").e(e);
        }
    }

    private void onDefaultWallet(Wallet wallet)
    {
        // TODO: [Notifications] Uncomment once backend service is implemented
//        if (!viewModel.isWatchOnlyWallet() && !viewModel.isPostNotificationsPermissionRequested(wallet.address))
//        {
//            viewModel.setPostNotificationsPermissionRequested(wallet.address);
//            if (PermissionUtils.requestPostNotificationsPermission(this, requestPermissionLauncher))
//            {
//                viewModel.subscribeToNotifications();
//            }
//        }

        if (viewModel.checkNewWallet(wallet.address))
        {
            viewModel.setNewWallet(wallet.address, false);
            Intent selectNetworkIntent = new Intent(this, NetworkToggleActivity.class);
            selectNetworkIntent.putExtra(C.EXTRA_SINGLE_ITEM, false);
            networkSettingsHandler.launch(selectNetworkIntent);
        }
    }

    private void setupFragmentListeners()
    {
        //TODO: Move all fragment comms to this model - see all instances of ((HomeActivity)getActivity()).
        getSupportFragmentManager()
                .setFragmentResultListener(RESET_TOKEN_SERVICE, this, (requestKey, b) ->
                {
                    viewModel.restartTokensService();
                    //trigger wallet adapter reset
                    resetTokens();
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.RESET_WALLET, this, (requestKey, b) ->
                {
                    viewModel.restartTokensService();
                    resetTokens();
                    showPage(WALLET);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.CHANGE_CURRENCY, this, (k, b) ->
                {
                    resetTokens();
                    showPage(WALLET);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.RESET_TOOLBAR, this, (requestKey, b) -> invalidateOptionsMenu());

        getSupportFragmentManager()
                .setFragmentResultListener(C.ADDED_TOKEN, this, (requestKey, b) ->
                {
                    List<ContractLocator> contractList = b.getParcelableArrayList(ADDED_TOKEN);
                    if (contractList != null)
                    {
                        getFragment(ACTIVITY).addedToken(contractList);
                    }
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.SHOW_BACKUP, this, (requestKey, b) -> showBackupWalletDialog(b.getBoolean(SHOW_BACKUP, false)));

        getSupportFragmentManager()
                .setFragmentResultListener(C.HANDLE_BACKUP, this, (requestKey, b) ->
                {
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
                .setFragmentResultListener(C.TOKEN_CLICK, this, (requestKey, b) ->
                {
                    tokenClicked = true;
                    handler.postDelayed(() -> tokenClicked = false, 10000);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.CHANGED_LOCALE, this, (requestKey, b) ->
                {
                    viewModel.restartHomeActivity(getApplicationContext());
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.SETTINGS_INSTANTIATED, this, (k, b) ->
                {
                    loadingComplete();
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.QRCODE_SCAN, this, (requestKey, b) ->
                {
                    ScanOptions options = Utils.getQRScanOptions(this);
                    hideDialog();
                    qrCodeScanner.launch(options);
                });

        getSupportFragmentManager()
                .setFragmentResultListener(C.AWALLET_CODE, this, (requestKey, b) ->
                {
                    String code = b.getString(C.AWALLET_CODE);
                    handleDeeplink(code, null);
                });
    }

    //TODO: Implement all QR scan using this method
    private final ActivityResultLauncher<ScanOptions> qrCodeScanner = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null)
                {
                    Toast.makeText(this, R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
                }
                else if (viewModel.requiresProcessing(result.getContents()))
                {
                    viewModel.handleQRCode(this, result.getContents());
                }
            });

    @Override
    public void onNewIntent(Intent startIntent)
    {
        super.onNewIntent(startIntent);
        Uri data = startIntent.getData();

        if (data != null)
        {
            handleDeeplink(data.toString(), startIntent);
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

        successOverlay.setOnClickListener(view ->
        {
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
                int background = ContextCompat.getColor(getApplicationContext(), R.color.translucent_dark);
                int statusBarColor = getWindow().getStatusBarColor();
                backupWalletDialog = TutoShowcase.from(this);
                backupWalletDialog.setContentView(R.layout.showcase_backup_wallet)
                        .setBackgroundColor(background)
                        .onClickContentView(R.id.btn_close, view ->
                        {
                            getWindow().setStatusBarColor(statusBarColor);
                            backupWalletDialog.dismiss();
                        })
                        .onClickContentView(R.id.showcase_layout, view ->
                        {
                            getWindow().setStatusBarColor(statusBarColor);
                            backupWalletDialog.dismiss();
                        })
                        .on(R.id.settings_tab)
                        .addCircle()
                        .onClick(v ->
                        {
                            getWindow().setStatusBarColor(statusBarColor);
                            backupWalletDialog.dismiss();
                            showPage(SETTINGS);
                        });
                backupWalletDialog.show();
                getWindow().setStatusBarColor(background);
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
        setWCConnect();
        viewModel.prepare();
        viewModel.getWalletName(this);
        viewModel.setErrorCallback(this);
        if (homeReceiver == null)
        {
            homeReceiver = new HomeReceiver(this, this);
            homeReceiver.register(this);
        }
        initViews();
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
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
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
        getFragment(DAPP_BROWSER).onItemClick(url);
    }

    @Override
    public void onDestroy()
    {
        if (getSelectedItem() != null)
            viewModel.storeCurrentFragmentId(getSelectedItem().ordinal());
        super.onDestroy();
        viewModel.onClean();
        if (homeReceiver != null)
        {
            homeReceiver.unregister(this);
            homeReceiver = null;
        }
    }

    @Override
    public GasService getGasService()
    {
        return viewModel.getGasService();
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
                cDialog.setPrimaryButtonListener(v ->
                {
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
    public void playStoreUpdateReady(int updateVersion)
    {
        //signal to WalletFragment an update is ready
        //display entry in the WalletView
        getFragment(SETTINGS).signalPlayStoreUpdate(updateVersion);
    }

    @Override
    public void externalUpdateReady(String updateVersion)
    {
        getFragment(SETTINGS).signalExternalUpdate(updateVersion);
    }

    @Override
    public void tokenScriptError(String message)
    {
        handler.removeCallbacksAndMessages(null); //remove any previous error call, only use final error
        // This is in a runnable because the error will come from non main thread process
        handler.postDelayed(() ->
        {
            hideDialog();
            AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
            aDialog.setTitle(getString(R.string.tokenscript_file_error));
            aDialog.setMessage(message);
            aDialog.setIcon(AWalletAlertDialog.ERROR);
            aDialog.setButtonText(R.string.button_ok);
            aDialog.setButtonListener(v -> aDialog.dismiss());
            dialog = aDialog;
            dialog.show();
        }, 500);
    }

    void backupWalletFail(String keyBackup, boolean hasNoLock)
    {
        //postpone backup until later
        getFragment(SETTINGS).backupSeedSuccess(hasNoLock);
        if (keyBackup != null)
        {
            getFragment(WALLET).remindMeLater(new Wallet(keyBackup));
            viewModel.checkIsBackedUp(keyBackup);
        }
    }

    void backupWalletSuccess(String keyBackup)
    {
        getFragment(SETTINGS).backupSeedSuccess(false);
        getFragment(WALLET).storeWalletBackupTime(keyBackup);
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
    public void loadingComplete()
    {
        int lastId = viewModel.getLastFragmentId();
        if (!TextUtils.isEmpty(openLink)) //delayed open link from intent - safe now that all fragments have been initialised
        {
            showPage(DAPP_BROWSER);
            getFragment(DAPP_BROWSER).switchNetworkAndLoadUrl(0, openLink);
            openLink = null;
            viewModel.storeCurrentFragmentId(-1);
        }
        else if (!TextUtils.isEmpty(openToken))
        {
            showPage(WALLET);
            getFragment(WALLET).setImportFilename(openToken);
            openToken = null;
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

    private BaseFragment getFragment(WalletPage page)
    {
        // if fragment hasn't been created yet, return a blank BaseFragment to avoid crash
        if ((page.ordinal() + 1) > getSupportFragmentManager().getFragments().size())
        {
            recreate(); //restart activity required
            return new BaseFragment();
        }
        else
        {
            return (BaseFragment) getSupportFragmentManager().getFragments().get(page.ordinal());
        }
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
        getFragment(ACTIVITY).resetTokens();
        getFragment(WALLET).resetTokens();
    }

    @Override
    public void resetTransactions()
    {
        getFragment(ACTIVITY).resetTransactions();
    }

    private void hideDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
        if (wcProgressDialog != null && wcProgressDialog.isShowing())
        {
            wcProgressDialog.dismiss();
        }
    }

    private void checkNotificationPermission(int permissionTag)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
        {
            String[] permissions;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            {
                permissions = new String[]{Manifest.permission.POST_NOTIFICATIONS};
            }
            else
            {
                permissions = new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY};
            }
            requestPermissions(permissions, permissionTag);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case DappBrowserFragment.REQUEST_CAMERA_ACCESS:
                getFragment(DAPP_BROWSER).gotCameraAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FILE_ACCESS:
                getFragment(DAPP_BROWSER).gotFileAccess(permissions, grantResults);
                break;
            case DappBrowserFragment.REQUEST_FINE_LOCATION:
                getFragment(DAPP_BROWSER).gotGeoAccess(permissions, grantResults);
                break;
            case RC_ASSET_EXTERNAL_WRITE_PERM:
                //Can't get here
                break;
            case RC_ASSET_NOTIFICATION_PERM:
                //display import notification
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data); // intercept return intent from PIN/Swipe authentications
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case DAPP_BARCODE_READER_REQUEST_CODE:
                getFragment(DAPP_BROWSER).handleQRCode(resultCode, data, this);
                break;
            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                if (getSelectedItem() == DAPP_BROWSER)
                {
                    getFragment(DAPP_BROWSER).pinAuthorisation(resultCode == RESULT_OK);
                }
                break;
            case C.REQUEST_BACKUP_WALLET:
                String keyBackup = null;
                boolean noLockScreen = false;
                if (data != null) keyBackup = data.getStringExtra("Key");
                if (data != null) noLockScreen = data.getBooleanExtra("nolock", false);
                if (resultCode == RESULT_OK) backupWalletSuccess(keyBackup);
                else backupWalletFail(keyBackup, noLockScreen);
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
                    getFragment(DAPP_BROWSER).switchNetworkAndLoadUrl(data.getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID),
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
                    getFragment(ACTIVITY).scrollToTop();
                    showPage(ACTIVITY);
                }
                break;
            case C.ADDED_TOKEN_RETURN:
                if (data != null && data.hasExtra(C.EXTRA_TOKENID_LIST))
                {
                    List<ContractLocator> tokenData = data.getParcelableArrayListExtra(C.EXTRA_TOKENID_LIST);
                    getFragment(ACTIVITY).addedToken(tokenData);
                }
                else if (data != null && data.getBooleanExtra(RESET_WALLET, false))
                {
                    viewModel.restartTokensService();
                    //trigger wallet adapter reset
                    resetTokens();
                }
                break;
            default:
                //No Action
                break;
        }
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
            getFragment(DAPP_BROWSER).backPressed();
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

    private void hideSystemUI()
    {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat inset = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        inset.setSystemBarsBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        inset.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
    }

    private void showSystemUI()
    {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat inset = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        inset.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
    }

    private void handleDeeplink(String importData, Intent startIntent)
    {
        DeepLinkRequest request = DeepLinkService.parseIntent(importData, startIntent);
        switch (request.type)
        {
            case WALLETCONNECT:
                //Determine if any action is required; if this is not a pairing request then ignore for now
                if (request.data.contains("relay-protocol"))
                {
                    Intent intent = new Intent(this, WalletConnectV2Activity.class);
                    intent.putExtra("url", request.data);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                }
                else
                {
                    walletConnectRequestPending();
                }
                break;
            case SMARTPASS:
                viewModel.handleSmartPass(this, request.data);
                break;
            case URL_REDIRECT:
                viewModel.track(Analytics.Action.DEEP_LINK);
                if (fragmentsInitialised())
                {
                    showPage(DAPP_BROWSER);
                    getFragment(DAPP_BROWSER).switchNetworkAndLoadUrl(0, request.data);
                }
                else
                {
                    openLink = request.data; //open link once fragments are initialised
                }
                break;
            case TOKEN_NOTIFICATION:
                if (fragmentsInitialised())
                {
                    showPage(WALLET);
                    getFragment(WALLET).setImportFilename(request.data);
                }
                else
                {
                    openToken = request.data; //open token once fragments are initialised
                }
                break;
            case WALLET_API_DEEPLINK:
                Intent intent = new Intent(this, ApiV1Activity.class);
                intent.putExtra(C.Key.API_V1_REQUEST_URL, request.data);
                viewModel.track(Analytics.Action.DEEP_LINK_API_V1);
                startActivity(intent);
                break;
            case LEGACY_MAGICLINK:
                new ImportTokenRouter().open(this, request.data);
                finish();
                break;
            case IMPORT_SCRIPT:
                viewModel.importScriptFile(this, startIntent);
                break;
            case INVALID_LINK:
                break;
        }
    }

    @Override
    public void signingComplete(SignatureFromKey signature, Signable message)
    {
        String signHex = Numeric.toHexString(signature.signature);
        Timber.d("Initial Msg: %s", message.getMessage());
        awWalletConnectClient.signComplete(signature, message);
    }

    @Override
    public void signingFailed(Throwable error, Signable message)
    {
        awWalletConnectClient.signFail(error.getMessage(), message);
    }

    @Override
    public WalletType getWalletType()
    {
        return viewModel.defaultWallet().getValue().type;
    }

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModelWC.getAuthenticationForSignature(viewModel.defaultWallet().getValue(), this, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction tx)
    {

    }

    @Override
    public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
    {

    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        if (!actionCompleted)
        {
            awWalletConnectClient.dismissed(callbackId);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {

    }

    //TODO: Implement when passing transactions through here
    ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> { //awWalletConnectClient.setCurrentGasIndex(result));
            });

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter
    {
        public ScreenSlidePagerAdapter(@NonNull FragmentActivity fragmentActivity)
        {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position)
        {
            switch (WalletPage.values()[position])
            {
                case WALLET:
                default:
                    return new WalletFragment();
                case ACTIVITY:
                    return new ActivityFragment();
                case DAPP_BROWSER:
                    if (CustomViewSettings.hideDappBrowser())
                    {
                        return new BaseFragment();
                    }
                    else
                    {
                        return new DappBrowserFragment();
                    }
                case SETTINGS:
                    return new NewSettingsFragment();
            }
        }

        @Override
        public int getItemCount()
        {
            return WalletPage.values().length;
        }
    }

    private boolean fragmentsInitialised()
    {
        return getSupportFragmentManager().getFragments().size() >= SETTINGS.ordinal();
    }

    @Override
    public void attestationImported(TokenCardMeta newToken)
    {
        runOnUiThread(() -> {
            BaseFragment frag = getFragment(WALLET);
            if (frag instanceof WalletFragment)
            {
                ((WalletFragment) frag).updateAttestationMeta(newToken); //add to wallet
            }

            //display import success
            if (dialog == null || !dialog.isShowing())
            {
                AWalletAlertDialog aDialog = new AWalletAlertDialog(this);

                aDialog.setTitle(R.string.attestation_imported);
                aDialog.setIcon(AWalletAlertDialog.SUCCESS);
                aDialog.setButtonText(R.string.button_ok);
                aDialog.setButtonListener(v ->
                        aDialog.dismiss());
                dialog = aDialog;
                dialog.show();
            }
        });
    }

    @Override
    public void importError(String error)
    {
        runOnUiThread(() -> {
            hideDialog();
            AWalletAlertDialog aDialog = new AWalletAlertDialog(this);

            aDialog.setTitle(R.string.attestation_import_failed);
            aDialog.setMessage(error);
            aDialog.setIcon(AWalletAlertDialog.ERROR);
            aDialog.setButtonText(R.string.button_ok);
            aDialog.setButtonListener(v ->
                    aDialog.dismiss());
            dialog = aDialog;
            dialog.show();
        });
    }

    @Override
    public void smartPassValidation(SmartPassReturn validation)
    {
        //Handle smart pass return
        switch (validation)
        {
            case ALREADY_IMPORTED:
                //No need to report anything
                break;
            case IMPORT_SUCCESS:
                importedSmartPass();
                break;
            case IMPORT_FAILED:
                //No need to report anything?
                break;
            case NO_CONNECTION:
                showNoConnection();
                break;
        }
    }

    private void showNoConnection()
    {
        runOnUiThread(() -> {
            AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
            aDialog.setTitle(R.string.no_connection);
            aDialog.setMessage(R.string.no_connection_to_smart_layer);
            aDialog.setIcon(AWalletAlertDialog.WARNING);
            aDialog.setButtonText(R.string.button_ok);
            aDialog.setButtonListener(v ->
                    aDialog.dismiss());
            dialog = aDialog;
            dialog.show();
        });
    }

    private void importedSmartPass()
    {
        runOnUiThread(() -> {
            AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
            aDialog.setTitle(R.string.imported_smart_pass);
            aDialog.setMessage(R.string.smartpass_imported);
            aDialog.setIcon(AWalletAlertDialog.SUCCESS);
            aDialog.setButtonText(R.string.button_ok);
            aDialog.setButtonListener(v ->
                    aDialog.dismiss());
            dialog = aDialog;
            dialog.show();
        });
    }

    //WalletConnect progress
    private void walletConnectRequestPending()
    {
        hideDialog();
        runOnUiThread(() -> {
            wcProgressDialog = new AWalletAlertDialog(this);
            wcProgressDialog.setProgressMode();
            wcProgressDialog.setTitle(R.string.title_wallet_connect);
            wcProgressDialog.setCancelable(false);
            wcProgressDialog.show();
            handler.postDelayed(this::hideDialog, 10000);
        });
    }

    public void clearWalletConnectRequest()
    {
        handler.removeCallbacksAndMessages(null);
        runOnUiThread(this::hideDialog);
    }
}
