package com.alphawallet.app.ui;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.google.gson.Gson;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.OnDappHomeNavClickListener;
import com.alphawallet.app.ui.widget.OnHistoryItemRemovedListener;
import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeInterface;
import com.alphawallet.app.ui.widget.entity.DappBrowserSwipeLayout;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.OnSignMessageListener;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.OnSignTransactionListener;
import com.alphawallet.app.web3.OnSignTypedMessageListener;
import com.alphawallet.app.web3.Web3View;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.TypedData;
import com.alphawallet.app.web3.entity.Web3Transaction;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.SignTransactionInterface;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.URLLoadInterface;
import com.alphawallet.app.entity.URLLoadReceiver;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.DappBrowserViewModel;
import com.alphawallet.app.viewmodel.DappBrowserViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignMessageDialog;
import com.alphawallet.app.widget.SignTransactionDialog;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
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

    private static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

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
    private URLLoadReceiver URLReceiver;
    private String loadOnInit;

    private final Fragment dappHomeFragment;
    private final Fragment myDappsFragment;
    private final Fragment discoverDappsFragment;
    private final Fragment browserHistoryFragment;

    private Toolbar toolbar;
    private ImageView back;
    private ImageView next;
    private ImageView clear;
    private TextView currentNetwork;
    private ImageView currentNetworkCircle;
    private LinearLayout currentNetworkClicker;
    private TextView balance;
    private TextView symbol;

    private String currentWebpageTitle;
    private String currentFragment;

    private WebBackForwardList sessionHistory;
    private String lastHomeTag;
    private PinAuthenticationCallbackInterface authInterface;
    private Message<String> messageToSign;
    private byte[] messageBytes;
    private DAppFunction dAppFunction;
    private SignType signType;

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
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentFragment == null) currentFragment = DAPP_HOME;
        attachFragment(currentFragment);
        if (web3 == null && getActivity() != null) //trigger reload
        {
            ((HomeActivity)getActivity()).ResetDappBrowser();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        initView(view);
        initViewModel();
        setupAddressBar();
        viewModel.prepare(getContext());
        URLReceiver = new URLLoadReceiver(getActivity(), this);

        lastHomeTag = DAPP_HOME;
        loadOnInit = null;

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            String url = getArguments().getString("url");
            loadOnInit = url;
        } else {
            String prevFragment = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CURRENT_FRAGMENT, null);
            String prevUrl = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CURRENT_URL, null);
            if (savedInstanceState != null)
            {
                currentFragment = savedInstanceState.getString(CURRENT_FRAGMENT, "");
                String lastUrl = savedInstanceState.getString(CURRENT_URL, "");
                if (currentFragment.isEmpty())
                {
                    attachFragment(DAPP_HOME);
                }
                else
                {
                    attachFragment(currentFragment);
                }

                if (lastUrl.length() > 0)
                    loadOnInit = lastUrl;
            }
            else if (prevFragment != null)
            {
                attachFragment(prevFragment);
                if (prevUrl != null && prevFragment.equals(DAPP_BROWSER) && prevUrl.length() > 0) loadOnInit = prevUrl;
            } else {
                attachFragment(DAPP_HOME);
            }
        }

        return view;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment.getTag() != null)
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
                    lastHomeTag = DAPP_HOME;
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
            }

            if (f != null) showFragment(f, tag);
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        this.currentFragment = tag;
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
        detachFragments(false);
        attachFragment(dappHomeFragment, DAPP_HOME);
        web3.stopLoading();
        urlTv.getText().clear();
    }

    @Override
    public void onDappHomeNavClick(int position) {
        detachFragments(true);
        switch (position) {
            case 0: {
                attachFragment(myDappsFragment, MY_DAPPS);
                break;
            }
            case 1: {
                attachFragment(discoverDappsFragment, DISCOVER_DAPPS);
                break;
            }
            case 2: {
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
        loadUrl(dapp.getUrl());
    }

    @Override
    public void onHistoryItemRemoved(DApp dApp) {
        adapter.removeSuggestion(dApp);
    }

    @Override
    public void onDestroy()
    {
        if (getContext() != null) getContext().unregisterReceiver(URLReceiver);
        super.onDestroy();
    }

    private void initView(View view) {
        web3 = view.findViewById(R.id.web3view);
        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setRefreshInterface(this);
        toolbar = view.findViewById(R.id.address_bar);
        toolbar.inflateMenu(R.menu.menu_bookmarks);
        toolbar.getMenu().findItem(R.id.action_reload)
                .setOnMenuItemClickListener(menuItem -> {
                    reloadPage();
                    return true;
                });
        toolbar.getMenu().findItem(R.id.action_share)
                .setOnMenuItemClickListener(menuItem -> {
                    if (web3.getUrl() != null && currentFragment != null && currentFragment.equals(DAPP_BROWSER)) {
                        if (getContext() != null) viewModel.share(getContext(), web3.getUrl());
                    }
                    else
                    {
                        displayNothingToShare();
                    }
                    return true;
                });
        toolbar.getMenu().findItem(R.id.action_scan)
                .setOnMenuItemClickListener(menuItem -> {
                    viewModel.startScan(getActivity());
                    return true;
                });
        toolbar.getMenu().findItem(R.id.action_add_to_my_dapps)
                .setOnMenuItemClickListener(menuItem -> {
                    viewModel.addToMyDapps(getContext(), currentWebpageTitle, urlTv.getText().toString());
                    return true;
                });

        RelativeLayout layout = view.findViewById(R.id.address_bar_layout);
        layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

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
        toolbar.getMenu().setGroupVisible(R.id.dapp_browser_menu, false);
        currentNetwork.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        back.setVisibility(View.GONE);
        clear.setVisibility(View.VISIBLE);
        urlTv.showDropDown();
    }

    private void cancelSearchSession() {
        detachFragment(SEARCH);
        toolbar.getMenu().setGroupVisible(R.id.dapp_browser_menu, true);
        currentNetwork.setVisibility(View.VISIBLE);
        next.setVisibility(View.VISIBLE);
        back.setVisibility(View.VISIBLE);
        clear.setVisibility(View.GONE);
        urlTv.dismissDropDown();
        KeyboardUtils.hideKeyboard(urlTv);
        setBackForwardButtons();
    }

    private void detachFragment(String tag) {
        if (!isAdded()) return; //the dappBrowserFragment itself may not yet be attached.
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment.isVisible() && !fragment.isDetached()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, dappBrowserViewModelFactory)
                .get(DappBrowserViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.token().observe(this, this::onUpdateBalance);
    }

    private void onUpdateBalance(Token token) {
        balance.setVisibility(View.VISIBLE);
        symbol.setVisibility(View.VISIBLE);
        balance.setText(token.getScaledBalance());
        symbol.setText(token.tokenInfo.symbol);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        setupWeb3();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        int oldChain = this.networkInfo != null ? this.networkInfo.chainId : -1;
        this.networkInfo = networkInfo;
        currentNetwork.setText(networkInfo.getShortName());
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
        web3.setRpcUrl(networkInfo.rpcServerUrl);
        web3.setWalletAddress(new Address(wallet.address));

        web3.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView webview, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
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
        });

        web3.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
            loadUrl(loadOnInit);
            loadOnInit = null;
        }
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
                web3.onSignCancel(message);
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
        //minimum for transaction to be valid: recipient and value or payload
        if ((transaction.recipient.equals(Address.EMPTY) && transaction.payload != null) // Constructor
            || (!transaction.recipient.equals(Address.EMPTY) && (transaction.payload != null || transaction.value != null))) // Raw or Function TX
        {
            viewModel.openConfirmation(getActivity(), transaction, url, networkInfo);
        }
        else
        {
            //display transaction error
            onInvalidTransaction(transaction);
            web3.onSignCancel(transaction);
        }
    }

    //return from the openConfirmation above
    public void handleTransactionCallback(int resultCode, Intent data)
    {
        if (data == null) return;
        if (resultCode == RESULT_OK)
        {
            Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            web3.onSignTransactionSuccessful(web3Tx, hashData);
        }
        else
        {
            Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
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

    private void onInvalidTransaction(Web3Transaction transaction) {
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
            web3.goBack();
            detachFragments(true);
            loadSessionUrl(-1);
        }
        else
        {
            detachFragments(true);
            if (lastHomeTag != null && !lastHomeTag.equals(DAPP_HOME))
            {
                attachFragment(lastHomeTag);
                lastHomeTag = DAPP_HOME;
            }
            else
            {
                attachFragment(DAPP_HOME);
            }
        }
    }

    private void goToNextPage() {
        if (web3.canGoForward()) {
            web3.goForward();
            detachFragments(true);
            loadSessionUrl(1);
        }
    }

    /**
     * Browse to relative entry with sanity check on value
     * @param relative relative addition or subtraction of browsing index
     */
    private void loadSessionUrl(int relative)
    {
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
        DApp dapp = new DApp(title, url);
        DappBrowserUtils.addToHistory(getContext(), dapp);
        adapter.addSuggestion(dapp);
        sessionHistory = web3.copyBackForwardList();
        this.currentFragment = DAPP_BROWSER;
        setBackForwardButtons();
    }

    private void setBackForwardButtons() {
        if (sessionHistory == null) {
            sessionHistory = web3.copyBackForwardList();
        }

        if (currentFragment.equals(DAPP_HOME)) {
            back.setAlpha(0.3f);
        } else {
            back.setAlpha(1.0f);
        }

        if (sessionHistory.getCurrentIndex() < sessionHistory.getSize()-1) {
            next.setAlpha(1.0f);
        } else {
            next.setAlpha(0.3f);
        }
    }

    private boolean loadUrl(String urlText)
    {
        if (lastHomeTag == null) lastHomeTag = DAPP_BROWSER;
        else if (!lastHomeTag.equals(DAPP_BROWSER)) lastHomeTag = currentFragment;
        detachFragments(true);
        this.currentFragment = DAPP_BROWSER;
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
        web3.reload();
    }

    @Override
    public void onItemClick(String url)
    {
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
                        if (qrCode != null && !checkForMagicLink(qrCode))
                        {
                            if (Utils.isAddressValid(qrCode))
                            {
                                DisplayAddressFound(qrCode, messenger);
                            }
                            else
                            {
                                //attempt to go to site
                                loadUrl(qrCode);
                            }
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
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isResumed()) {
            if (isVisibleToUser)
            {
                viewModel.startGasPriceChecker();
            }
            else
            {
                viewModel.stopGasPriceChecker();
            }
        }
    }

    private void DisplayAddressFound(String address, FragmentMessenger messenger)
    {
        if (getActivity() == null) return;
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.address_found));
        resultDialog.setMessage(getString(R.string.is_address));
        resultDialog.setButtonText(R.string.dialog_load_as_contract);
        resultDialog.setButtonListener(v -> {
            messenger.AddToken(address);
            resultDialog.dismiss();
        });
        resultDialog.setSecondaryButtonText(R.string.action_cancel);
        resultDialog.setSecondaryButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
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

    public void onActivityResult(int requestCode, int resultCode)
    {
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            GotAuthorisation(resultCode == RESULT_OK);
        }
    }

    @Override
    public void GotAuthorisation(boolean gotAuth)
    {
        if (gotAuth && authInterface != null) authInterface.CompleteAuthentication(SIGN_DATA);
        else if (!gotAuth && authInterface != null) authInterface.FailedAuthentication(SIGN_DATA);

        if (gotAuth)
        {
            viewModel.signMessage(messageBytes, dAppFunction, messageToSign);
        }
        else if (dialog != null && dialog.isShowing())
        {
            web3.onSignCancel(messageToSign);
            dialog.dismiss();
        }
    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
    }
}
