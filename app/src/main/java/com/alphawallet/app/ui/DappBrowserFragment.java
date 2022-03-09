package com.alphawallet.app.ui;

import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static com.alphawallet.app.entity.tokens.Token.TOKEN_BALANCE_PRECISION;
import static com.alphawallet.app.ui.HomeActivity.RESET_TOKEN_SERVICE;
import static com.alphawallet.app.ui.MyAddressActivity.KEY_ADDRESS;
import static com.alphawallet.app.util.KeyboardUtils.showKeyboard;
import static com.alphawallet.app.util.Utils.isValidUrl;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.URLLoadInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.WalletConnectService;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.OnDappHomeNavClickListener;
import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeInterface;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeLayout;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.DappBrowserViewModel;
import com.alphawallet.app.web3.OnEthCallListener;
import com.alphawallet.app.web3.OnSignMessageListener;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.OnSignTransactionListener;
import com.alphawallet.app.web3.OnSignTypedMessageListener;
import com.alphawallet.app.web3.OnWalletActionListener;
import com.alphawallet.app.web3.OnWalletAddEthereumChainObjectListener;
import com.alphawallet.app.web3.Web3View;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.TestNetDialog;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

@AndroidEntryPoint
public class DappBrowserFragment extends BaseFragment implements OnSignTransactionListener, OnSignPersonalMessageListener,
        OnSignTypedMessageListener, OnSignMessageListener, OnEthCallListener, OnWalletAddEthereumChainObjectListener,
        OnWalletActionListener, URLLoadInterface, ItemClickListener, OnDappHomeNavClickListener, DappBrowserSwipeInterface,
        SignAuthenticationCallback, ActionSheetCallback, TestNetDialog.TestNetDialogCallback
{
    private static final String TAG = DappBrowserFragment.class.getSimpleName();
    private static final String DAPP_BROWSER = "DAPP_BROWSER";
    private static final String MY_DAPPS = "MY_DAPPS";
    private static final String DISCOVER_DAPPS = "DISCOVER_DAPPS";
    private static final String HISTORY = "HISTORY";
    public static final String SEARCH = "SEARCH";
    public static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String CURRENT_FRAGMENT = "currentFragment";
    public static final String DAPP_CLICK = "dapp_click";
    public static final String DAPP_REMOVE_HISTORY = "dapp_remove";
    private static final String CURRENT_URL = "urlInBar";
    private static final String WALLETCONNECT_CHAINID_ERROR = "Error: ChainId missing or not supported";
    private static final long MAGIC_BUNDLE_VAL = 0xACED00D;
    private static final String BUNDLE_FILE = "awbrowse";
    private ValueCallback<Uri[]> uploadMessage;
    private WebChromeClient.FileChooserParams fileChooserParams;
    private RealmResults<RealmToken> realmUpdate;
    private Realm realm = null;

    private ActionSheetDialog confirmationDialog;

    public static final int REQUEST_FILE_ACCESS = 31;
    public static final int REQUEST_FINE_LOCATION = 110;
    public static final int REQUEST_CAMERA_ACCESS = 111;

    /**
     Below object is used to set Animation duration for expand/collapse and rotate
     */
    private final int ANIMATION_DURATION = 100;

    private DappBrowserViewModel viewModel;

    private DappBrowserSwipeLayout swipeRefreshLayout;
    private Web3View web3;
    private AutoCompleteTextView urlTv;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo activeNetwork;
    private AWalletAlertDialog resultDialog;
    private DappBrowserSuggestionsAdapter adapter;
    private AlertDialog chainSwapDialog;
    private String loadOnInit; //Web3 needs to be fully set up and initialised before any dapp loading can be done
    private boolean homePressed;
    private AddEthereumChainPrompt addCustomChainDialog;

    private Toolbar toolbar;
    private ImageView back;
    private ImageView next;
    private ImageView clear;
    private ImageView refresh;
    private FrameLayout webFrame;
    private TextView balance;
    private TextView symbol;
    private View layoutNavigation;
    private GeolocationPermissions.Callback geoCallback = null;
    private PermissionRequest requestCallback = null;
    private String geoOrigin;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String walletConnectSession;
    private boolean focusFlag;

    private String currentWebpageTitle;
    private String currentFragment;

    // These two members are for loading a Dapp with an associated chain change.
    // Some multi-chain Dapps have a watchdog thread that checks the chain
    // This thread stays in operation until a new page load is complete.
    private String loadUrlAfterReload;
    private static volatile long forceChainChange = 0;

    private DAppFunction dAppFunction;

    @Nullable
    private Disposable disposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LocaleUtils.setActiveLocale(getContext());
        super.onCreate(savedInstanceState);
        focusFlag = false;

        getChildFragmentManager()
                .setFragmentResultListener(DAPP_CLICK, this, (requestKey, bundle) -> {
                    DApp dapp = bundle.getParcelable(DAPP_CLICK);
                    DApp removedDapp = bundle.getParcelable(DAPP_REMOVE_HISTORY);
                    addToBackStack(DAPP_BROWSER);
                    if (dapp != null) { loadUrl(dapp.getUrl()); }
                    else if (removedDapp != null) { adapter.removeSuggestion(removedDapp); }
                });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        homePressed = false;
        if (currentFragment == null) currentFragment = DAPP_BROWSER;
        attachFragment(currentFragment);
        if ((web3 == null || viewModel == null) && getActivity() != null) //trigger reload
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.DAPP_BROWSER);
        }
        else
        {
            web3.setWebLoadCallback(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LocaleUtils.setActiveLocale(getContext());
        loadOnInit = null;
        int webViewID = CustomViewSettings.minimiseBrowserURLBar() ? R.layout.fragment_webview_compact : R.layout.fragment_webview;
        View view = inflater.inflate(webViewID, container, false);
        initViewModel();
        initView(view);
        setupAddressBar();

        attachFragment(DAPP_BROWSER);

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            loadOnInit = getArguments().getString("url");
        }

        return view;
    }

    private void attachFragment(String tag) {
        if (tag != null && getHost() != null && getChildFragmentManager().findFragmentByTag(tag) == null)
        {
            Fragment f = null;
            switch (tag)
            {
                case DISCOVER_DAPPS:
                    f = new DiscoverDappsFragment();
                    break;
                case MY_DAPPS:
                    f = new MyDappsFragment();
                    break;
                case HISTORY:
                    f = new BrowserHistoryFragment();
                    break;
                case DAPP_BROWSER: //special case - dapp browser is no fragments loaded
                    addToBackStack(DAPP_BROWSER);
                    break;
            }

            if (f != null && !f.isAdded()) showFragment(f, tag);
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        addToBackStack(tag);
        getChildFragmentManager().beginTransaction()
                .add(R.id.frame, fragment, tag)
                .commit();

        setBackForwardButtons();
    }

    private void detachFragments()
    {
        detachFragment(MY_DAPPS);
        detachFragment(DISCOVER_DAPPS);
        detachFragment(HISTORY);
        detachFragment(SEARCH);
    }

    private void homePressed()
    {
        homePressed = true;
        detachFragments();
        currentFragment = DAPP_BROWSER;
        if (urlTv != null)
            urlTv.getText().clear();
        if (web3 != null)
        {
            resetDappBrowser();
        }

        //blank forward / backward arrows
        setBackForwardButtons();
    }

    @Override
    public void onDappHomeNavClick(int position) {
        detachFragments();
        addToBackStack(DAPP_BROWSER);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onDestroy();
        stopBalanceListener();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
    }

    private void setupMenu(@NotNull View baseView)
    {
        refresh = baseView.findViewById(R.id.refresh);
        final MenuItem reload = toolbar.getMenu().findItem(R.id.action_reload);
        final MenuItem share = toolbar.getMenu().findItem(R.id.action_share);
        final MenuItem scan = toolbar.getMenu().findItem(R.id.action_scan);
        final MenuItem add = toolbar.getMenu().findItem(R.id.action_add_to_my_dapps);
        final MenuItem history = toolbar.getMenu().findItem(R.id.action_history);
        final MenuItem bookmarks = toolbar.getMenu().findItem(R.id.action_my_dapps);
        final MenuItem clearCache = toolbar.getMenu().findItem(R.id.action_clear_cache);
        final MenuItem network = toolbar.getMenu().findItem(R.id.action_network);
        final MenuItem setAsHomePage = toolbar.getMenu().findItem(R.id.action_set_as_homepage);

        if (reload != null) reload.setOnMenuItemClickListener(menuItem -> {
            reloadPage();
            return true;
        });
        if (share != null) share.setOnMenuItemClickListener(menuItem -> {
            if (web3.getUrl() != null && currentFragment != null && currentFragment.equals(DAPP_BROWSER)) {
                if (getContext() != null) viewModel.share(getContext(), web3.getUrl());
            }
            else
            {
                displayNothingToShare();
            }
            return true;
        });
        if (scan != null) scan.setOnMenuItemClickListener(menuItem -> {
            viewModel.startScan(getActivity());
            return true;
        });
        if (add != null) add.setOnMenuItemClickListener(menuItem -> {
            viewModel.addToMyDapps(getContext(), currentWebpageTitle, urlTv.getText().toString());
            return true;
        });
        if (history != null) history.setOnMenuItemClickListener(menuItem -> {
            attachFragment(HISTORY);
            return true;
        });
        if (bookmarks != null) bookmarks.setOnMenuItemClickListener(menuItem -> {
            attachFragment(MY_DAPPS);
            return true;
        });
        if (clearCache != null) clearCache.setOnMenuItemClickListener(menuItem -> {
            viewModel.onClearBrowserCacheClicked(getContext());
            return true;
        });

        if (network != null) {
            network.setOnMenuItemClickListener(menuItem -> {
                openNetworkSelection();
                return true;
            });

            updateNetworkMenuItem();
        }

        if (setAsHomePage != null) {
            setAsHomePage.setOnMenuItemClickListener(menuItem -> {
                viewModel.setHomePage(getContext(), urlTv.getText().toString());
                return true;
            });
        }
    }

    private void updateNetworkMenuItem()
    {
        if (activeNetwork != null)
        {
            toolbar.getMenu().findItem(R.id.action_network).setTitle(getString(R.string.network_menu_item, activeNetwork.getShortName()));
            symbol.setText(activeNetwork.getShortName());
        }
    }

    private void initView(@NotNull View view) {
        web3 = view.findViewById(R.id.web3view);
        urlTv = view.findViewById(R.id.url_tv);
        Bundle savedState = readBundleFromLocal();
        if (savedState != null)
        {
            web3.restoreState(savedState);
            String lastUrl = savedState.getString(CURRENT_URL);
            loadOnInit = TextUtils.isEmpty(lastUrl) ? getDefaultDappUrl() : lastUrl;
        }
        else
        {
            loadOnInit = getDefaultDappUrl();
        }

        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
        webFrame = view.findViewById(R.id.frame);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setRefreshInterface(this);

        toolbar = view.findViewById(R.id.address_bar);
        layoutNavigation = view.findViewById(R.id.layout_navigator);

        View home = view.findViewById(R.id.home);

        home.setOnClickListener(v -> homePressed());

        //If you are wondering about the strange way the menus are inflated - this is required to ensure
        //that the menu text gets created with the correct localisation under every circumstance
        MenuInflater inflater = new MenuInflater(LocaleUtils.getActiveLocaleContext(getContext()));
        if (CustomViewSettings.minimiseBrowserURLBar())
        {
            inflater.inflate(R.menu.menu_scan, toolbar.getMenu());
        }
        else if (getDefaultDappUrl() != null)
        {
            inflater.inflate(R.menu.menu_bookmarks, toolbar.getMenu());
        }
        refresh = view.findViewById(R.id.refresh);


        RelativeLayout layout = view.findViewById(R.id.address_bar_layout);
        layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if (refresh != null) {
            refresh.setOnClickListener(v -> reloadPage());
        }

        back = view.findViewById(R.id.back);
        back.setOnClickListener(v -> backPressed());

        next = view.findViewById(R.id.next);
        next.setOnClickListener(v -> goToNextPage());

        clear = view.findViewById(R.id.clear_url);
        clear.setOnClickListener(v -> {
            clearAddressBar();
        });

        balance = view.findViewById(R.id.balance);
        symbol = view.findViewById(R.id.symbol);
        web3.setWebLoadCallback(this);

        webFrame.setOnApplyWindowInsetsListener(resizeListener);

        setupMenu(view);
    }

    private void displayNothingToShare() {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setTitle(getString(R.string.nothing_to_share));
        resultDialog.setMessage(getString(R.string.nothing_to_share_message));
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    private void openNetworkSelection() {
        Intent intent = new Intent(getContext(), SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        if (activeNetwork != null) intent.putExtra(C.EXTRA_CHAIN_ID, activeNetwork.chainId);
        getNetwork.launch(intent);
    }

    private void clearAddressBar() {
        if (urlTv.getText().toString().isEmpty()) {
            cancelSearchSession();
        } else {
            urlTv.getText().clear();
            openURLInputView();
            KeyboardUtils.showKeyboard(urlTv); //ensure keyboard shows here so we can listen for it being cancelled
        }
    }

    private void setupAddressBar() {
        adapter = new DappBrowserSuggestionsAdapter(
                getContext(),
                viewModel.getDappsMasterList(getContext()),
                this::onItemClick
        );
        urlTv.setAdapter(null);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO)
            {
                String urlText = urlTv.getText().toString();
                handled = loadUrl(urlText);
                detachFragments();
                cancelSearchSession();
            }
            return handled;
        });

        // Both these are required, the onFocus listener is required to respond to the first click.
        urlTv.setOnFocusChangeListener((v, hasFocus) -> {
            //see if we have focus flag
            if (hasFocus && focusFlag && getActivity() != null) openURLInputView();
        });

        urlTv.setOnClickListener(v -> {
            openURLInputView();
        });

        urlTv.setShowSoftInputOnFocus(true);

        urlTv.setOnLongClickListener(v -> {
            urlTv.dismissDropDown();
            return false;
        });

        urlTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                adapter.setHighlighted(editable.toString());
            }
        });
    }

    @Override
    public void comeIntoFocus()
    {
        if (viewModel != null)
        {
            if (viewModel.getActiveNetwork() == null || activeNetwork.chainId != viewModel.getActiveNetwork().chainId)
            {
                viewModel.checkForNetworkChanges();
            }
            else
            {
                viewModel.startBalanceUpdate();
                startBalanceListener();
            }
        }
        if (urlTv != null)
        {
            urlTv.clearFocus();
            KeyboardUtils.hideKeyboard(urlTv);
        }
        focusFlag = true;
    }

    @Override
    public void leaveFocus()
    {
        focusFlag = false;
        if (web3 != null) web3.requestFocus();
        if (urlTv != null) urlTv.clearFocus();
        if (viewModel != null) viewModel.stopBalanceUpdate();
        stopBalanceListener();
    }

    // TODO: Move all nav stuff to widget
    private void openURLInputView() {
        urlTv.setAdapter(null);
        expandCollapseView(layoutNavigation, false);

        disposable = Observable.zip(
                Observable.interval(600, TimeUnit.MILLISECONDS).take(1),
                Observable.fromArray(clear), (interval, item) -> item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::postBeginSearchSession);
    }

    private void postBeginSearchSession(@NotNull ImageView item)
    {
        urlTv.setAdapter(adapter);
        urlTv.showDropDown();
        if (item.getVisibility() == View.GONE)
        {
            expandCollapseView(item, true);
            showKeyboard(urlTv);
        }
    }

    /**
     * Used to expand or collapse the view
     */
    private synchronized void expandCollapseView(@NotNull View view, boolean expandView)
    {
        //detect if view is expanded or collapsed
        boolean isViewExpanded = view.getVisibility() == View.VISIBLE;

        //Collapse view
        if (isViewExpanded && !expandView)
        {
            int finalWidth = view.getWidth();
            ValueAnimator valueAnimator = slideAnimator(finalWidth, 0, view);
            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            valueAnimator.start();
        }
        //Expand view
        else if (!isViewExpanded && expandView)
        {
            view.setVisibility(View.VISIBLE);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            view.measure(widthSpec, heightSpec);
            int width = view.getMeasuredWidth();
            ValueAnimator valueAnimator = slideAnimator(0, width, view);
            valueAnimator.start();
        }
    }

    @NotNull
    private ValueAnimator slideAnimator(int start, int end, final View view) {

        final ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(valueAnimator -> {
            // Update Height
            int value = (Integer) valueAnimator.getAnimatedValue();

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = value;
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }

    private void addToBackStack(String nextFragment)
    {
        if (currentFragment != null && !currentFragment.equals(DAPP_BROWSER))
        {
            detachFragment(currentFragment);
        }
        currentFragment = nextFragment;
    }

    private void addToForwardStack(String prevFragment)
    {
        currentFragment = prevFragment;
    }

    private void cancelSearchSession() {
        detachFragment(SEARCH);
        KeyboardUtils.hideKeyboard(urlTv);
        setBackForwardButtons();
    }

    private void shrinkSearchBar()
    {
        if (toolbar != null)
        {
            toolbar.getMenu().setGroupVisible(R.id.dapp_browser_menu, true);
            expandCollapseView(layoutNavigation, true);
            clear.setVisibility(View.GONE);
            urlTv.dismissDropDown();
        }
    }

    private void detachFragment(String tag) {
        if (!isAdded()) return; //the dappBrowserFragment itself may not yet be attached.
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment.isVisible() && !fragment.isDetached()) {
            fragment.onDetach();
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }

        //fragments can only be 1 deep
        currentFragment = DAPP_BROWSER;
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(DappBrowserViewModel.class);
        viewModel.activeNetwork().observe(getViewLifecycleOwner(), this::onNetworkChanged);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        activeNetwork = viewModel.getActiveNetwork();
        viewModel.findWallet();
    }

    private void startBalanceListener()
    {
        if (wallet == null || activeNetwork == null) return;
        if (realm == null || realm.isClosed()) realm = viewModel.getRealmInstance(wallet);

        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();
        realmUpdate = realm.where(RealmToken.class)
                .equalTo("address", TokensRealmSource.databaseKey(activeNetwork.chainId, "eth")).findAllAsync();
        realmUpdate.addChangeListener(realmTokens -> {
            //update balance
            if (realmTokens.size() == 0) return;
            RealmToken realmToken = realmTokens.first();
            balance.setVisibility(View.VISIBLE);
            symbol.setVisibility(View.VISIBLE);
            String newBalanceStr = BalanceUtils.getScaledValueFixed(new BigDecimal(realmToken.getBalance()), ETHER_DECIMALS, TOKEN_BALANCE_PRECISION);
            balance.setText(newBalanceStr);
            symbol.setText(activeNetwork.getShortName());
        });
    }

    private void stopBalanceListener()
    {
        if (realmUpdate != null)
        {
            realmUpdate.removeAllChangeListeners();
            realmUpdate = null;
        }

        if (realm != null && !realm.isClosed()) realm.close();
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        if (activeNetwork != null)
        {
            boolean needsReload = loadOnInit == null;
            setupWeb3();
            if (needsReload) reloadPage();
        }
    }

    public void switchNetworkAndLoadUrl(long chainId, String url)
    {
        forceChainChange = chainId; //avoid prompt to change chain for 1inch
        loadUrlAfterReload = url;   //after reload with new chain inject, page is clean to load the correct site

        if (viewModel == null)
        {
            initViewModel();
            return;
        }

        activeNetwork = viewModel.getNetworkInfo(chainId);
        updateNetworkMenuItem();
        viewModel.setNetwork(chainId);
        startBalanceListener();
        setupWeb3();
        web3.resetView();
        web3.reload();
    }

    private void onNetworkChanged(NetworkInfo networkInfo)
    {
        boolean networkChanged = networkInfo != null && (activeNetwork == null || activeNetwork.chainId != networkInfo.chainId);
        this.activeNetwork = networkInfo;
        if (networkInfo != null)
        {
            if (networkChanged) {
                viewModel.findWallet();
                updateNetworkMenuItem();
            }

            if (networkChanged && isOnHomePage())
                resetDappBrowser(); //trigger a reset if on homepage

            updateFilters(networkInfo);
        }
        else
        {
            openNetworkSelection();
            resetDappBrowser();
        }
    }

    private void updateFilters(NetworkInfo networkInfo)
    {
        if (networkInfo.hasRealValue() && !viewModel.isMainNetsSelected())
        {
            //switch to main net, no need to ask user
            viewModel.setMainNetsSelected(true);
        }

        viewModel.addNetworkToFilters(networkInfo);
        getParentFragmentManager().setFragmentResult(RESET_TOKEN_SERVICE, new Bundle()); //reset tokens service and wallet page with updated filters
    }

    ActivityResultLauncher<Intent> getNewNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                long networkId = result.getData().getLongExtra(C.EXTRA_CHAIN_ID, 1);
                loadNewNetwork(networkId);
            });

    private void launchNetworkPicker()
    {
        Intent intent = new Intent(getContext(), SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        if (activeNetwork != null) intent.putExtra(C.EXTRA_CHAIN_ID, activeNetwork.chainId);
        getNewNetwork.launch(intent);
    }

    private void launchWalletConnectSessionCancel()
    {
        String sessionId = walletConnectSession != null ? viewModel.getSessionId(walletConnectSession) : "";
        Intent bIntent = new Intent(getContext(), WalletConnectService.class);
        bIntent.setAction(String.valueOf(WalletConnectActions.CLOSE.ordinal()));
        bIntent.putExtra("session", sessionId);
        getContext().startService(bIntent);
        reloadPage();
    }

    private void displayCloseWC()
    {
        handler.post(() -> {
            if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
            resultDialog = new AWalletAlertDialog(getContext());
            resultDialog.setIcon(WARNING);
            resultDialog.setTitle(R.string.title_wallet_connect);
            resultDialog.setMessage(getString(R.string.unsupported_walletconnect));
            resultDialog.setButtonText(R.string.button_ok);
            resultDialog.setButtonListener(v -> {
                launchWalletConnectSessionCancel();
                launchNetworkPicker();
                resultDialog.dismiss();
            });
            resultDialog.show();
        });
    }

    private void setupWeb3() {
        web3.setChainId(activeNetwork.chainId);
        web3.setRpcUrl(viewModel.getNetworkNodeRPC(activeNetwork.chainId));
        web3.setWalletAddress(new Address(wallet.address));

        web3.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView webview, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (refresh != null) {
                        refresh.setEnabled(true);
                    }
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                    swipeRefreshLayout.setRefreshing(true);
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage msg)
            {
                boolean ret = super.onConsoleMessage(msg);

                if (msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                {
                    if (msg.message().contains(WALLETCONNECT_CHAINID_ERROR))
                    {
                        displayCloseWC();
                    }
                }

                return ret;
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                currentWebpageTitle = title;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                requestCameraPermission(request);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback)
            {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                requestGeoPermission(origin, callback);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fCParams)
            {
                if (filePathCallback == null) return true;
                uploadMessage = filePathCallback;
                fileChooserParams = fCParams;
                if (checkReadPermission()) return requestUpload();
                else return true;
            }
        });

        web3.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String[] prefixCheck = url.split(":");
                if (prefixCheck.length > 1)
                {
                    Intent intent;
                    switch (prefixCheck[0])
                    {
                        case C.DAPP_PREFIX_TELEPHONE:
                            intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse(url));
                            startActivity(Intent.createChooser(intent, "Call " + prefixCheck[1]));
                            return true;
                        case C.DAPP_PREFIX_MAILTO:
                            intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse(url));
                            startActivity(Intent.createChooser(intent, "Email: " + prefixCheck[1]));
                            return true;
                        case C.DAPP_PREFIX_ALPHAWALLET:
                            if(prefixCheck[1].equals(C.DAPP_SUFFIX_RECEIVE)) {
                                viewModel.showMyAddress(getContext());
                                return true;
                            }
                            break;
                        case C.DAPP_PREFIX_WALLETCONNECT:
                            //start walletconnect
                            if (wallet.type == WalletType.WATCH)
                            {
                                showWalletWatch();
                            }
                            else
                            {
                                walletConnectSession = url;
                                if (getContext() != null)
                                    viewModel.handleWalletConnect(getContext(), url, activeNetwork);
                            }
                            return true;
                        default:
                            break;
                    }
                }

                setUrlText(url);
                return false;
            }
        });

        web3.setOnSignMessageListener(this);
        web3.setOnSignPersonalMessageListener(this);
        web3.setOnSignTransactionListener(this);
        web3.setOnSignTypedMessageListener(this);
        web3.setOnEthCallListener(this);
        web3.setOnWalletAddEthereumChainObjectListener(this);
        web3.setOnWalletActionListener(this);

        if (loadOnInit != null)
        {
            web3.clearCache(false); //on restart with stored app, we usually need this
            addToBackStack(DAPP_BROWSER);
            web3.resetView();
            web3.loadUrl(Utils.formatUrl(loadOnInit));
            setUrlText(Utils.formatUrl(loadOnInit));
            loadOnInit = null;
        }
    }

    private void setUrlText(String newUrl)
    {
        if (urlTv == null)
        {
            if (getView() == null) return; //unable to get view at this time
            urlTv = getView().findViewById(R.id.url_tv);
        }
        urlTv.setText(newUrl);
        setBackForwardButtons();
    }

    ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) uploadMessage.onReceiveValue(new Uri[] { uri });
                }
            });

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                long networkId = result.getData().getLongExtra(C.EXTRA_CHAIN_ID, 1);
                forceChainChange = networkId;
                loadNewNetwork(networkId);
                //might have adjusted the filters
                getParentFragmentManager().setFragmentResult(RESET_TOKEN_SERVICE, new Bundle());
            });

    private void loadNewNetwork(long newNetworkId)
    {
        if (activeNetwork == null || activeNetwork.chainId != newNetworkId)
        {
            balance.setVisibility(View.GONE);
            symbol.setVisibility(View.GONE);
            viewModel.setNetwork(newNetworkId);
            onNetworkChanged(viewModel.getNetworkInfo(newNetworkId));
            startBalanceListener();
        }
        //refresh URL page
        reloadPage();
    }

    protected boolean requestUpload()
    {
        try
        {
            getContent.launch(determineMimeType(fileChooserParams));
        }
        catch (ActivityNotFoundException e)
        {
            uploadMessage = null;
            Toast.makeText(requireActivity().getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public void setCurrentGasIndex(int gasSelectionIndex, BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime, long customNonce)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
        }
    }

    @Override
    public void onSignMessage(final EthereumMessage message) {
        handleSignMessage(message);
    }

    @Override
    public void onSignPersonalMessage(final EthereumMessage message) {
        handleSignMessage(message);
    }

    @Override
    public void onSignTypedMessage(@NotNull EthereumTypedMessage message)
    {
        if (message.getPrehash() == null || message.getMessageType() == SignMessageType.SIGN_ERROR)
        {
            web3.onSignCancel(message.getCallbackId());
        }
        else
        {
            handleSignMessage(message);
        }
    }

    @Override
    public void onEthCall(Web3Call call)
    {
        Single.fromCallable(() -> {
            //let's make the call
            Web3j web3j = TokenRepository.getWeb3jService(activeNetwork.chainId);
            //construct call
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, call.to.toString(), call.payload);
            return web3j.ethCall(transaction, call.blockParam).send();
        }).map(EthCall::getValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> web3.onCallFunctionSuccessful(call.leafPosition, result),
                        error -> web3.onCallFunctionError(call.leafPosition, error.getMessage()))
                .isDisposed();
    }

    @Override
    public void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObj)
    {
        // read chain value
        long chainId = chainObj.getChainId();
        final NetworkInfo info = viewModel.getNetworkInfo(chainId);

        if (forceChainChange != 0 || getContext() == null)
        {
            return; //No action if chain change is forced
        }

        // handle unknown network
        if (info == null) {
            // show add custom chain dialog
            addCustomChainDialog = new AddEthereumChainPrompt(getContext(), chainObj, chainObject -> {
                viewModel.addCustomChain(chainObject);
                loadNewNetwork(chainObj.getChainId());
                addCustomChainDialog.dismiss();
            });
            addCustomChainDialog.show();
        }
        else
        {
            changeChainRequest(callbackId, info);
        }
    }

    private void changeChainRequest(long callbackId, NetworkInfo info)
    {
        //Don't show dialog if network doesn't need to be changed or if already showing
        if ((activeNetwork != null && activeNetwork.chainId == info.chainId) || (chainSwapDialog != null && chainSwapDialog.isShowing()))
        {
            web3.onWalletActionSuccessful(callbackId, null);
            return;
        }

        //if we're switching between mainnet and testnet we need to pop open the 'switch to testnet' dialog (class TestNetDialog)
        // - after the user switches to testnet, go straight to switching the network (loadNewNetwork)
        // - if user is switching form testnet to mainnet, simply add the title below

        // at this stage, we know if it's testnet or not
        if (!info.hasRealValue() && (activeNetwork != null && activeNetwork.hasRealValue()))
        {
            TestNetDialog testnetDialog = new TestNetDialog(requireContext(), info.chainId, this);
            testnetDialog.show();
        }
        else
        {
            //go straight to chain change dialog
            showChainChangeDialog(callbackId, info);
        }
    }

    @Override
    public void onRequestAccounts(long callbackId)
    {
        //TODO: Pop open dialog which asks user to confirm they wish to expose their address to this dapp eg:
        //title = "Request Account Address"
        //message = "${dappUrl} requests your address. \nAuthorise?"
        //if user authorises, then do an evaluateJavascript to populate the web3.eth.getCoinbase with the current address,
        //and additionally add a window.ethereum.setAddress function in init.js to set up addresses
        //together with this update, also need to track which websites have been given permission, and if they already have it (can probably get away with using SharedPrefs)
        //then automatically perform with step without a dialog (ie same as it does currently)
        web3.onWalletActionSuccessful(callbackId, "[" + wallet.address + "]");
    }

    //EIP-3326
    @Override
    public void onWalletSwitchEthereumChain(long callbackId, WalletAddEthereumChainObject chainObj)
    {
        //request user to change chains
        long chainId = chainObj.getChainId();
        final NetworkInfo info = viewModel.getNetworkInfo(chainId);
        if (info == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.unknown_network_title)
                    .setMessage(getString(R.string.unknown_network, String.valueOf(chainId)))
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        if (chainSwapDialog != null && chainSwapDialog.isShowing()) chainSwapDialog.dismiss();
                    })
                    .setCancelable(true);

            chainSwapDialog = builder.create();
            chainSwapDialog.show();
        }
        else
        {
            changeChainRequest(callbackId, info);
        }
    }

    /**
     *
     * This will pop the ActionSheetDialog to request a chain change, with appropriate warning
     * if switching between mainnets and testnets
     *
     * @param callbackId
     * @param newNetwork
     */
    private void showChainChangeDialog(long callbackId, NetworkInfo newNetwork)
    {
        Token baseToken = viewModel.getTokenService().getTokenOrBase(newNetwork.chainId, wallet.address);
        String message = getString(R.string.request_change_chain, newNetwork.name, String.valueOf(newNetwork.chainId));
        if (newNetwork.hasRealValue() && !activeNetwork.hasRealValue())
        {
            message += "\n" + getString(R.string.warning_switch_to_main);
        }
        else if (!newNetwork.hasRealValue() && activeNetwork.hasRealValue())
        {
            message += "\n" + getString(R.string.warning_switching_to_test);
        }

        confirmationDialog = new ActionSheetDialog(requireActivity(), this, R.string.switch_chain_request, message, R.string.switch_and_reload,
                                callbackId, baseToken);

        confirmationDialog.setCanceledOnTouchOutside(true);
        confirmationDialog.show();
        confirmationDialog.fullExpand();
    }

    private void handleSignMessage(Signable message)
    {
        dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Signable message) {
                web3.onSignCancel(message.getCallbackId());
                confirmationDialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message) {
                String signHex = Numeric.toHexString(data);
                Timber.d("Initial Msg: %s", message.getMessage());
                web3.onSignMessageSuccessful(message, signHex);

                confirmationDialog.success();
            }
        };

        if (confirmationDialog == null || !confirmationDialog.isShowing())
        {
            confirmationDialog = new ActionSheetDialog(requireActivity(), this, this, message);
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
            confirmationDialog.fullExpand();
        }
    }

    @Override
    public void onSignTransaction(Web3Transaction transaction, String url)
    {
        try
        {
            viewModel.updateGasPrice(activeNetwork.chainId); //start updating gas price right before we open
            //TODO: Ensure we have received gas price before continuing
            //minimum for transaction to be valid: recipient and value or payload
            if ((confirmationDialog == null || !confirmationDialog.isShowing()) &&
                    (transaction.recipient.equals(Address.EMPTY) && transaction.payload != null) // Constructor
                    || (!transaction.recipient.equals(Address.EMPTY) && (transaction.payload != null || transaction.value != null))) // Raw or Function TX
            {
                Token token = viewModel.getTokenService().getTokenOrBase(activeNetwork.chainId, transaction.recipient.toString());
                confirmationDialog = new ActionSheetDialog(requireActivity(), transaction, token,
                        "", transaction.recipient.toString(), viewModel.getTokenService(), this);
                confirmationDialog.setURL(url);
                confirmationDialog.setCanceledOnTouchOutside(false);
                confirmationDialog.show();
                confirmationDialog.fullExpand();

                viewModel.calculateGasEstimate(wallet, Numeric.hexStringToByteArray(transaction.payload),
                        activeNetwork.chainId, transaction.recipient.toString(), new BigDecimal(transaction.value), transaction.gasLimit)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(estimate -> confirmationDialog.setGasEstimate(estimate),
                                Throwable::printStackTrace)
                        .isDisposed();

                return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        onInvalidTransaction(transaction);
        web3.onSignCancel(transaction.leafPosition);
    }

    /**
     * Debug function for assisting testing
     *
     * @param tx
     * @return
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private Web3Transaction getDebugTx(@NotNull Web3Transaction tx)
    {
        return new Web3Transaction(
                tx.recipient,
                tx.contract,
                tx.value,
                BigInteger.ZERO,//tx.gasPrice,
                tx.gasLimit,
                tx.nonce,
                tx.payload,
                tx.leafPosition
        );
    }

    //Transaction failed to be sent
    private void txError(Throwable throwable)
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(requireContext());
        resultDialog.setIcon(ERROR);
        resultDialog.setTitle(R.string.error_transaction_failed);
        resultDialog.setMessage(throwable.getMessage());
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();

        if (confirmationDialog != null && confirmationDialog.isShowing()) confirmationDialog.dismiss();
    }

    private void showWalletWatch()
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(requireContext());
        resultDialog.setIcon(AWalletAlertDialog.WARNING);
        resultDialog.setTitle(R.string.title_wallet_connect);
        resultDialog.setMessage(R.string.action_watch_account);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();
    }

    private void onInvalidTransaction(Web3Transaction transaction)
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.invalid_transaction));

        if (transaction.recipient.equals(Address.EMPTY) && (transaction.payload == null || transaction.value != null))
        {
            resultDialog.setMessage(getString(R.string.contains_no_recipient));
        }
        else if (transaction.payload == null && transaction.value == null)
        {
            resultDialog.setMessage(getString(R.string.contains_no_value));
        }
        else
        {
            resultDialog.setMessage(getString(R.string.contains_no_data));
        }
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    public void backPressed()
    {
        if (web3 == null || back == null || back.getAlpha() == 0.3f) return;
        if (!currentFragment.equals(DAPP_BROWSER))
        {
            detachFragment(currentFragment);
            checkBackClickArrowVisibility();
        }
        else if (web3.canGoBack())
        {
            checkBackClickArrowVisibility(); //to make arrows function correctly - don't want to wait for web page to load to check back/forwards - this looks clunky
            loadSessionUrl(-1);
            web3.goBack();
            detachFragments();
        }
        else if (!web3.getUrl().equalsIgnoreCase(getDefaultDappUrl()))
        {
            //load homepage
            homePressed = true;
            web3.resetView();
            web3.loadUrl(getDefaultDappUrl());
            setUrlText(getDefaultDappUrl());
            checkBackClickArrowVisibility();
        }
        else
        {
            checkBackClickArrowVisibility();
        }
    }

    private void goToNextPage()
    {
        if (next.getAlpha() == 0.3f) return;
        if (web3.canGoForward())
        {
            checkForwardClickArrowVisibility();
            loadSessionUrl(1);
            web3.goForward();
        }
    }

    /**
     * Check if this is the last web item and the last fragment item.
     */
    private void checkBackClickArrowVisibility()
    {
        //will this be last item?
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int nextIndex = sessionHistory.getCurrentIndex() - 1;

        String nextUrl;

        if (nextIndex >= 0)
        {
            WebHistoryItem newItem = sessionHistory.getItemAtIndex(nextIndex);
            nextUrl = newItem.getUrl();
        }
        else
        {
            nextUrl = urlTv.getText().toString();// web3.getUrl();// getDefaultDappUrl();
        }

        if (nextUrl.equalsIgnoreCase(getDefaultDappUrl()))
        {
            back.setAlpha(0.3f);
        }
        else
        {
            back.setAlpha(1.0f);
        }
    }

    /**
     * After a forward click while web browser active, check if forward and back arrows should be updated.
     * Note that the web item only becomes history after the next page is loaded, so if the next item is new, then
     */
    private void checkForwardClickArrowVisibility()
    {
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int nextIndex = sessionHistory.getCurrentIndex() + 1;
        if (nextIndex >= sessionHistory.getSize() - 1) next.setAlpha(0.3f);
        else next.setAlpha(1.0f);
    }

    /**
     * Browse to relative entry with sanity check on value
     * @param relative relative addition or subtraction of browsing index
     */
    private void loadSessionUrl(int relative)
    {
        WebBackForwardList sessionHistory = web3.copyBackForwardList();
        int newIndex = sessionHistory.getCurrentIndex() + relative;
        if (newIndex < sessionHistory.getSize())
        {
            WebHistoryItem newItem = sessionHistory.getItemAtIndex(newIndex);
            if (newItem != null)
            {
                setUrlText(newItem.getUrl());
            }
        }
    }

    @Override
    public void onWebpageLoaded(String url, String title)
    {
        if (getContext() == null) return; //could be a late return from dead fragment
        if (homePressed)
        {
            homePressed = false;
            if (currentFragment.equals(DAPP_BROWSER) && url.equals(getDefaultDappUrl()))
            {
                web3.clearHistory();
            }
        }

        if (isValidUrl(url))
        {
            DApp dapp = new DApp(title, url);
            DappBrowserUtils.addToHistory(getContext(), dapp);
            adapter.addSuggestion(dapp);
        }

        onWebpageLoadComplete();

        if (urlTv != null) urlTv.setText(url);
    }

    @Override
    public void onWebpageLoadComplete()
    {
        handler.post(() -> {
            setBackForwardButtons();
            if (loadUrlAfterReload != null)
            {
                loadUrl(loadUrlAfterReload);
                loadUrlAfterReload = null;
            }
        }); //execute on UI thread

        if (forceChainChange != 0)
        {
            handler.postDelayed(() -> forceChainChange = 0, 5000);
        }
    }

    private void setBackForwardButtons()
    {
        WebBackForwardList sessionHistory;
        boolean canBrowseBack = false;
        boolean canBrowseForward = false;

        if (currentFragment != null && !currentFragment.equals(DAPP_BROWSER))
        {
            canBrowseBack = true;
        }
        else if (web3 != null)
        {
            sessionHistory = web3.copyBackForwardList();
            canBrowseBack = !isOnHomePage();
            canBrowseForward = (sessionHistory != null && sessionHistory.getCurrentIndex() < sessionHistory.getSize() - 1);
        }

        if (back != null)
        {
            if (canBrowseBack)
            {
                back.setAlpha(1.0f);
            }
            else
            {
                back.setAlpha(0.3f);
            }
        }

        if (next != null)
        {
            if (canBrowseForward)
            {
                next.setAlpha(1.0f);
            }
            else
            {
                next.setAlpha(0.3f);
            }
        }
    }

    private boolean isOnHomePage()
    {
        if (web3 != null)
        {
            String url = web3.getUrl();
            return EthereumNetworkRepository.isDefaultDapp(url);
        }
        else
        {
            return false;
        }
    }

    private boolean loadUrl(String urlText)
    {
        detachFragments();
        addToBackStack(DAPP_BROWSER);
        cancelSearchSession();
        if (checkForMagicLink(urlText)) return true;
        web3.resetView();
        web3.loadUrl(Utils.formatUrl(urlText));
        setUrlText(Utils.formatUrl(urlText));
        web3.requestFocus();
        getParentFragmentManager().setFragmentResult(RESET_TOOLBAR, new Bundle());
        return true;
    }

    public void loadDirect(String urlText)
    {
        if (web3 == null)
        {
            if (getActivity() != null) ((HomeActivity)getActivity()).resetFragment(WalletPage.DAPP_BROWSER);
            loadOnInit = urlText;
        }
        else
        {
            // reset initial url, to avoid issues with initial load
            loadOnInit = null;
            cancelSearchSession();
            addToBackStack(DAPP_BROWSER);
            setUrlText(Utils.formatUrl(urlText));
            web3.resetView();
            web3.loadUrl(Utils.formatUrl(urlText));
            //ensure focus isn't on the keyboard
            KeyboardUtils.hideKeyboard(urlTv);
            web3.requestFocus();
        }
    }

    public void reloadPage() {
        if (currentFragment.equals(DAPP_BROWSER))
        {
            if (refresh != null) {
                refresh.setEnabled(false);
            }
            web3.resetView();
            web3.reload();
        }
    }

    @Override
    public void onItemClick(String url)
    {
        addToBackStack(DAPP_BROWSER);
        loadUrl(url);
    }

    public void testRecoverAddressFromSignature(@NotNull String message, String sig)
    {
        String prefix = PERSONAL_MESSAGE_PREFIX + message.length();
        byte[] msgHash = (prefix + message).getBytes();

        byte[] signatureBytes = Numeric.hexStringToByteArray(sig);
        Sign.SignatureData sd = sigFromByteArray(signatureBytes);
        String addressRecovered;

        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(msgHash, sd);
            addressRecovered = "0x" + Keys.getAddress(recoveredKey);
            Timber.d("Recovered: " + addressRecovered);
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
    }

    private void resetDappBrowser()
    {
        web3.clearHistory();
        web3.stopLoading();
        web3.resetView();
        web3.loadUrl(getDefaultDappUrl());
        setUrlText(getDefaultDappUrl());
    }

    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger)
    {
        //result
        String qrCode = null;
        try
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    if (data != null)
                    {
                        qrCode = data.getStringExtra(C.EXTRA_QR_CODE);
                        if (qrCode == null || checkForMagicLink(qrCode)) return;
                        QRParser parser = QRParser.getInstance(EthereumNetworkRepository.extraChains());
                        QRResult result = parser.parse(qrCode);
                        switch (result.type)
                        {
                            case ADDRESS:
                                //ethereum address was scanned. In dapp browser what do we do? maybe populate an input field with address?
                                copyToClipboard(result.getAddress());
                                break;
                            case PAYMENT:
                                //EIP681 payment request scanned, should go to send
                                viewModel.showSend(getContext(), result);
                                break;
                            case TRANSFER:
                                //EIP681 transfer, go to send
                                viewModel.showSend(getContext(), result);
                                break;
                            case FUNCTION_CALL:
                                //EIP681 function call. TODO: create function call confirmation. For now treat same way as tokenscript function call
                                break;
                            case URL:
                                loadUrlRemote(qrCode);
                                break;
                            case OTHER:
                                qrCode = null;
                                break;
                        }
                    }
                    break;
                case QRScanner.DENY_PERMISSION:
                    showCameraDenied();
                    break;
                case QRScanner.WALLET_CONNECT:
                    return;
                default:
                    break;
            }
        }
        catch (Exception e)
        {
            qrCode = null;
        }

        if (qrCode == null && getActivity() != null)
        {
            Toast.makeText(getActivity(), R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads URL from remote process; this converts a request to load URL which isn't on the app's thread
     * @param qrCode
     */
    private void loadUrlRemote(final String qrCode)
    {
        handler.post(() -> loadUrl(qrCode));
    }

    private void showCameraDenied()
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setTitle(R.string.title_dialog_error);
        resultDialog.setMessage(R.string.error_camera_permission_denied);
        resultDialog.setIcon(ERROR);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();
    }

    private void copyToClipboard(String address)
    {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private boolean checkForMagicLink(String data)
    {
        try
        {
            ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
            if (parser.parseUniversalLink(data).chainId > 0) //see if it's a valid link
            {
                //handle magic link import
                viewModel.showImportLink(getActivity(), data);
                return true;
            }
        }
        catch (SalesOrderMalformed e)
        {
            //
        }

        return false;
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    private boolean checkReadPermission()
    {
        if (ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            requireActivity().requestPermissions(permissions, REQUEST_FILE_ACCESS);
            return false;
        }
    }

    // Handles the requesting of the fine location permission.
    // Note: If you intend allowing geo-location in your app you need to ask the permission.
    private void requestGeoPermission(String origin, GeolocationPermissions.Callback callback)
    {
        if (ContextCompat.checkSelfPermission(requireContext().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            geoCallback = callback;
            geoOrigin = origin;
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            requireActivity().requestPermissions(permissions, REQUEST_FINE_LOCATION);
        }
        else
        {
            callback.invoke(origin, true, false);
        }
    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission(@NotNull PermissionRequest request)
    {
        final String[] requestedResources = request.getResources();
        requestCallback = request;
        for (String r : requestedResources)
        {
            if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            {
                final String[] permissions = new String[]{Manifest.permission.CAMERA};
                requireActivity().requestPermissions(permissions, REQUEST_CAMERA_ACCESS);
            }
        }
    }

    public void gotCameraAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean cameraAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] != -1)
            {
                cameraAccess = true;
                if (requestCallback != null) requestCallback.grant(requestCallback.getResources()); //now we can grant permission
            }
        }
        if (!cameraAccess) Toast.makeText(getContext(), "Permission not given", Toast.LENGTH_SHORT).show();
    }

    public void gotGeoAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean geoAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] != -1)
            {
                geoAccess = true;
                break;
            }
        }
        if (!geoAccess) Toast.makeText(getContext(), "Permission not given", Toast.LENGTH_SHORT).show();
        if (geoCallback != null && geoOrigin != null) geoCallback.invoke(geoOrigin, geoAccess, false);
    }

    public void gotFileAccess(@NotNull String[] permissions, int[] grantResults)
    {
        boolean fileAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] != -1)
            {
                fileAccess = true;
                break;
            }
        }

        if (fileAccess) requestUpload();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        web3.saveState(outState);
        //serialise the bundle and store locally
        writeBundleToLocalStorage(outState);

        super.onSaveInstanceState(outState);
    }

    private void writeBundleToLocalStorage(Bundle bundle)
    {
        File file = new File(requireContext().getFilesDir(), BUNDLE_FILE);
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            getSerialisedBundle(bundle).writeTo(fos);
        }
        catch (Exception e)
        {
            //
        }
    }

    private ByteArrayOutputStream getSerialisedBundle(Bundle bundle) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
            oos.writeObject(MAGIC_BUNDLE_VAL);
            Object item;
            for (String key : bundle.keySet())
            {
                item = bundle.get(key);
                if (item instanceof Serializable)
                {
                    oos.writeObject(key);
                    try
                    {
                        oos.writeObject(item);
                    }
                    catch (Exception e)
                    {
                        oos.writeObject(0);
                    }
                }
            }
            oos.writeObject(CURRENT_FRAGMENT);
            oos.writeObject(currentFragment);
            oos.writeObject(CURRENT_URL);
            String uurl = urlTv.getText().toString();
            String uurl2 = web3.getUrl();
            oos.writeObject(urlTv.getText().toString());
        }
        return bos;
    }

    private Bundle readBundleFromLocal()
    {
        File file = new File(requireContext().getFilesDir(), BUNDLE_FILE);
        try (FileInputStream fis = new FileInputStream(file))
        {
            ObjectInputStream oos = new ObjectInputStream(fis);
            Object check = oos.readObject();
            if(!((Long)MAGIC_BUNDLE_VAL).equals(check)) {
                return null;
            }

            Bundle bundle = new Bundle();

            while(fis.available()>0)
            {
                String key = (String) oos.readObject();
                Object val = oos.readObject();
                if(key!=null && val instanceof Serializable && !((Integer)0).equals(val)) {
                    bundle.putSerializable(key, (Serializable) val);
                }
            }

            return bundle;
        }
        catch (Exception e)
        {
            //
        }

        return null;
    }

    @Override
    public void RefreshEvent()
    {
        //determine scroll position
        Timber.tag("Touch").i("SCROLL: %s", web3.getScrollY());
        if (web3.getScrollY() == 0)
        {
            loadUrl(web3.getUrl());
        }
    }

    @Override
    public int getCurrentScrollPosition()
    {
        return web3.getScrollY();
    }

    // this is called when the signing is approved by the user (e.g. fingerprint / PIN)
    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.dismiss();
        }
    }

    @Override
    public void gotAuthorisationForSigning(boolean gotAuth, Signable messageToSign)
    {
        if (gotAuth)
        {
            viewModel.completeAuthentication(SIGN_DATA);
            viewModel.signMessage(messageToSign, dAppFunction);
        }
        else
        {
            web3.onSignCancel(messageToSign.getCallbackId());
        }
    }

    /**
     * Endpoint from PIN/Swipe authorisation
     * @param gotAuth
     */
    public void pinAuthorisation(boolean gotAuth)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.completeSignRequest(gotAuth);
        }
    }

    @Override
    public void buttonClick(long callbackId, Token baseToken)
    {
        //handle button click
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.dismiss();
        }

        //switch network
        loadNewNetwork(baseToken.tokenInfo.chainId);
        web3.onWalletActionSuccessful(callbackId, null);
    }

    @Override
    public void cancelAuthentication()
    {

    }

    /**
     * ActionSheet interfaces
     */

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthorisation(wallet, getActivity(), callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        final SendTransactionInterface callback = new SendTransactionInterface()
        {
            @Override
            public void transactionSuccess(Web3Transaction web3Tx, String hashData)
            {
                confirmationDialog.transactionWritten(hashData);
                web3.onSignTransactionSuccessful(web3Tx, hashData);
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                confirmationDialog.dismiss();
                txError(error);
                web3.onSignCancel(callbackId);
            }
        };

        viewModel.sendTransaction(finalTx, activeNetwork.chainId, callback);
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            //actionsheet dismissed before completing signing.
            web3.onSignCancel(callbackId);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        if (getActivity() != null) ((HomeActivity)getActivity()).useActionSheet(mode);
    }

    // Handle resizing the browser view when the soft keyboard pops up and goes.
    // The issue this fixes is where you need to enter data at the bottom of the webpage,
    // and the keyboard hides the input field
    // Need to handle the inverse event where the keyboard is hidden, and we size the page back
    // (Remembering to allow for the navigation bar).
    private final View.OnApplyWindowInsetsListener resizeListener = (v, insets) -> {
        if (v == null || getActivity() == null) { return insets; }

        Rect r = new Rect();
        v.getWindowVisibleDisplayFrame(r);

        int heightDifference = v.getRootView().getHeight() - (r.bottom - r.top);
        int navBarHeight = ((HomeActivity) getActivity()).getNavBarHeight();

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();

        // check if we need to resize the webview. If we don't do this, the keyboard covers the bottom of the site
        // and might be obscuring elements the user needs to see while typing
        if (heightDifference > 0 && webFrame != null && layoutParams.bottomMargin != heightDifference)
        {
            //go into 'shrink' mode so no web site data is hidden
            layoutParams.bottomMargin = heightDifference;
            webFrame.setLayoutParams(layoutParams);
        }
        else if (heightDifference == 0 && layoutParams.bottomMargin != navBarHeight)
        {
            //go back into full screen mode, and expand URL bar out
            layoutParams.bottomMargin = navBarHeight;
            webFrame.setLayoutParams(layoutParams);
            shrinkSearchBar();
        }

        return insets;
    };

    // Required for if we have status bar showing
    public void softKeyboardVisible()
    {
        if (getActivity() == null) { return; }

        //boolean requiresDappBrowserResize = !viewModel.fullScreenSelected();

        Rect r = new Rect();
        webFrame.getWindowVisibleDisplayFrame(r);

        int heightDifference = webFrame.getRootView().getHeight() - r.bottom;

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();

        layoutParams.bottomMargin = heightDifference;
        webFrame.setLayoutParams(layoutParams);
    }

    public void softKeyboardGone()
    {
        if (getActivity() == null) { return; }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) webFrame.getLayoutParams();
        layoutParams.bottomMargin = ((HomeActivity) getActivity()).getNavBarHeight();
        webFrame.setLayoutParams(layoutParams);
        shrinkSearchBar();
    }

    @NotNull
    private String determineMimeType(@NotNull WebChromeClient.FileChooserParams fileChooserParams)
    {
        if (fileChooserParams == null || fileChooserParams.getAcceptTypes().length == 0) return "*/*"; // Allow anything
        String mime;
        String firstType = fileChooserParams.getAcceptTypes()[0];

        if (fileChooserParams.getAcceptTypes().length == 1)
        {
            mime = firstType;
        }
        else
        {
            //TODO: Resolve types
            switch (firstType)
            {
                case "png":
                case "gif":
                case "svg":
                case "jpg":
                case "jpeg":
                case "image/*":
                    mime = "image/*";
                    break;

                case "mp4":
                case "x-msvideo":
                case "x-ms-wmv":
                case "mpeg4-generic":
                case "video/*":
                    mime = "video/*";
                    break;

                case "mpeg":
                case "aac":
                case "wav":
                case "ogg":
                case "midi":
                case "x-ms-wma":
                case "audio/*":
                    mime = "audio/*";
                    break;

                case "pdf":
                    mime = "application/*";
                    break;

                case "xml":
                case "csv":
                    mime = "text/*";
                    break;

                default:
                    mime = "*/*";
            }
        }

        return mime;
    }

    private String getDefaultDappUrl()
    {
        String customHome = viewModel.getHomePage(getContext());
        return customHome != null ? customHome : EthereumNetworkRepository.defaultDapp(activeNetwork != null ? activeNetwork.chainId : 0);
    }

    @Override
    public void onTestNetDialogClosed()
    {
        //don't change to the new network

    }

    @Override
    public void onTestNetDialogConfirmed(long newChainId)
    {
        viewModel.setMainNetsSelected(false);
        //proceed with new network change, no need to pop a second dialog, we are swapping from a main net to a testnet
        NetworkInfo newNetwork = viewModel.getNetworkInfo(newChainId);
        if (newNetwork != null)
        {
            loadNewNetwork(newChainId);
        }
    }
}
