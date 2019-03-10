package io.stormbird.wallet.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.FragmentMessenger;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.SignTransactionInterface;
import io.stormbird.wallet.entity.URLLoadInterface;
import io.stormbird.wallet.entity.URLLoadReceiver;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.OnDappClickListener;
import io.stormbird.wallet.ui.widget.OnDappHomeNavClickListener;
import io.stormbird.wallet.ui.widget.adapter.AutoCompleteUrlAdapter;
import io.stormbird.wallet.ui.widget.entity.ItemClickListener;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.ui.zxing.QRScanningActivity;
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
import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;
import static io.stormbird.wallet.ui.HomeActivity.DAPP_BARCODE_READER_REQUEST_CODE;

public class DappBrowserFragment extends Fragment implements
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener,
        URLLoadInterface, ItemClickListener, SignTransactionInterface, OnDappClickListener, OnDappHomeNavClickListener
{
    private static final String TAG = DappBrowserFragment.class.getSimpleName();
    private static final String DAPP_HOME = "DAPP_HOME";
    private static final String MY_DAPPS = "MY_DAPPS";
    private static final String DISCOVER_DAPPS = "DISCOVER_DAPPS";
    private static final String HISTORY = "HISTORY";
    private static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

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
    private AutoCompleteUrlAdapter adapter;
    private URLLoadReceiver URLReceiver;

    private Fragment dappHomeFragment;
    private Fragment myDappsFragment;
    private Fragment discoverDappsFragment;
    private Fragment browserHistoryFragment;

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
        if (adapter == null || !adapter.hasContext()) {
            setupAddressBar();
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

        // Load url from a link within the app
        if (getArguments() != null && getArguments().getString("url") != null) {
            String url = getArguments().getString("url");
            loadUrl(url);
        } else {
            attachFragment(dappHomeFragment, DAPP_HOME);
        }

        return view;
    }

    private void attachFragment(Fragment fragment, String tag) {
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            if (tag.equals(DAPP_HOME)) {
                DappHomeFragment f = (DappHomeFragment) fragment;
                f.setCallbacks(this, this);
                showFragment(f, tag);
            } else if (tag.equals(DISCOVER_DAPPS)) {
                DiscoverDappsFragment f = (DiscoverDappsFragment) fragment;
                f.setCallbacks(this);
                showFragment(f, tag);
            } else if (tag.equals(MY_DAPPS)) {
                MyDappsFragment f = (MyDappsFragment) fragment;
                f.setCallbacks(this);
                showFragment(f, tag);
            } else if (tag.equals(HISTORY)) {
                BrowserHistoryFragment f = (BrowserHistoryFragment) fragment;
                f.setCallbacks(this);
                showFragment(f, tag);
            }
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        getChildFragmentManager().beginTransaction()
                .add(R.id.frame, fragment, tag)
                .commit();
    }

    private void detachFragments(boolean detachHome) {
        Fragment fragment;
        if (detachHome) {
            fragment = getChildFragmentManager().findFragmentByTag(DAPP_HOME);
            if (fragment != null && fragment.isVisible()) {
                getChildFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        }

        fragment = getChildFragmentManager().findFragmentByTag(MY_DAPPS);
        if(fragment != null && fragment.isVisible()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }

        fragment = getChildFragmentManager().findFragmentByTag(DISCOVER_DAPPS);
        if(fragment != null && fragment.isVisible()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }

        fragment = getChildFragmentManager().findFragmentByTag(HISTORY);
        if(fragment != null && fragment.isVisible()) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
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
        detachFragments(true);
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
    }

    private void setupAddressBar() {
//        urlTv.setText(viewModel.getLastUrl(getContext()));

        adapter = new AutoCompleteUrlAdapter(getContext(), C.DAPP_BROWSER_HISTORY);
        adapter.setListener(this);
        urlTv.setAdapter(adapter);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO)
            {
                String urlText = urlTv.getText().toString();
                handled = loadUrl(urlText);
                detachFragments(true);
            }
            return handled;
        });

        urlTv.setOnClickListener(v -> urlTv.showDropDown());
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, dappBrowserViewModelFactory)
                .get(DappBrowserViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        setupWeb3();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
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
                //Test Sig
                testRecoverAddressFromSignature(Hex.hexToUtf8(message.value), signHex);
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
            viewModel.openConfirmation(getContext(), transaction, url);
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

    @Override
    public void onWebpageLoaded(String url)
    {
        adapter.addDAppURL(url);
    }

    private boolean loadUrl(String urlText)
    {
        web3.loadUrl(Utils.formatUrl(urlText));
        urlTv.setText(Utils.formatUrl(urlText));
        web3.requestFocus();
        viewModel.setLastUrl(getContext(), urlText);
        adapter.add(Utils.formatUrl(urlText));
        adapter.notifyDataSetChanged();
        KeyboardUtils.hideKeyboard(urlTv);
        Activity current = getActivity();
        if (current != null)
        {
            current.sendBroadcast(new Intent(RESET_TOOLBAR));
        }
        return true;
    }

    public void addBookmark()
    {
        if (urlTv != null && urlTv.getText() != null)
        {
            viewModel.addBookmark(getContext(), urlTv.getText().toString());
        }
    }

    public void viewBookmarks()
    {
        if (viewModel == null) return;
        List<String> bookmarks = viewModel.getBookmarks();
        //display in popup
        if (getActivity() == null) return;
        SelectNetworkDialog dialog = new SelectNetworkDialog(getActivity(), bookmarks.toArray(new String[bookmarks.size()]), urlTv.getText().toString());
        dialog.setTitle(R.string.bookmarks);
        dialog.setButtonText(R.string.visit);
        dialog.setOnClickListener(v1 -> {
            String url = dialog.getSelectedItem();
            urlTv.setText(url);
            loadUrl(url);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void removeBookmark()
    {
        viewModel.removeBookmark(getContext(), urlTv.getText().toString());
    }

    public boolean getUrlIsBookmark()
    {
        return viewModel != null && urlTv != null && viewModel.getBookmarks().contains(urlTv.getText().toString());
    }

    public void reloadPage() {
        web3.reload();
    }

    public void share() {
        if (web3.getUrl() != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, web3.getUrl());
            intent.setType("text/plain");
            startActivity(intent);
        }
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

    public void scanQR()
    {
        //scanning intent
        Intent intent = new Intent(getContext(), QRScanningActivity.class);
        startActivityForResult(intent, DAPP_BARCODE_READER_REQUEST_CODE);
    }

    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger)
    {
        //result
        String qrCode = null;
        if (resultCode == FullScannerFragment.SUCCESS && data != null)
        {
            qrCode = data.getStringExtra(FullScannerFragment.BarcodeObject);
        }

        if (qrCode != null)
        {
            //detect if this is an address
            if (isAddressValid(qrCode))
            {
                DisplayAddressFound(qrCode, messenger);
            }
            else
            {
                //attempt to go to site
                loadUrl(qrCode);
            }
        }
        else
        {
            Toast.makeText(getContext(), R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
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

    private boolean isAddressValid(String address)
    {
        try
        {
            new org.web3j.abi.datatypes.Address(address);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
