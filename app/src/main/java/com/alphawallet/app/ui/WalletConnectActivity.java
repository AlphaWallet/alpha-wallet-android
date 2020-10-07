package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
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
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.repository.SignRecord;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import kotlin.Unit;
import okhttp3.OkHttpClient;
import wallet.core.jni.Hash;

import static com.alphawallet.app.repository.EthereumNetworkBase.VELAS_MAINNET_ID;

public class WalletConnectActivity extends BaseActivity
{
    private static final String TAG = "WCClient";
    public static final String WC_LOCAL_PREFIX = "wclocal:";
    public static final String WC_INTENT = "wcintent:";

    @Inject
    WalletConnectViewModelFactory viewModelFactory;
    WalletConnectViewModel viewModel;

    private WCClient client;
    private WCSession session;
    private WCPeerMeta peerMeta;

    private OkHttpClient httpClient;
    private AlertDialog signDialog;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView address;
    private TextView signCount;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;
    private boolean fromDappBrowser = false;  //if using this from dappBrowser (which is a bit strange but could happen) then return back to browser once signed
    private boolean fromPhoneBrowser = false; //if from phone browser, clicking 'back' should take user back to dapp running on the phone's browser,
                                              // -- according to WalletConnect's expected UX docs.
    private boolean fromSessionActivity = false;

    private Signable signable;
    private DAppFunction dappFunction;

    private Wallet wallet;
    private String qrCode;

    private SignAuthenticationCallback signCallback;
    private long lastId;

    private boolean startup = false;
    private boolean switchConnection = false;
    private boolean terminateSession = false;

    @Nullable
    private Disposable messageCheck;

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

