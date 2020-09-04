package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.util.MessageUtils;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.viewmodel.WalletConnectViewModelFactory;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.ProviderTypedData;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import kotlin.Unit;
import okhttp3.OkHttpClient;
import wallet.core.jni.Hash;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

public class WalletConnectActivity extends BaseActivity
{
    private static final String TAG = WalletConnectActivity.class.getSimpleName();
    public static final String WC_LOCAL_PREFIX = "wclocal:";
    public static final String WC_INTENT = "wcintent:";
    public static final String WC_SESSION_CREATE_ELEMENT = "?bridge";

    @Inject
    WalletConnectViewModelFactory viewModelFactory;
    WalletConnectViewModel viewModel;

    private WCClient client;
    private WCSession session;
    private WCPeerMeta peerMeta;

    private OkHttpClient httpClient;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView address;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;
    private boolean fromDappBrowser = false;
    private boolean fromPhoneBrowser = false;

    private Signable signable;
    private DAppFunction dappFunction;

    private Wallet wallet;
    private String qrCode;
    private Handler handler = new Handler();

    private SignAuthenticationCallback signCallback;
    private long lastId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        AndroidInjection.inject(this);

        setContentView(R.layout.activity_wallet_connect);

        toolbar();

        setTitle(getString(R.string.title_wallet_connect));

        initViews();

        initViewModel();

        retrieveQrCode();

