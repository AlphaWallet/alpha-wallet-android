package com.alphawallet.app.ui;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.SignTransactionInterface;
import com.alphawallet.app.entity.URLLoadInterface;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.OnDappHomeNavClickListener;
import com.alphawallet.app.ui.widget.OnHistoryItemRemovedListener;
import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeInterface;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeLayout;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.DappBrowserViewModel;
import com.alphawallet.app.viewmodel.DappBrowserViewModelFactory;
import com.alphawallet.app.web3.OnSignMessageListener;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.OnSignTransactionListener;
import com.alphawallet.app.web3.OnSignTypedMessageListener;
import com.alphawallet.app.web3.Web3View;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.TypedData;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignMessageDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;
import com.google.gson.Gson;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static com.alphawallet.app.entity.tokens.Token.TOKEN_BALANCE_PRECISION;
import static com.alphawallet.app.ui.MyAddressActivity.KEY_ADDRESS;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;

public class DappBrowserFragment extends Fragment implements OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener,
        URLLoadInterface, ItemClickListener, SignTransactionInterface, OnDappClickListener, OnDappHomeNavClickListener, OnHistoryItemRemovedListener, DappBrowserSwipeInterface, SignAuthenticationCallback
{
    private static final String TAG = DappBrowserFragment.class.getSimpleName();
    private static final String DAPP_BROWSER = "DAPP_BROWSER";
    private static final String DAPP_HOME = "DAPP_HOME";
    private static final String MY_DAPPS = "MY_DAPPS";
    private static final String DISCOVER_DAPPS = "DISCOVER_DAPPS";
    private static final String HISTORY = "HISTORY";
    public static final String SEARCH = "SEARCH";
    public static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String CURRENT_FRAGMENT = "currentFragment";
    private static final String CURRENT_URL = "urlInBar";
    private ValueCallback<Uri[]> uploadMessage;
    private WebChromeClient.FileChooserParams fileChooserParams;
    private Intent picker;
    private final Deque<String> forwardFragmentStack = new LinkedList<>();
    private final Deque<String> backFragmentStack = new LinkedList<>();
    private RealmResults<RealmToken> realmUpdate;

    private final String BROWSER_HOME = EthereumNetworkRepository.defaultDapp() != null
                                        ? DAPP_BROWSER : DAPP_HOME;

    private static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    private static final int UPLOAD_FILE = 1;
    public static final int REQUEST_FILE_ACCESS = 31;
    public static final int REQUEST_FINE_LOCATION = 110;

    static byte[] getEthereumMessagePrefix(int messageLength) {
        return MESSAGE_PREFIX.concat(String.valueOf(messageLength)).getBytes();
    }

    @Inject
    DappBrowserViewModelFactory dappBrowserViewModelFactory;
    private DappBrowserViewModel viewModel;

    private DappBrowserSwipeLayout swipeRefreshLayout;
    private Web3View web3;
    private AutoCompleteTextView urlTv;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo networkInfo;
    private SignMessageDialog dialog;
    private AWalletAlertDialog resultDialog;
    private DappBrowserSuggestionsAdapter adapter;
    private String loadOnInit;
    private boolean homePressed;

    private final Fragment dappHomeFragment;
    private final Fragment myDappsFragment;
    private final Fragment discoverDappsFragment;
    private final Fragment browserHistoryFragment;

    private Toolbar toolbar;
    private ImageView back;
    private ImageView next;
    private ImageView clear;
    private ImageView refresh;
    private TextView currentNetwork;
    private ImageView currentNetworkCircle;
    private LinearLayout currentNetworkClicker;
    private TextView balance;
    private TextView symbol;
    private GeolocationPermissions.Callback geoCallback = null;
    private String geoOrigin;
    private final Handler handler;

    private String currentWebpageTitle;
    private String currentFragment;

    private PinAuthenticationCallbackInterface authInterface;
    private Message<String> messageToSign;
    private byte[] messageBytes;
    private DAppFunction dAppFunction;
    private SignType signType;
    private volatile boolean canSign = true;

    private enum SignType
    {
        SIGN_PERSONAL_MESSAGE, SIGN_MESSAGE
    }

    public DappBrowserFragment()
    {
        dappHomeFragment = new DappHomeFragment();
        myDappsFragment = new MyDappsFragment();
        discoverDappsFragment = new DiscoverDappsFragment();
        browserHistoryFragment = new BrowserHistoryFragment();
        handler = new Handler();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        homePressed = false;
        if (currentFragment == null) currentFragment = BROWSER_HOME;
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
        AndroidSupportInjection.inject(this);
        int webViewID = VisibilityFilter.minimiseBrowserURLBar() ? R.layout.fragment_webview_compact : R.layout.fragment_webview;
        View view = inflater.inflate(webViewID, container, false);
        initViewModel();
        initView(view);
        setupAddressBar();
        viewModel.prepare(getContext());
        loadOnInit = null;

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            String url = getArguments().getString("url");
            loadOnInit = url;
        } else {
            String initFragment = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CURRENT_FRAGMENT, "");
            String lastUrl = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CURRENT_URL, "");
            if (savedInstanceState != null)
            {
                initFragment = savedInstanceState.getString(CURRENT_FRAGMENT, "");
                lastUrl = savedInstanceState.getString(CURRENT_URL, "");
            }

            //Dapp Browser init priority order:
            //1. Default DAPP: Load on startup or load last Dapp
            //2. Last fragment DAPP_BROWSER + browsing URL: load Dapp + init HOME fragment backstack
            //3. Last fragment not DAPP_BROWSER, load last fragment
            //4. No previous activity: Load DAPP_HOME.

            if (EthereumNetworkRepository.defaultDapp() != null)
            {
                attachFragment(DAPP_BROWSER);
                if (!lastUrl.isEmpty()) loadOnInit = lastUrl;
                else loadOnInit = EthereumNetworkRepository.defaultDapp();                          //1. Load last used dapp or default dapp
            }
            else if (!initFragment.isEmpty())
            {
                if (initFragment.equals(DAPP_BROWSER) && !lastUrl.isEmpty()) loadOnInit = lastUrl;  //2. load last dapp
                else initFragment(initFragment);                                                    //3. load last fragment
            }
            else
            {
                attachFragment(DAPP_HOME);                                                          //4. default to DAPP_HOME
            }
        }

        return view;
    }

    private void initFragment(String startingFragment)
    {
        if (!startingFragment.isEmpty())
        {
            addToBackStack(DAPP_HOME);
            attachFragment(startingFragment);
        }
        else
        {
            attachFragment(DAPP_HOME);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (getContext() != null && fragment.getTag() != null)
        {
            switch (fragment.getTag())
            {
                case DAPP_HOME:
                    ((DappHomeFragment) fragment).setCallbacks(this, this);
                    break;
                case DISCOVER_DAPPS:
                    ((DiscoverDappsFragment) fragment).setCallbacks(this);
                    break;
                case MY_DAPPS:
                    ((MyDappsFragment) fragment).setCallbacks(this);
                    break;
                case HISTORY:
                    ((BrowserHistoryFragment) fragment).setCallbacks(this, this);
                    break;
                case DAPP_BROWSER:
                    break;
                default:
                    //no init
                    break;
            }
        }
    }

    private void attachFragment(Fragment fragment, String tag) {
        if (tag != null && getHost() != null && getChildFragmentManager().findFragmentByTag(tag) == null)
        {
            showFragment(fragment, tag);
        }
    }

    private void attachFragment(String tag) {
        if (tag != null && getHost() != null && getChildFragmentManager().findFragmentByTag(tag) == null)
        {
            Fragment f = null;
            switch (tag)
            {
                case DAPP_HOME:
                    f = dappHomeFragment;
                    break;
                case DISCOVER_DAPPS:
                    f = discoverDappsFragment;
                    break;
                case MY_DAPPS:
                    f = myDappsFragment;
                    break;
                case HISTORY:
                    f = browserHistoryFragment;
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

    private void detachFragments(boolean detachHome) {
        if (detachHome) {
            detachFragment(DAPP_HOME);
        }
        detachFragment(MY_DAPPS);
        detachFragment(DISCOVER_DAPPS);
        detachFragment(HISTORY);
        detachFragment(SEARCH);
    }

    public void homePressed()
    {
        homePressed = true;
        detachFragments(false);
        forwardFragmentStack.clear();
        backFragmentStack.clear();
        currentFragment = BROWSER_HOME;
        if (!BROWSER_HOME.equals(DAPP_BROWSER)) attachFragment(dappHomeFragment, BROWSER_HOME);
        if (urlTv != null)
            urlTv.getText().clear();
        if (web3 != null)
        {
            web3.clearHistory();
            web3.stopLoading();

            if (EthereumNetworkRepository.defaultDapp() != null)
            {
                loadUrl(EthereumNetworkRepository.defaultDapp());
            }
        }

        //blank forward / backward arrows
        setBackForwardButtons();
    }

    @Override
    public void onDappHomeNavClick(int position) {
        detachFragments(true);
        switch (position) {
            case 0: {
                forwardFragmentStack.clear();
                addToBackStack(MY_DAPPS);
                attachFragment(myDappsFragment, MY_DAPPS);
                break;
            }
            case 1: {
                forwardFragmentStack.clear();
                addToBackStack(DISCOVER_DAPPS);
                attachFragment(discoverDappsFragment, DISCOVER_DAPPS);
                break;
            }
            case 2: {
                forwardFragmentStack.clear();
                addToBackStack(HISTORY);
                attachFragment(browserHistoryFragment, HISTORY);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onDappClick(DApp dapp) {
        forwardFragmentStack.clear();
        addToBackStack(DAPP_BROWSER);
        loadUrl(dapp.getUrl());
    }

    @Override
    public void onHistoryItemRemoved(DApp dApp) {
        adapter.removeSuggestion(dApp);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onDestroy();
        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();
    }

    private void setupMenu(View baseView)
    {
        refresh = baseView.findViewById(R.id.refresh);
        final MenuItem reload = toolbar.getMenu().findItem(R.id.action_reload);
        final MenuItem share = toolbar.getMenu().findItem(R.id.action_share);
        final MenuItem scan = toolbar.getMenu().findItem(R.id.action_scan);
        final MenuItem add = toolbar.getMenu().findItem(R.id.action_add_to_my_dapps);

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
    }

    private void initView(View view) {
        web3 = view.findViewById(R.id.web3view);
        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setRefreshInterface(this);
        toolbar = view.findViewById(R.id.address_bar);
        if (VisibilityFilter.minimiseBrowserURLBar())
        {
            toolbar.inflateMenu(R.menu.menu_scan);
        }
        else if (EthereumNetworkRepository.defaultDapp() != null)
        {
            toolbar.inflateMenu(R.menu.menu_defaultdapp);
        }
        else
        {
            toolbar.inflateMenu(R.menu.menu_bookmarks);
        }
        refresh = view.findViewById(R.id.refresh);
        setupMenu(view);

        RelativeLayout layout = view.findViewById(R.id.address_bar_layout);
        layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        refresh.setOnClickListener(v -> reloadPage());

        back = view.findViewById(R.id.back);
        back.setOnClickListener(v -> goToPreviousPage());

        next = view.findViewById(R.id.next);
        next.setOnClickListener(v -> goToNextPage());

        clear = view.findViewById(R.id.clear_url);
        clear.setOnClickListener(v -> {
            clearAddressBar();
        });

        currentNetworkClicker = view.findViewById(R.id.network_holder);
        currentNetworkClicker.setOnClickListener(v -> selectNetwork());
        currentNetwork = view.findViewById(R.id.network_text);
        currentNetworkCircle = view.findViewById(R.id.network_colour);
        balance = view.findViewById(R.id.balance);
        symbol = view.findViewById(R.id.symbol);
        web3.setWebLoadCallback(this);

        if (viewModel.getActiveFilterCount() == 1 && EthereumNetworkRepository.defaultDapp() != null) currentNetworkClicker.setVisibility(View.GONE);
    }

    private void displayNothingToShare()
    {
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

    private void selectNetwork() {
        Intent intent = new Intent(getContext(), SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, String.valueOf(networkInfo.chainId));
        if (getActivity() != null) getActivity().startActivityForResult(intent, C.REQUEST_SELECT_NETWORK);
    }

    private void clearAddressBar() {
        if (urlTv.getText().toString().isEmpty()) {
            cancelSearchSession();
        } else {
            urlTv.getText().clear();
        }
    }

    private void setupAddressBar() {
        adapter = new DappBrowserSuggestionsAdapter(
                getContext(),
                viewModel.getDappsMasterList(getContext()),
                this::onItemClick
        );
        urlTv.setAdapter(adapter);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO)
            {
                String urlText = urlTv.getText().toString();
                forwardFragmentStack.clear();
                handled = loadUrl(urlText);
                detachFragments(true);
                cancelSearchSession();
            }
            return handled;
        });

        urlTv.setOnClickListener(v -> {
            beginSearchSession();
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

    private void beginSearchSession() {
        SearchFragment f = new SearchFragment();
        f.setCallbacks(view -> {
            cancelSearchSession();
        });
        attachFragment(f, SEARCH);
        currentNetwork.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        back.setVisibility(View.GONE);
        clear.setVisibility(View.VISIBLE);
        urlTv.showDropDown();
    }

    private void addToBackStack(String nextFragment)
    {
        if (currentFragment != null && !currentFragment.equals(nextFragment)) backFragmentStack.add(currentFragment);
        currentFragment = nextFragment;
    }

    private void addToForwardStack(String prevFragment)
    {
        if (currentFragment != null && !currentFragment.equals(prevFragment)) forwardFragmentStack.add(currentFragment);
        currentFragment = prevFragment;
    }

    private void cancelSearchSession() {
        detachFragment(SEARCH);
        if (toolbar != null)
        {
            toolbar.getMenu().setGroupVisible(R.id.dapp_browser_menu, true);
            currentNetwork.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
            back.setVisibility(View.VISIBLE);
            clear.setVisibility(View.GONE);
            urlTv.dismissDropDown();
        }
        KeyboardUtils.hideKeyboard(urlTv);
        setBackForwardButtons();
    }

    private void detachFragment(String tag) {
        if (!isAdded()) return; //the dappBrowserFragment itself may not yet be attached.
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment.isVisible() && !fragment.isDetached()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, dappBrowserViewModelFactory)
                .get(DappBrowserViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void startBalanceListener()
    {
        if (wallet == null || networkInfo == null) return;

        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();
        realmUpdate = viewModel.getRealmInstance(wallet).where(RealmToken.class)
                .equalTo("address", TokensRealmSource.databaseKey(networkInfo.chainId, "eth"))
                .equalTo("chainId", networkInfo.chainId).findAllAsync();
        realmUpdate.addChangeListener(realmTokens -> {
            //update balance
            if (realmTokens.size() == 0) return;
            RealmToken realmToken = realmTokens.first();
            balance.setVisibility(View.VISIBLE);
            symbol.setVisibility(View.VISIBLE);
            String newBalanceStr = BalanceUtils.getScaledValueFixed(new BigDecimal(realmToken.getBalance()), ETHER_DECIMALS, TOKEN_BALANCE_PRECISION);
            balance.setText(newBalanceStr);
            symbol.setText(networkInfo.getShortName());
        });
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        setupWeb3();
        startBalanceListener();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        int oldChain = this.networkInfo != null ? this.networkInfo.chainId : -1;
        this.networkInfo = networkInfo;
        currentNetwork.setText(networkInfo.getShortName());
        startBalanceListener();
        //select resource
        Utils.setChainCircle(currentNetworkCircle, networkInfo.chainId);
        //reset the pane if required
        if (oldChain > 0 && oldChain != this.networkInfo.chainId)
        {
            web3.reload();
        }
    }

    private void setupWeb3() {
        web3.setActivity(getActivity());
        web3.setChainId(networkInfo.chainId);
        web3.setRpcUrl(EthereumNetworkBase.getDefaultNodeURL(networkInfo.chainId));
        web3.setWalletAddress(new Address(wallet.address));

        web3.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView webview, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    refresh.setEnabled(true);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                    swipeRefreshLayout.setRefreshing(true);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                currentWebpageTitle = title;
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
                picker = fileChooserParams.createIntent();

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
                        default:
                            break;
                    }
                }

                urlTv.setText(url);
                return false;
            }
        });

        web3.setOnSignMessageListener(this);
        web3.setOnSignPersonalMessageListener(this);
        web3.setOnSignTransactionListener(this);
        web3.setOnSignTypedMessageListener(this);

        if (loadOnInit != null)
        {
            addToBackStack(BROWSER_HOME);
            loadUrl(loadOnInit);
            loadOnInit = null;
        }
    }

    protected boolean requestUpload()
    {
        try
        {
            startActivityForResult(picker, UPLOAD_FILE);
        }
        catch (ActivityNotFoundException e)
        {
            uploadMessage = null;
            Toast.makeText(getActivity().getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @Override
    public void onSignMessage(Message<String> message) {
        messageToSign = message;
        dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Message<String> message) {
                web3.onSignCancel(message);
                dialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Message<String> message) {
                String signHex = Numeric.toHexString(data);
                Log.d(TAG, "Initial Msg: " + message.value);
                web3.onSignMessageSuccessful(message, signHex);
                dialog.dismiss();
            }
        };

        try
        {
            dialog = new SignMessageDialog(getActivity(), message);
            dialog.setAddress(wallet.address);
            dialog.setOnApproveListener(v -> {
                //ensure we generate the signature correctly:
                if (message.value != null)
                {
                    messageBytes = message.value.getBytes();
                    if (message.value.substring(0, 2).equals("0x"))
                    {
                        messageBytes = Numeric.hexStringToByteArray(message.value);
                    }
                    viewModel.getAuthorisation(wallet, getActivity(), this);
                }
                else
                {
                    onSignError();
                }
            });
            dialog.setOnRejectListener(v -> {
                if (web3 != null) web3.onSignCancel(message);
                dialog.dismiss();
            });
            dialog.show();
        }
        catch (Exception e)
        {
            onSignError(e.getMessage());
        }
    }

    @Override
    public void onSignPersonalMessage(Message<String> message) {
        messageToSign = message;
        dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Message<String> message) {
                web3.onSignCancel(message);
                dialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Message<String> message) {
                String signHex = Numeric.toHexString(data);
                Log.d(TAG, "Initial Msg: " + message.value);
                web3.onSignPersonalMessageSuccessful(message, signHex);
                //Test Sig in debug build
                if (BuildConfig.DEBUG) testRecoverAddressFromSignature(Hex.hexToUtf8(message.value), signHex);
                dialog.dismiss();
            }
        };

        try
        {
            dialog = new SignMessageDialog(getActivity(), message);
            dialog.setAddress(wallet.address);
            String signString = Hex.hexToUtf8(message.value);
            //Analyse if this is an ISO-8859-1 string, otherwise show the hex
            if (!Charset.forName("ISO-8859-1").newEncoder().canEncode(signString)) signString = message.value;
            dialog.setMessage(signString);
            dialog.setOnApproveListener(v -> {
                messageBytes = getEthereumMessage(Numeric.hexStringToByteArray(message.value));
                viewModel.getAuthorisation(wallet, getActivity(), this);
            });
            dialog.setOnRejectListener(v -> {
                web3.onSignCancel(message);
                dialog.dismiss();
            });
            dialog.show();
        }
        catch (Exception e)
        {
            // this will be mainly for developers, so no need to tidy the exception
            // if a user comes across this message they can report to the dapp writer
            onSignError(e.getMessage());
        }
    }

    static byte[] getEthereumMessage(byte[] message) {
        byte[] prefix = getEthereumMessagePrefix(message.length);

        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);

        return result;
    }

    @Override
    public void onSignTypedMessage(Message<TypedData[]> message) {
        //TODO
        Toast.makeText(getActivity(), new Gson().toJson(message), Toast.LENGTH_LONG).show();
        web3.onSignCancel(message);
    }

    @Override
    public void onSignTransaction(Web3Transaction transaction, String url)
    {
        try
        {
            viewModel.updateGasPrice(networkInfo.chainId); //start updating gas price right before we open
            //minimum for transaction to be valid: recipient and value or payload
            if ((transaction.recipient.equals(Address.EMPTY) && transaction.payload != null) // Constructor
                    || (!transaction.recipient.equals(Address.EMPTY) && (transaction.payload != null || transaction.value != null))) // Raw or Function TX
            {
                if (canSign)
                {
                    viewModel.openConfirmation(getActivity(), transaction, url, networkInfo);
                    canSign = false;
                    handler.postDelayed(() -> canSign = true, 3000); //debounce 3 seconds to avoid multiple signing issues
                }
            }
            else
            {
                //display transaction error
                onInvalidTransaction(transaction);
                web3.onSignCancel(transaction);
            }
        }
        catch (android.os.TransactionTooLargeException e)
        {
            transactionTooLarge();
            web3.onSignCancel(transaction);
        }
        catch (Exception e)
        {
            onInvalidTransaction(transaction);
            web3.onSignCancel(transaction);
        }
    }

    //return from the openConfirmation above
    public void handleTransactionCallback(int resultCode, Intent data)
    {
        if (data == null || web3 == null) return;
        Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
        if (resultCode == RESULT_OK && web3Tx != null)
        {
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            web3.onSignTransactionSuccessful(web3Tx, hashData);
        }
        else if (web3Tx != null)
        {
            web3.onSignCancel(web3Tx);
        }
    }
    
    private void onSignError()
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.dialog_title_sign_message));
        resultDialog.setMessage(getString(R.string.contains_no_data));
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    private void onSignError(String message)
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.dialog_title_sign_message));
        resultDialog.setMessage(message);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

    private void transactionTooLarge()
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.transaction_too_large));
        resultDialog.setMessage(getString(R.string.unable_to_handle_tx));

        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
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

    private void goToPreviousPage() {
        if (web3.canGoBack()) {
            checkBackClickArrowVisibility(); //to make arrows function correctly - don't want to wait for web page to load to check back/forwards - this looks clunky
            web3.goBack();
            detachFragments(true);
            loadSessionUrl(-1);
        }
        else if (backFragmentStack.peekLast() != null)
        {
            String lastPage = backFragmentStack.pollLast();
            if (!lastPage.equals(currentFragment))
            {
                addToForwardStack(lastPage);
                detachFragments(true);
                attachFragment(lastPage);
            }
            setBackForwardButtons();
        }
    }

    private void goToNextPage() {
        if (currentFragment.equals(DAPP_BROWSER) && web3.canGoForward())
        {
            checkForwardClickArrowVisibility();
            web3.goForward();
            loadSessionUrl(1);
        }
        else if (forwardFragmentStack.peekLast() != null)
        {
            String nextPage = forwardFragmentStack.pollLast();

            if (!nextPage.equals(currentFragment))
            {
                addToBackStack(nextPage);
                detachFragments(true);
                attachFragment(nextPage);
            }

            setBackForwardButtons();
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
        if (backFragmentStack.peekLast() == null && nextIndex <= 0) back.setAlpha(0.3f);
        else back.setAlpha(1.0f);

        next.setAlpha(1.0f); //if we clicked back then we would have a next available
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

        back.setAlpha(1.0f);
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
                urlTv.setText(newItem.getUrl());
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
            if (BROWSER_HOME.equals(DAPP_BROWSER) && url.equals(EthereumNetworkRepository.defaultDapp()))
            {
                web3.clearHistory();
            }
        }
        DApp dapp = new DApp(title, url);
        DappBrowserUtils.addToHistory(getContext(), dapp);
        adapter.addSuggestion(dapp);
        onWebpageLoadComplete();
    }

    @Override
    public void onWebpageLoadComplete()
    {
        handler.post(this::setBackForwardButtons); //execute on UI thread
    }

    private void setBackForwardButtons()
    {
        WebBackForwardList sessionHistory = null;
        if (web3 != null) sessionHistory = web3.copyBackForwardList();

        String nextFrag = forwardFragmentStack.peekLast();
        String backFrag = backFragmentStack.peekLast();

        if (back != null)
        {
            if (backFrag != null || (currentFragment.equals(DAPP_BROWSER) && (web3 != null && web3.canGoBack())))
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
            if (nextFrag != null || (currentFragment.equals(DAPP_BROWSER) && (sessionHistory != null && sessionHistory.getCurrentIndex() < sessionHistory.getSize() - 1)))
            {
                next.setAlpha(1.0f);
            }
            else
            {
                next.setAlpha(0.3f);
            }
        }
    }

    private boolean loadUrl(String urlText)
    {
        detachFragments(true);
        addToBackStack(DAPP_BROWSER);
        cancelSearchSession();
        if (checkForMagicLink(urlText)) return true;
        web3.loadUrl(Utils.formatUrl(urlText), getWeb3Headers());
        urlTv.setText(Utils.formatUrl(urlText));
        web3.requestFocus();
        viewModel.setLastUrl(getContext(), urlText);
        Activity current = getActivity();
        if (current != null)
        {
            current.sendBroadcast(new Intent(RESET_TOOLBAR));
        }
        return true;
    }

    /* Required for CORS requests */
    private Map<String, String> getWeb3Headers()
    {
        //headers
        return new HashMap<String, String>() {{
            put("Connection", "close");
            put("Content-Type", "text/plain");
            put("Access-Control-Allow-Origin", "*");
            put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            put("Access-Control-Max-Age", "600");
            put("Access-Control-Allow-Credentials", "true");
            put("Access-Control-Allow-Headers", "accept, authorization, Content-Type");
        }};
    }

    public void reloadPage() {
        if (currentFragment.equals(DAPP_BROWSER))
        {
            refresh.setEnabled(false);
            web3.reload();
        }
    }

    @Override
    public void onItemClick(String url)
    {
        forwardFragmentStack.clear();
        addToBackStack(DAPP_BROWSER);
        loadUrl(url);
    }

    public void testRecoverAddressFromSignature(String message, String sig)
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
            System.out.println("Recovered: " + addressRecovered);
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void signTransaction(Web3Transaction transaction, String txHex, boolean success)
    {
        if (success)
        {
            web3.onSignTransactionSuccessful(transaction, txHex);
        }
        else
        {
            web3.onSignCancel(transaction);
        }
    }

    public void handleSelectNetwork(int resultCode, Intent data) {
        if (getActivity() == null) return;
        if (resultCode == RESULT_OK) {
            int networkId = data.getIntExtra(C.EXTRA_CHAIN_ID, 1); //default to mainnet in case of trouble
            if (networkInfo.chainId != networkId) {
                viewModel.setNetwork(networkId);
                if (getActivity() != null) getActivity().sendBroadcast(new Intent(RESET_WALLET));
                balance.setVisibility(View.GONE);
                symbol.setVisibility(View.GONE);
            }
        }
    }

    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger)
    {
        //result
        String qrCode = null;
        try
        {
            switch (resultCode)
            {
                case FullScannerFragment.SUCCESS:
                    if (data != null)
                    {
                        qrCode = data.getStringExtra(FullScannerFragment.BarcodeObject);
                        if (qrCode == null || checkForMagicLink(qrCode)) return;
                        QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
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
                case QRScanningActivity.DENY_PERMISSION:
                    showCameraDenied();
                    break;
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
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    private boolean checkReadPermission()
    {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            getActivity().requestPermissions(permissions, REQUEST_FILE_ACCESS);
            return false;
        }
    }

    // Handles the requesting of the fine location permission.
    // Note: If you intend allowing geo-location in your app you need to ask the permission.
    private void requestGeoPermission(String origin, GeolocationPermissions.Callback callback)
    {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            geoCallback = callback;
            geoOrigin = origin;
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            getActivity().requestPermissions(permissions, REQUEST_FINE_LOCATION);
        }
        else
        {
            callback.invoke(origin, true, false);
        }
    }

    public void gotGeoAccess(String[] permissions, int[] grantResults)
    {
        boolean geoAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] != -1) geoAccess = true;
        }
        if (!geoAccess) Toast.makeText(getContext(), "Permission not given", Toast.LENGTH_SHORT).show();
        if (geoCallback != null && geoOrigin != null) geoCallback.invoke(geoOrigin, geoAccess, false);
    }

    public void gotFileAccess(String[] permissions, int[] grantResults)
    {
        boolean fileAccess = false;
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] != -1) fileAccess = true;
        }

        if (fileAccess && picker != null) requestUpload();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_FRAGMENT, currentFragment);
        outState.putString(CURRENT_URL, urlTv.getText().toString());
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(CURRENT_FRAGMENT, currentFragment)
                .putString(CURRENT_URL, urlTv.getText().toString())
                .apply();
    }

    @Override
    public void RefreshEvent()
    {
        //determine scroll position
        Log.i("Touch", "SCROLL: " + web3.getScrollY());
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

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            gotAuthorisation(resultCode == RESULT_OK);
        }
        else if (requestCode == UPLOAD_FILE && uploadMessage != null)
        {
            if (resultCode == RESULT_OK)
            {
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            }
            uploadMessage = null;
        }
        else if (requestCode == REQUEST_FILE_ACCESS)
        {
            if (resultCode == RESULT_OK)
            {
                requestUpload();
            }
        }
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (gotAuth)
        {
            viewModel.completeAuthentication(SIGN_DATA);
            viewModel.signMessage(messageBytes, dAppFunction, messageToSign);
        }
        else if (dialog != null && dialog.isShowing())
        {
            web3.onSignCancel(messageToSign);
            dialog.dismiss();
        }
    }

    @Override
    public void cancelAuthentication()
    {

    }
}
