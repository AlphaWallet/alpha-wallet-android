package io.stormbird.wallet.ui;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.entity.*;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SignatureException;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnDappClickListener;
import io.stormbird.wallet.ui.widget.OnDappHomeNavClickListener;
import io.stormbird.wallet.ui.widget.OnHistoryItemRemovedListener;
import io.stormbird.wallet.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import io.stormbird.wallet.ui.widget.entity.ItemClickListener;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.util.DappBrowserUtils;
import io.stormbird.wallet.util.Hex;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.DappBrowserViewModel;
import io.stormbird.wallet.viewmodel.DappBrowserViewModelFactory;
import io.stormbird.wallet.web3.OnSignMessageListener;
import io.stormbird.wallet.web3.OnSignPersonalMessageListener;
import io.stormbird.wallet.web3.OnSignTransactionListener;
import io.stormbird.wallet.web3.OnSignTypedMessageListener;
import io.stormbird.wallet.web3.Web3View;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.TypedData;
import io.stormbird.wallet.web3.entity.Web3Transaction;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SelectNetworkDialog;
import io.stormbird.wallet.widget.SignMessageDialog;

import static io.stormbird.wallet.C.RESET_TOOLBAR;
import static io.stormbird.wallet.C.RESET_WALLET;
import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;

public class DappBrowserFragment extends Fragment implements
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener,
        URLLoadInterface, ItemClickListener, SignTransactionInterface, OnDappClickListener, OnDappHomeNavClickListener,
        OnHistoryItemRemovedListener
{
    private static final String TAG = DappBrowserFragment.class.getSimpleName();
    private static final String DAPP_BROWSER = "DAPP_BROWSER";
    private static final String DAPP_HOME = "DAPP_HOME";
    private static final String MY_DAPPS = "MY_DAPPS";
    private static final String DISCOVER_DAPPS = "DISCOVER_DAPPS";
    private static final String HISTORY = "HISTORY";
    public static final String SEARCH = "SEARCH";
    private static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    public static final String CURRENT_FRAGMENT = "currentFragment";

    @Inject
    DappBrowserViewModelFactory dappBrowserViewModelFactory;
    private DappBrowserViewModel viewModel;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Web3View web3;
    private AutoCompleteTextView urlTv;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo networkInfo;
    private SignMessageDialog dialog;
    private AWalletAlertDialog resultDialog;
    private DappBrowserSuggestionsAdapter adapter;
    private URLLoadReceiver URLReceiver;

    private Fragment dappHomeFragment;
    private Fragment myDappsFragment;
    private Fragment discoverDappsFragment;
    private Fragment browserHistoryFragment;

    private Toolbar toolbar;
    private ImageView back;
    private ImageView next;
    private ImageView clear;
    private TextView currentNetwork;
    private TextView balance;
    private TextView symbol;

    private String currentWebpageTitle;
    private String currentFragment;

    private WebBackForwardList sessionHistory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dappHomeFragment = new DappHomeFragment();
        myDappsFragment = new MyDappsFragment();
        discoverDappsFragment = new DiscoverDappsFragment();
        browserHistoryFragment = new BrowserHistoryFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        attachFragment(currentFragment);
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

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            String url = getArguments().getString("url");
            loadUrl(url);
        } else {
            if (savedInstanceState != null) {
                currentFragment = savedInstanceState.getString(CURRENT_FRAGMENT, "");
                if (currentFragment.isEmpty()) {
                    attachFragment(DAPP_HOME);
                } else {
                    attachFragment(currentFragment);
                }
            } else {
                attachFragment(DAPP_HOME);
            }
        }

        return view;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof DappHomeFragment) {
            DappHomeFragment f = (DappHomeFragment) fragment;
            f.setCallbacks(this, this);
        }
        if (fragment instanceof DiscoverDappsFragment) {
            DiscoverDappsFragment f = (DiscoverDappsFragment) fragment;
            f.setCallbacks(this);
        }
        if (fragment instanceof MyDappsFragment) {
            MyDappsFragment f = (MyDappsFragment) fragment;
            f.setCallbacks(this);
        }
        if (fragment instanceof BrowserHistoryFragment) {
            BrowserHistoryFragment f = (BrowserHistoryFragment) fragment;
            f.setCallbacks(this, this);
        }
    }

    private void attachFragment(Fragment fragment, String tag) {
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            if (tag.equals(DAPP_HOME)) {
                DappHomeFragment f = (DappHomeFragment) fragment;
                showFragment(f, tag);
            } else if (tag.equals(DISCOVER_DAPPS)) {
                DiscoverDappsFragment f = (DiscoverDappsFragment) fragment;
                showFragment(f, tag);
            } else if (tag.equals(MY_DAPPS)) {
                MyDappsFragment f = (MyDappsFragment) fragment;
                showFragment(f, tag);
            } else if (tag.equals(HISTORY)) {
                BrowserHistoryFragment f = (BrowserHistoryFragment) fragment;
                showFragment(f, tag);
            } else {
                showFragment(fragment, tag);
            }
        }
    }

    private void attachFragment(String tag) {
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            if (tag.equals(DAPP_HOME)) {
                DappHomeFragment f = new DappHomeFragment();
                showFragment(f, tag);
            } else if (tag.equals(DISCOVER_DAPPS)) {
                DiscoverDappsFragment f = new DiscoverDappsFragment();
                showFragment(f, tag);
            } else if (tag.equals(MY_DAPPS)) {
                MyDappsFragment f = new MyDappsFragment();
                showFragment(f, tag);
            } else if (tag.equals(HISTORY)) {
                BrowserHistoryFragment f = new BrowserHistoryFragment();
                showFragment(f, tag);
            }
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        this.currentFragment = tag;
        getChildFragmentManager().beginTransaction()
                .add(R.id.frame, fragment, tag)
                .commit();

        if (tag.equals(DISCOVER_DAPPS) || tag.equals(MY_DAPPS) || tag.equals(HISTORY)) {
            back.setAlpha(1.0f);
            back.setOnClickListener(v -> homePressed());
        } else {
            setBackForwardButtons();
            back.setOnClickListener(v -> goToPreviousPage());
        }
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
        web3.setActivity(getActivity());
        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> web3.reload());
        toolbar = view.findViewById(R.id.address_bar);
        toolbar.inflateMenu(R.menu.menu_bookmarks);
        toolbar.getMenu().findItem(R.id.action_reload)
                .setOnMenuItemClickListener(menuItem -> {
                    reloadPage();
                    return true;
                });
        toolbar.getMenu().findItem(R.id.action_share)
                .setOnMenuItemClickListener(menuItem -> {
                    if (web3.getUrl() != null) {
                        viewModel.share(getContext(), web3.getUrl());
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

        currentNetwork = view.findViewById(R.id.current_network);
        currentNetwork.setOnClickListener(v -> selectNetwork());
        balance = view.findViewById(R.id.balance);
        symbol = view.findViewById(R.id.symbol);
    }

    private void selectNetwork() {
        SelectNetworkDialog dialog = new SelectNetworkDialog(getActivity(), viewModel.getNetworkList(), String.valueOf(networkInfo.chainId), true);
        dialog.setOnClickListener(v1 -> {
            if (networkInfo.chainId != dialog.getSelectedChainId()) {
                viewModel.setNetwork(dialog.getSelectedChainId());
                getActivity().sendBroadcast(new Intent(RESET_WALLET));
                balance.setVisibility(View.GONE);
                symbol.setVisibility(View.GONE);
            }
            dialog.dismiss();
        });
        dialog.show();
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
        //reset the pane if required
        if (oldChain > 0 && oldChain != this.networkInfo.chainId)
        {
            web3.reload();
        }
    }

    private void setupWeb3() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        web3.setChainId(networkInfo.chainId);
        String rpcURL = networkInfo.rpcServerUrl;
        web3.setRpcUrl(rpcURL);
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
    }

    @Override
    public void onSignMessage(Message<String> message) {
        DAppFunction dAppFunction = new DAppFunction() {
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

        dialog = new SignMessageDialog(getActivity(), message);
        dialog.setAddress(wallet.address);
        dialog.setOnApproveListener(v -> {
            //ensure we generate the signature correctly:
            byte[] signRequest = message.value.getBytes();
            if (message.value.substring(0, 2).equals("0x"))
            {
                signRequest = Numeric.hexStringToByteArray(message.value);
            }
            viewModel.signMessage(signRequest, dAppFunction, message);
        });
        dialog.setOnRejectListener(v -> {
            web3.onSignCancel(message);
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    public void onSignPersonalMessage(Message<String> message) {
        DAppFunction dAppFunction = new DAppFunction() {
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

        dialog = new SignMessageDialog(getActivity(), message);
        dialog.setAddress(wallet.address);
        dialog.setMessage(Hex.hexToUtf8(message.value));
        dialog.setOnApproveListener(v -> {
            String convertedMessage = Hex.hexToUtf8(message.value);
            String signMessage = PERSONAL_MESSAGE_PREFIX
                    + convertedMessage.length()
                    + convertedMessage;
            viewModel.signMessage(signMessage.getBytes(), dAppFunction, message);
        });
        dialog.setOnRejectListener(v -> {
            web3.onSignCancel(message);
            dialog.dismiss();
        });
        dialog.show();
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
        if (transaction.payload == null || transaction.payload.length() < 1)
        {
            //display transaction error
            onInvalidTransaction();
            web3.onSignCancel(transaction);
        }
        else
        {
            viewModel.openConfirmation(getContext(), transaction, url, networkInfo);
        }
    }

    private void onProgress() {
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.NONE);
        resultDialog.setTitle(R.string.title_dialog_sending);
        resultDialog.setMessage(R.string.transfer);
        resultDialog.setProgressMode();
        resultDialog.setCancelable(false);
        resultDialog.show();
    }

    private void onInvalidTransaction() {
        resultDialog = new AWalletAlertDialog(getActivity());
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.invalid_transaction));
        resultDialog.setMessage(getString(R.string.contains_no_data));
        resultDialog.setProgressMode();
        resultDialog.setCancelable(false);
        resultDialog.show();
    }

    private void goToPreviousPage() {
        if (web3.canGoBack()) {
            web3.goBack();
            detachFragments(true);
            urlTv.setText(sessionHistory.getItemAtIndex(sessionHistory.getCurrentIndex()-1).getUrl());
        }
    }

    private void goToNextPage() {
        if (web3.canGoForward()) {
            web3.goForward();
            detachFragments(true);
            urlTv.setText(sessionHistory.getItemAtIndex(sessionHistory.getCurrentIndex()+1).getUrl());
        }
    }

    @Override
    public void onWebpageLoaded(String url, String title)
    {
        DApp dapp = new DApp(title, url);
        DappBrowserUtils.addToHistory(getContext(), dapp);
        adapter.addSuggestion(dapp);
        sessionHistory = web3.copyBackForwardList();
        setBackForwardButtons();
    }

    private void setBackForwardButtons() {
        if (sessionHistory == null) {
            sessionHistory = web3.copyBackForwardList();
        }
        if (sessionHistory.getCurrentIndex() > 0) {
            back.setAlpha(1.0f);
        } else {
            back.setAlpha(0.3f);
        }
        if (sessionHistory.getCurrentIndex() < sessionHistory.getSize()-1) {
            next.setAlpha(1.0f);
        } else {
            next.setAlpha(0.3f);
        }
    }

    private boolean loadUrl(String urlText)
    {
        detachFragments(true);
        this.currentFragment = DAPP_BROWSER;
        cancelSearchSession();
        if (checkForMagicLink(urlText)) return true;
        web3.loadUrl(Utils.formatUrl(urlText));
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
        byte[] msgHash = (prefix + message).getBytes(); //Hash.sha3((prefix + message3).getBytes());

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

    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger)
    {
        //result
        String qrCode = null;
        try
        {
            if (resultCode == FullScannerFragment.SUCCESS && data != null)
            {
                qrCode = data.getStringExtra(FullScannerFragment.BarcodeObject);
            }

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
        catch (Exception e)
        {
            qrCode = null;
        }

        if (qrCode == null)
        {
            Toast.makeText(getContext(), R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }


    private boolean checkForMagicLink(String data)
    {
        try
        {
            ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions());
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

    private void DisplayAddressFound(String address, FragmentMessenger messenger)
    {
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
    }
}