        viewModel.prepare();
    }

    private void initViewModel()
    {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(WalletConnectViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void initViews()
    {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        address = findViewById(R.id.address);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setPrimaryButtonText(R.string.action_end_session);
        functionBar.setPrimaryButtonClickListener(v -> {
            onBackPressed();
        });
    }

    private void onDefaultWallet(Wallet wallet)
    {
        this.wallet = wallet;
        address.setText(wallet.address);
        handler.postDelayed(() -> {
            if (!wallet.address.isEmpty())
            {
                initWalletConnectPeerMeta();
                initWalletConnectClient();
                initWalletConnectSession();
            }
        }, 10); //very small delay, seems to help connection if connecting from local browser
    }

    private void initWalletConnectSession()
    {
        session = WCSession.Companion.from(qrCode);
        if (session != null)
        {
            client.connect(session, peerMeta, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        else
        {
            if (!qrCode.contains(WC_SESSION_CREATE_ELEMENT))
            {
                invalidSession();
            }
            else
            {
                Toast.makeText(this, R.string.invalid_walletconnect_data, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void invalidSession()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.invalid_walletconnect_session)
                .setMessage(R.string.restart_walletconnect_session)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> finish())
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void initWalletConnectPeerMeta()
    {
        String name = getString(R.string.app_name);
        String url = "https://www.alphawallet.com";
        String description = wallet.address;
        String[] icons = {"https://alphawallet.com/wp-content/themes/alphawallet/img/alphawallet-logo.svg"};

        peerMeta = new WCPeerMeta(
                name,
                url,
                description,
                Arrays.asList(icons)
        );
    }

    private void retrieveQrCode()
    {
        Bundle data = getIntent().getExtras();
        if (data != null)
        {
            String walletConnectCode = data.getString("qrCode");
            if (walletConnectCode != null && walletConnectCode.startsWith(WC_LOCAL_PREFIX)) {
                walletConnectCode = walletConnectCode.replace(WC_LOCAL_PREFIX, "");
                fromDappBrowser = true;
            }
            else if (walletConnectCode != null && walletConnectCode.startsWith(WC_INTENT)) {
                walletConnectCode = walletConnectCode.replace(WC_INTENT, "");
                fromPhoneBrowser = true; //don't use this yet, but could use it for switching between apps
            }
            this.qrCode = walletConnectCode;
            System.out.println("WCClient: " + qrCode);
        }
        else
        {
            Toast.makeText(this, "Error retrieving QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initWalletConnectClient()
    {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        client = new WCClient(new GsonBuilder(), httpClient);

        client.setOnSessionRequest((id, peer) -> {
            runOnUiThread(() -> {
                onSessionRequest(id, peer);
            });
            return Unit.INSTANCE;
        });

        client.setOnFailure(throwable -> {
            runOnUiThread(() -> {
                onFailure(throwable);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSign((id, message) -> {
            runOnUiThread(() -> {
                onEthSign(id, message);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSignTransaction((id, transaction) -> {
            runOnUiThread(() -> {
                onEthSignTransaction(id, transaction);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSendTransaction((id, transaction) -> {
            runOnUiThread(() -> {
                onEthSendTransaction(id, transaction);
            });
            return Unit.INSTANCE;
        });

        client.setOnDisconnect((code, reason) -> {
            finish();
            return Unit.INSTANCE;
        });
    }

    /**
     * If we're using AlphaWallet to act as signing agent for another browser running locally on the device,
     * We don't get the 'close' event if user closes the session on the other browser.
     * Check when resuming AW if the connection is still valid
     */
    @Override
    public void onResume()
    {
        super.onResume();
        //check client connection
        if (client != null && !client.isConnected())
        {
            finish();
        }
    }

    private void onSessionRequest(Long id, WCPeerMeta peer)
    {
        String[] accounts = {wallet.address};

        Glide.with(this)
                .load(peer.getIcons().get(0))
                .into(icon);
        peerName.setText(peer.getName());
        peerUrl.setText(peer.getUrl());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setIcon(icon.getDrawable())
                .setTitle(peer.getName())
                .setMessage(peer.getUrl())
                .setPositiveButton(R.string.dialog_approve, (d, w) -> {
                    client.approveSession(Arrays.asList(accounts), 1);
                    progressBar.setVisibility(View.GONE);
                    functionBar.setVisibility(View.VISIBLE);
                    infoLayout.setVisibility(View.VISIBLE);
                    if (fromDappBrowser)
                    {
                        //switch back to dappBrowser
                        switchToDappBrowser();
                    }
                })
                .setNegativeButton(R.string.dialog_reject, (d, w) -> {
                    client.rejectSession(getString(R.string.message_reject_request));
                    finish();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSign(Long id, WCEthereumSignMessage message)
    {
        int signType = 0;
        signable = null;
        lastId = id;
        switch (message.getType())
        {
            case MESSAGE:
                signType = R.string.dialog_title_sign_message;
                signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id);
                break;
            case PERSONAL_MESSAGE:
                signType = R.string.dialog_title_sign_message;
                signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id, true);
                break;
            case TYPED_MESSAGE:
                signType = R.string.dialog_title_sign_typed_message;
                //See TODO in SignCallbackJSInterface, refactor duplicate code
                try
                {
                    try
                    {
                        ProviderTypedData[] rawData = new Gson().fromJson(message.getData(), ProviderTypedData[].class);
                        ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream();
                        writeBuffer.write(Hash.keccak256(MessageUtils.encodeParams(rawData)));
                        writeBuffer.write(Hash.keccak256(MessageUtils.encodeValues(rawData)));
                        CharSequence signMessage = MessageUtils.formatTypedMessage(rawData);
                        signable = new EthereumTypedMessage(writeBuffer.toByteArray(), signMessage, peerUrl.getText().toString(), id);
                    }
                    catch (JsonSyntaxException e)
                    {
                        StructuredDataEncoder eip721Object = new StructuredDataEncoder(message.getData());
                        CharSequence signMessage = MessageUtils.formatEIP721Message(eip721Object);
                        signable = new EthereumTypedMessage(eip721Object.getStructuredData(), signMessage, peerUrl.getText().toString(), id);
                    }
                }
                catch (IOException e)
                {
                    showErrorDialog(getString(R.string.message_authentication_failed));
                    client.rejectRequest(id, getString(R.string.message_authentication_failed));
                    if (fromDappBrowser) switchToDappBrowser();
                }
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(signType)
                .setMessage(signable.getUserMessage())
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    doSignMessage();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    client.rejectRequest(id, getString(R.string.message_reject_request));
                    if (fromDappBrowser) switchToDappBrowser();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction)
    {
        Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.dialog_title_sign_transaction)
                .setMessage(w3Tx.getFormattedTransaction(this, MAINNET_ID))
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    signTransaction(id, transaction);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    client.rejectRequest(id, getString(R.string.message_reject_request));
                    if (fromDappBrowser) switchToDappBrowser();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction)
    {
        sendTransaction(id, transaction);
    }

    private void onFailure(Throwable throwable)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(throwable.getMessage())
                .setNeutralButton(R.string.try_again, (d, w) -> {
                    finish();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void doSignMessage()
    {
        dappFunction = new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Signable message)
            {
                showErrorDialog(error.getMessage());
                if (fromDappBrowser) switchToDappBrowser();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                client.approveRequest(message.getCallbackId(), Numeric.toHexString(data));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                viewModel.signMessage(
                        signable,
                        dappFunction);
            }

            @Override
            public void cancelAuthentication()
            {
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(lastId, getString(R.string.message_authentication_failed));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        viewModel.getAuthenticationForSignature(wallet, this, signCallback);
    }

    private void sendTransaction(Long id, WCEthereumTransaction transaction)
    {
        lastId = id;
        try
        {
            //minimum for transaction to be valid: recipient and value or payload
            if ((transaction.getTo().equals(Address.EMPTY) && transaction.getData() != null) // Constructor
                    || (!transaction.getTo().equals(Address.EMPTY) && (transaction.getData() != null || transaction.getValue() != null))) // Raw or Function TX
            {
                viewModel.confirmTransaction(this, transaction, peerUrl.getText().toString(), MAINNET_ID, id);
            }
            else
            {
                //display transaction error
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(id, getString(R.string.message_authentication_failed));
            }
        }
        catch (Exception e)
        {
            showErrorDialog(getString(R.string.message_authentication_failed));
            client.rejectRequest(id, getString(R.string.message_authentication_failed));
        }
    }

    private void signTransaction(Long id, WCEthereumTransaction transaction)
    {
        lastId = id;
        Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        //simply sign the transaction and return
        dappFunction = new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Signable message)
            {
                showErrorDialog(error.getMessage());
                if (fromDappBrowser) switchToDappBrowser();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                client.approveRequest(message.getCallbackId(), Numeric.toHexString(data));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                //sign the transaction
                viewModel.signTransaction(getBaseContext(), w3Tx, dappFunction, peerUrl.getText().toString(), MAINNET_ID);
                if (fromDappBrowser) switchToDappBrowser();
            }

            @Override
            public void cancelAuthentication()
            {
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(lastId, getString(R.string.message_authentication_failed));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        viewModel.getAuthenticationForSignature(wallet, this, signCallback);
    }

    private void killSession()
    {
        System.out.println(TAG + ": Terminate Session");
        if (client != null && session != null && client.isConnected())
        {
            client.killSession();
        }
        else
        {
            finish();
        }
    }

    private void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(message)
                .setPositiveButton(R.string.try_again, (d, w) -> {
                    initWalletConnectSession();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    public void onDestroy()
    {
        Thread shut = new Thread(shutDown); //shut down on async
        shut.start();
        super.onDestroy();
    }

    private Runnable shutDown = () -> killSession();

    @Override
    public void onBackPressed()
    {
        if (!fromDappBrowser)
        {
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                        .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                            Thread shut = new Thread(shutDown); //shut down on async in case this is solitary application
                            shut.start();
                        })
                        .setNegativeButton(R.string.action_cancel, (d, w) -> {
                            d.dismiss();
                        })
                        .create();
                dialog.show();
            });
        }
        else
        {
            switchToDappBrowser();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if (requestCode == C.REQUEST_TRANSACTION_CALLBACK)
            {
                handleTransactionCallback(resultCode, data);
            }
            else if (signCallback != null) signCallback.gotAuthorisation(true);
        }
        else
        {
            showErrorDialog(getString(R.string.message_authentication_failed));
            client.rejectRequest(lastId, getString(R.string.message_authentication_failed));
        }
    }

    //return from the openConfirmation above
    public void handleTransactionCallback(int resultCode, Intent data)
    {
        if (data == null) return;
        Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
        if (resultCode == RESULT_OK && web3Tx != null)
        {
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            client.approveRequest(lastId, hashData);
        }
        else if (web3Tx != null)
        {
            showErrorDialog(getString(R.string.message_authentication_failed));
            client.rejectRequest(lastId, getString(R.string.message_authentication_failed));
        }

        if (fromDappBrowser) switchToDappBrowser();
    }

    private void switchToDappBrowser()
    {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
}