        Log.d(TAG, "Starting Activity: " + getSessionId());
        startup = true;
        viewModel.prepare();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Bundle data = intent.getExtras();
        if (data != null)
        {
            String sessionId = getSessionId();
            parseSessionCode(data.getString("qrCode"));
            String newSessionId = getSessionId();
            Log.d(TAG, "Received New Intent: " + newSessionId + " (" + sessionId + ")");
            client = viewModel.getClient(newSessionId);

            if (client == null || !client.isConnected())
            {
                //TODO: ERROR!
                showErrorDialogTerminate(getString(R.string.session_terminated));
                infoLayout.setVisibility(View.GONE);
            }
            else
            {
                //setup the screen
                Log.d(TAG, "Resume Connection session: " + newSessionId);
                displaySessionStatus(newSessionId);
            }
        }
    }

    private void retrieveQrCode()
    {
        Bundle data = getIntent().getExtras();
        if (data != null)
        {
            String qrCode = data.getString("qrCode");
            String sessionId = data.getString("session");
            if (sessionId != null) fromSessionActivity = true;
            if (!TextUtils.isEmpty(qrCode))
            {
                parseSessionCode(data.getString("qrCode"));
            }
            else if (!TextUtils.isEmpty(sessionId))
            {
                session = viewModel.getSession(sessionId);
            }
        }
        else
        {
            Toast.makeText(this, "Error retrieving QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void parseSessionCode(String wcCode)
    {
        if (wcCode != null && wcCode.startsWith(WC_LOCAL_PREFIX)) {
            wcCode = wcCode.replace(WC_LOCAL_PREFIX, "");
            fromDappBrowser = true;
        }
        else if (wcCode != null && wcCode.startsWith(WC_INTENT)) {
            wcCode = wcCode.replace(WC_INTENT, "");
            fromPhoneBrowser = true; //don't use this yet, but could use it for switching between apps
        }
        this.qrCode = wcCode;
        session = WCSession.Companion.from(qrCode);
        System.out.println("WCClient: " + qrCode);
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
        signCount = findViewById(R.id.tx_count);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setPrimaryButtonText(R.string.action_end_session);
        functionBar.setPrimaryButtonClickListener(v -> {
            endSessionDialog();
        });
    }

    //TODO: Refactor this into elements - this function is unmaintainable
    private void onDefaultWallet(Wallet wallet)
    {
        this.wallet = wallet;
        address.setText(wallet.address);

        Log.d(TAG, "Open Connection: " + getSessionId());

        String peerId;
        String sessionId = getSessionId();
        String connectionId = viewModel.getRemotePeerId(sessionId);
        if (!TextUtils.isEmpty(wallet.address))
        {
            if (connectionId == null && session != null) //new session request
            {
                Log.d(TAG, "New Session: " + getSessionId());
                //new connection, create a random ID to identify us to the remotePeer.
                peerId = UUID.randomUUID().toString(); //Create a new ID for our side of this session. The remote peer uses this ID to identify us
                connectionId = null; //connectionId is only relevant for resuming a session
            }
            else //existing session, rebuild the session data
            {
                client = viewModel.getClient(sessionId);
                Log.d(TAG, "Resume Session: " + getSessionId());
                //try to retrieve the session from database
                session = viewModel.getSession(sessionId);
                peerId = viewModel.getPeerId(sessionId);
                displaySessionStatus(sessionId);

                if (client != null && client.isConnected())
                {
                    Log.d(TAG, "Use running session: " + getSessionId());
                }
                else
                {
                    if (!fromSessionActivity && (session == null || client == null))
                    {
                        showErrorDialogTerminate(getString(R.string.session_terminated));
                        infoLayout.setVisibility(View.GONE);
                    }
                    else
                    {
                        //session is not current, no need to display the 'end session' button
                        functionBar.setVisibility(View.GONE);
                    }
                }

                startup = false;
                return;
            }

            Log.d(TAG, "connect: peerID " + peerId);
            Log.d(TAG, "connect: remotePeerID " + connectionId);

            initWalletConnectPeerMeta();
            initWalletConnectClient();
            initWalletConnectSession(peerId, connectionId);
        }

        startup = false;
    }

    private void startMessageCheck()
    {
        if (messageCheck != null && !messageCheck.isDisposed()) messageCheck.dispose();

        messageCheck = Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                .doOnNext(l -> checkMessages()).subscribe();
    }

    private void stopMessageCheck()
    {
        if (messageCheck != null && !messageCheck.isDisposed()) messageCheck.dispose();
    }

    private void checkMessages()
    {
        WCRequest rq = viewModel.getPendingRequest(getSessionId());
        if (rq != null)
        {
            switch (rq.type)
            {
                case MESSAGE:
                    runOnUiThread(() -> {
                        onEthSign(rq.id, rq.sign);
                    });
                    break;
                case SIGN_TX:
                    runOnUiThread(() -> {
                        onEthSignTransaction(rq.id, rq.tx);
                    });
                    break;
                case SEND_TX:
                    runOnUiThread(() -> {
                        onEthSendTransaction(rq.id, rq.tx);
                    });
                    break;
                case FAILURE:
                    runOnUiThread(() -> {
                        onFailure(rq.throwable);
                    });
                    break;
                case SESSION_REQUEST:
                    Log.d(TAG, "On Request: " + rq.peer.getName());
                    runOnUiThread(() -> {
                        onSessionRequest(rq.id, rq.peer);
                    });
                    break;
            }
        }
    }

    private String getSessionId()
    {
        if (qrCode != null)
        {
            String uriString = qrCode.replace("wc:", "wc://");
            return Uri.parse(uriString).getUserInfo();
        }
        else if (session != null)
        {
            return session.getTopic();
        }
        else
        {
            return null;
        }
    }

    private void initWalletConnectSession(String sessionId, String connectionId)
    {
        if (session == null)
        {
            //error situation!
            invalidSession();
            return;
        }

        Log.d(TAG, "Connect: " + getSessionId() + " (" + connectionId + ")");
        client.connect(session, peerMeta, sessionId, connectionId);
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

    private void initWalletConnectClient()
    {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        client = new WCClient(new GsonBuilder(), httpClient);

        client.setOnWCOpen(peerId -> {
            viewModel.putClient(getSessionId(), client);
            Log.d(TAG, "On Open: " + peerId);
            return Unit.INSTANCE;
        });

        client.setOnDisconnect((code, reason) -> {
            Log.d(TAG, "Terminate session?");
            if (viewModel != null) viewModel.pruneSession(client.sessionId());
            if (terminateSession)
            {
                Log.d(TAG, "Yes");
                finish();
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        startMessageCheck();
    }

    private void displaySessionStatus(String sessionId)
    {
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        WCPeerMeta remotePeerData = viewModel.getRemotePeer(sessionId);

        if (remotePeerData != null)
        {
            Glide.with(this)
                    .load(remotePeerData.getIcons().get(0))
                    .into(icon);
            peerName.setText(remotePeerData.getName());
            peerUrl.setText(remotePeerData.getUrl());
            updateSignCount();
        }
    }

    private void updateSignCount()
    {
        ArrayList<SignRecord> recordList = viewModel.getSignRecords(getSessionId());
        signCount.setText(String.valueOf(recordList.size()));

        if (recordList.size() > 0)
        {
            LinearLayout signLayout = findViewById(R.id.layout_sign);
            signLayout.setOnClickListener(v -> {
                //switch to view of signs
                Intent intent = new Intent(getApplication(), SignDetailActivity.class);
                intent.putParcelableArrayListExtra(C.EXTRA_STATE, recordList);
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent);
            });
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
        signCount.setText(R.string.empty);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setIcon(icon.getDrawable())
                .setTitle(peer.getName())
                .setMessage(peer.getUrl())
                .setPositiveButton(R.string.dialog_approve, (d, w) -> {
                    client.approveSession(Arrays.asList(accounts), 1);
                    viewModel.createNewSession(getSessionId(), client.getPeerId(), client.getRemotePeerId(), new Gson().toJson(session), new Gson().toJson(peer));
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
        signDialog = builder.setTitle(signType)
                .setMessage(signable.getUserMessage())
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    if (signDialog != null && signDialog.isShowing()) signDialog.cancel();
                    doSignMessage();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    viewModel.rejectRequest(getSessionId(), id, getString(R.string.message_reject_request));
                    if (fromDappBrowser) switchToDappBrowser();
                })
                .setCancelable(false)
                .create();
        signDialog.show();
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction)
    {
        Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.dialog_title_sign_transaction)
                .setMessage(w3Tx.getFormattedTransaction(this, VELAS_MAINNET_ID))
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    signTransaction(id, transaction);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    viewModel.rejectRequest(getSessionId(), id, getString(R.string.message_reject_request));
                    if (fromDappBrowser) switchToDappBrowser();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onFailure(Throwable throwable)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(throwable.getMessage())
                .setPositiveButton(R.string.try_again, (d, w) -> {
                    onDefaultWallet(wallet);
                })
                .setNeutralButton(R.string.action_cancel, (d, w) -> {
                    killSession();
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
                signable = null;
                if (fromDappBrowser) switchToDappBrowser();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                //store sign
                viewModel.recordSign(signable, getSessionId());
                signable = null;
                viewModel.approveRequest(getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                updateSignCount();
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
                showErrorDialogCancel(getString(R.string.title_dialog_error), getString(R.string.message_authentication_failed));
                viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_authentication_failed));
                signable = null;
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        viewModel.getAuthenticationForSignature(wallet, this, signCallback);
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction)
    {
        lastId = id;
        try
        {
            //minimum for transaction to be valid: recipient and value or payload
            if ((transaction.getTo().equals(Address.EMPTY) && transaction.getData() != null) // Constructor
                    || (!transaction.getTo().equals(Address.EMPTY) && (transaction.getData() != null || transaction.getValue() != null))) // Raw or Function TX
            {
                signable = new EthereumMessage(transaction.toString(), peerUrl.getText().toString(), id);
                viewModel.confirmTransaction(this, transaction, peerUrl.getText().toString(), VELAS_MAINNET_ID, id);
            }
            else
            {
                //display transaction error
                showErrorDialogCancel(getString(R.string.title_dialog_error), getString(R.string.message_authentication_failed));
                viewModel.rejectRequest(getSessionId(), id, getString(R.string.message_authentication_failed));
            }
        }
        catch (Exception e)
        {
            showErrorDialogCancel(getString(R.string.title_dialog_error), getString(R.string.message_authentication_failed));
            viewModel.rejectRequest(getSessionId(), id, getString(R.string.message_authentication_failed));
        }
    }

    private void signTransaction(Long id, WCEthereumTransaction transaction)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
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
                viewModel.recordSignTransaction(getApplicationContext(), w3Tx, VELAS_MAINNET_ID, getSessionId());
                updateSignCount();
                viewModel.approveRequest(getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                //sign the transaction
                signCallback = null;
                viewModel.signTransaction(getBaseContext(), w3Tx, dappFunction, peerUrl.getText().toString(), VELAS_MAINNET_ID);
                if (fromDappBrowser) switchToDappBrowser();
            }

            @Override
            public void cancelAuthentication()
            {
                signCallback = null;
                showErrorDialogCancel(getString(R.string.title_dialog_error), getString(R.string.message_authentication_failed));
                viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_authentication_failed));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        viewModel.getAuthenticationForSignature(wallet, this, signCallback);
    }

    private void killSession()
    {
        terminateSession = true;
        Log.d(TAG, ": Terminate Session: " + getSessionId());
        if (client != null && session != null && client.isConnected())
        {
            client.killSession();
        }
        else
        {
            finish();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        stopMessageCheck();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    private void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(message)
                .setPositiveButton(R.string.try_again, (d, w) -> {
                    onDefaultWallet(wallet);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void showErrorDialogCancel(String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
//                    if (fromPhoneBrowser || fromDappBrowser)
//                    {
//                        finish();
//                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    public void onBackPressed()
    {
        //TODO: If from phone browser then we should return a code that tells main activity to finish
        if (fromDappBrowser)
        {
            switchToDappBrowser();
        }
        else if (fromPhoneBrowser || qrCode == null)
        {
            //hand back to phone browser
            finish();
        }
        else
        {
            endSessionDialog();
        }
    }

    private void endSessionDialog()
    {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        killSession();
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) -> {
                        d.dismiss();
                    })
                    .create();
            dialog.show();
        });
    }

    private void showErrorDialogTerminate(String message)
    {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        finish();
                    })
                    .create();
            dialog.show();
        });
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
            showErrorDialogCancel(peerName.getText().toString(), getString(R.string.message_transaction_not_sent));
            if (requestCode == C.REQUEST_TRANSACTION_CALLBACK)
            {
                viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_authentication_failed));
            }
            else
            {
                viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_reject_request));
            }
        }
    }

    //return from the openConfirmation above
    public void handleTransactionCallback(int resultCode, Intent data)
    {
        signable = null;
        if (data == null) return;
        final Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
        if (resultCode == RESULT_OK && web3Tx != null)
        {
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            viewModel.recordSignTransaction(getApplicationContext(), web3Tx, VELAS_MAINNET_ID, getSessionId());
            viewModel.approveRequest(getSessionId(), lastId, hashData);
        }
        else if (web3Tx != null)
        {
            showErrorDialogCancel(getString(R.string.title_wallet_connect), getString(R.string.message_transaction_not_sent));
            viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_reject_request));
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
