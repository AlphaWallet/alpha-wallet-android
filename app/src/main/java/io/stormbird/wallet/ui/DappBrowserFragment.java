package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.AutoCompleteUrlAdapter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.DappBrowserViewModel;
import io.stormbird.wallet.viewmodel.DappBrowserViewModelFactory;
import io.stormbird.wallet.web3.OnGetBalanceListener;
import io.stormbird.wallet.web3.OnSignMessageListener;
import io.stormbird.wallet.web3.OnSignPersonalMessageListener;
import io.stormbird.wallet.web3.OnSignTransactionListener;
import io.stormbird.wallet.web3.OnSignTypedMessageListener;
import io.stormbird.wallet.web3.OnVerifyListener;
import io.stormbird.wallet.web3.Web3View;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.TypedData;
import io.stormbird.wallet.web3.entity.Web3Transaction;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignMessageDialog;

import static io.stormbird.wallet.C.ETH_SYMBOL;
import static io.stormbird.wallet.ui.ImportTokenActivity.getEthString;


public class DappBrowserFragment extends Fragment implements
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener, OnVerifyListener, OnGetBalanceListener {
    private static final String TAG = DappBrowserFragment.class.getSimpleName();

    @Inject
    DappBrowserViewModelFactory dappBrowserViewModelFactory;
    private DappBrowserViewModel viewModel;

    private Web3View web3;
    private AutoCompleteTextView urlTv;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo networkInfo;
    private SignMessageDialog dialog;
    private AWalletAlertDialog resultDialog;
    private AutoCompleteUrlAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        initView(view);
        initViewModel();
        setupAddressBar();
        viewModel.prepare();
        return view;
    }

    private void initView(View view) {
        web3 = view.findViewById(R.id.web3view);
        progressBar = view.findViewById(R.id.progressBar);
        urlTv = view.findViewById(R.id.url_tv);
    }

    private void setupAddressBar() {
        urlTv.setText(viewModel.getLastUrl(getContext()));

        adapter = new AutoCompleteUrlAdapter(getContext(), viewModel.getBrowserHistoryFromPrefs(getContext()));
        urlTv.setAdapter(adapter);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String urlText = urlTv.getText().toString();
                web3.loadUrl(Utils.formatUrl(urlText));
                web3.requestFocus();
                viewModel.setLastUrl(getContext(), urlText);
                viewModel.addToBrowserHistory(getContext(), urlText);
                adapter.add(urlText);
                adapter.notifyDataSetChanged();
                dismissKeyboard();
                handled = true;
            }
            return handled;
        });

        urlTv.setOnItemClickListener((parent, view, position, id) -> {
            web3.loadUrl(Utils.formatUrl(adapter.getItem(position)));
            web3.requestFocus();
            dismissKeyboard();
        });
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlTv.getWindowToken(), 0);
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

        // Default to last opened site
        if (web3.getUrl() == null) {
            web3.loadUrl(Utils.formatUrl(viewModel.getLastUrl(getContext())));
        }
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
    }

    private void setupWeb3() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        web3.setChainId(1);
        web3.setRpcUrl(C.ETH_RPC_URL);
        web3.setWalletAddress(new Address(wallet.address));

        web3.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView webview, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
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
        web3.setOnVerifyListener(this);
        web3.setOnGetBalanceListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
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
            viewModel.signMessage(message.value, dAppFunction, message);
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
                dialog.dismiss();
            }
        };

        dialog = new SignMessageDialog(getActivity(), message);
        dialog.setAddress(wallet.address);
        dialog.setOnApproveListener(v -> {
            String hex = hexToUtf8(message.value);
            String signMessage = ("\u0019Ethereum Signed Message:\n"
                    + hex.getBytes().length
                    + org.web3j.utils.Numeric.cleanHexPrefix(hex));
            viewModel.signMessage(signMessage, dAppFunction, message);
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
    public void onSignTransaction(Web3Transaction transaction, String url) {

        DAppFunction dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Message<String> message) {
                web3.onSignCancel(message);
                resultDialog.dismiss();

                resultDialog = new AWalletAlertDialog(getActivity());
                resultDialog.setTitle(getString(R.string.dialog_sign_transaction_failed));
                resultDialog.setIcon(AWalletAlertDialog.ERROR);
                resultDialog.setMessage(error.getMessage());
                resultDialog.setButtonText(R.string.dialog_ok);
                resultDialog.show();
            }

            @Override
            public void DAppReturn(byte[] data, Message<String> message) {
                String txHash = Numeric.toHexString(data);
                Log.d(TAG, "Initial Msg: " + message.value);
                web3.onSignTransactionSuccessful(transaction, txHash);  //onSignPersonalMessageSuccessful(message, signHex);
                resultDialog.dismiss();

                resultDialog = new AWalletAlertDialog(getActivity());
                resultDialog.setTitle(getString(R.string.dialog_sign_transaction_success));
                resultDialog.setIcon(AWalletAlertDialog.SUCCESS);
                resultDialog.setMessage(txHash);
                resultDialog.setButtonText(R.string.dialog_ok);
                resultDialog.show();
            }
        };

        dialog = new SignMessageDialog(getActivity());
        dialog.setRequester(url);
        dialog.setMessage("Transaction");
        dialog.setAddress(wallet.address);
        //calculate value of eth + gas
        Web3Transaction cTrans = viewModel.doGasSettings(transaction);

        BigInteger gasPrice = cTrans.gasPrice.multiply(cTrans.gasLimit); // TODO: Use web3 estimate gas
        BigInteger value = cTrans.value.add(gasPrice);
        BigDecimal eth = BalanceUtils.weiToEth(new BigDecimal(value));

        String ethPrice = getEthString(eth.doubleValue()) + " " + ETH_SYMBOL;
        String usdPrice = viewModel.getUSDValue(eth.doubleValue());
        dialog.setValue(ethPrice, usdPrice, viewModel.getNetworkName());
        dialog.setOnApproveListener(v -> {
            dialog.dismiss();
            //popup transaction wait dialog
            onProgress();
            viewModel.signTransaction(transaction, dAppFunction, url);
        });
        dialog.setOnRejectListener(v -> {
            web3.onSignCancel(cTrans);
            dialog.dismiss();
        });
        dialog.show();

        //Toast.makeText(getActivity(), str, Toast.LENGTH_LONG).show();
        //web3.onSignCancel(transaction);
    }

    @Override
    public void onVerify(String message, String signHex) {
        web3.onVerify(viewModel.getRecoveredAddress(message, signHex), viewModel.getVerificationResult(getContext(), wallet, message, signHex));
    }

    @Override
    public void onGetBalance(String balance) {
        web3.onGetBalance(viewModel.getFormattedBalance(balance));
    }

    public static String hexToUtf8(String hex) {
        hex = org.web3j.utils.Numeric.cleanHexPrefix(hex);
        ByteBuffer buff = ByteBuffer.allocate(hex.length() / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            buff.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        buff.rewind();
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = cs.decode(buff);
        return cb.toString();
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
}
