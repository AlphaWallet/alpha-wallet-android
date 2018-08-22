package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SignatureException;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.viewmodel.DappBrowserViewModel;
import io.stormbird.wallet.viewmodel.DappBrowserViewModelFactory;
import io.stormbird.wallet.web3.OnSignMessageListener;
import io.stormbird.wallet.web3.OnSignPersonalMessageListener;
import io.stormbird.wallet.web3.OnSignTransactionListener;
import io.stormbird.wallet.web3.OnSignTypedMessageListener;
import io.stormbird.wallet.web3.Web3View;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Transaction;
import io.stormbird.wallet.web3.entity.TypedData;
import io.stormbird.wallet.widget.SignMessageDialog;

import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;


public class DappBrowserFragment extends Fragment implements
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener
{
    private static final String ETH_RPC_URL = "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk";
    private static final String XCONTRACT_URL = "https://xcontract.herokuapp.com/sign";

    @Inject
    DappBrowserViewModelFactory dappBrowserViewModelFactory;
    private DappBrowserViewModel viewModel;

    private Web3View web3;
    private EditText url;
    private ProgressBar progressBar;
    private Wallet wallet;
    private NetworkInfo networkInfo;
    private SignMessageDialog dialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        web3 = view.findViewById(R.id.web3view);
        progressBar = view.findViewById(R.id.progressBar);
        url = view.findViewById(R.id.url);

        viewModel = ViewModelProviders.of(this, dappBrowserViewModelFactory)
                .get(DappBrowserViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);

        return view;
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        init();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
    }

    private void init() {
        url.setText(XCONTRACT_URL);
        url.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_GO) {
                web3.loadUrl(url.getText().toString());
                web3.requestFocus();
                handled = true;
            }
            return handled;
        });
        web3.loadUrl(url.getText().toString());
        setupWeb3();
    }

    private void setupWeb3() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        web3.setChainId(1);
        web3.setRpcUrl(ETH_RPC_URL);
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

        web3.setOnSignMessageListener(this);
        web3.setOnSignPersonalMessageListener(this);
        web3.setOnSignTransactionListener(this);
        web3.setOnSignTypedMessageListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    @Override
    public void onSignMessage(Message<String> message)
    {
        DAppFunction dAppFunction = new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Message<String> message)
            {
                web3.onSignCancel(message);
                dialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Message<String> message)
            {
                String signHex = Numeric.toHexString(data);
                web3.onSignMessageSuccessful(message, signHex);

                //TODO: Justin - here's how to to verify - which you should hook it's "web3Handler.verify(web3, message, signature)"
                //TODO: which returns an address. Ideally we edit the javascript to popup 'success' if the address returned matches the wallet address
                System.out.println(checkSignature(message, signHex));

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

    private String checkSignature(Message<String> message, String signHex)
    {
        byte[] messageCheck = message.value.getBytes();
        //if we're passed a hex then sign it correctly
        if (message.value.substring(0,2).equals("0x"))
        {
            messageCheck = Numeric.hexStringToByteArray(message.value);
        }

        //convert to signature
        Sign.SignatureData sigData = sigFromByteArray(Numeric.hexStringToByteArray(signHex));
        String recoveredAddress = "";

        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(messageCheck, sigData);
            recoveredAddress = Keys.getAddress(recoveredKey);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }

        return recoveredAddress;
    }

    @Override
    public void onSignPersonalMessage(Message<String> message) {
        //TODO
        Toast.makeText(getActivity(), message.value, Toast.LENGTH_LONG).show();
        web3.onSignCancel(message);
    }

    @Override
    public void onSignTypedMessage(Message<TypedData[]> message) {
        //TODO
        Toast.makeText(getActivity(), new Gson().toJson(message), Toast.LENGTH_LONG).show();
        web3.onSignCancel(message);
    }

    @Override
    public void onSignTransaction(Transaction transaction) {
        //TODO
        String str = new StringBuilder()
                .append(transaction.recipient == null ? "" : transaction.recipient.toString()).append(" : ")
                .append(transaction.contract == null ? "" : transaction.contract.toString()).append(" : ")
                .append(transaction.value.toString()).append(" : ")
                .append(transaction.gasPrice.toString()).append(" : ")
                .append(transaction.gasLimit).append(" : ")
                .append(transaction.nonce).append(" : ")
                .append(transaction.payload).append(" : ")
                .toString();
        Toast.makeText(getActivity(), str, Toast.LENGTH_LONG).show();
        web3.onSignCancel(transaction);
    }
}
