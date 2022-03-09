package com.alphawallet.app.ui;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.StyledStringBuilder;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.walletconnect.entity.WalletConnectCallback;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.TokenIcon;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import okhttp3.OkHttpClient;
import timber.log.Timber;

@AndroidEntryPoint
public class WalletConnectActivity extends BaseActivity implements ActionSheetCallback, StandardFunctionInterface, WalletConnectCallback
{
    private static final String TAG = "WCClient";
    private static final String DEFAULT_IDON = "https://example.walletconnect.org/favicon.ico";
    public static final String WC_LOCAL_PREFIX = "wclocal:";
    public static final String WC_INTENT = "wcintent:";

    private static final long CONNECT_TIMEOUT = 10 * DateUtils.SECOND_IN_MILLIS; // 10 Seconds timeout

    WalletConnectViewModel viewModel;

    private WCClient client;
    private WCSession session;
    private WCPeerMeta peerMeta;
    private WCPeerMeta remotePeerMeta;

    private ActionSheetDialog confirmationDialog;
    private ActionSheetDialog walletConnectDialog;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView statusText;
    private TextView textName;
    private ChainName chainName;
    private TokenIcon chainIcon;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;
    private boolean fromDappBrowser = false;  //if using this from dappBrowser (which is a bit strange but could happen) then return back to browser once signed
    private boolean fromPhoneBrowser = false; //if from phone browser, clicking 'back' should take user back to dapp running on the phone's browser,
                                              // -- according to WalletConnect's expected UX docs.
    private boolean fromSessionActivity = false;

    private String qrCode;

    private SignAuthenticationCallback signCallback;
    private long lastId;
    private long chainIdOverride;

    private boolean waitForWalletConnectSession = false;
    private long requestId = 0;
    private AlertDialog errorDialog = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_wallet_connect);

        toolbar();

        setTitle(getString(R.string.title_wallet_connect));

        initViews();

        initViewModel();

        Timber.tag(TAG).d("Starting Activity: %s", getSessionId());

        retrieveQrCode();
        viewModel.prepare();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Bundle data = intent.getExtras();
        chainIdOverride = 0;
        if (data != null)
        {
            //detect new intent from service
            String sessionIdFromService = data.getString("session");
            if (sessionIdFromService != null)
            {
                handleStartFromWCMessage(sessionIdFromService);
            }
            else
            {
                handleStartDirectFromQRScan();
            }
        }
    }

    private void onServiceReady(Boolean b)
    {
        if (waitForWalletConnectSession)
        {
            waitForWalletConnectSession = false;
            handleStartDirectFromQRScan();
        }
    }

    private void handleStartDirectFromQRScan()
    {
        String sessionId = getSessionId();
        retrieveQrCode();
        String newSessionId = getSessionId();
        Timber.tag(TAG).d("Received New Intent: %s (%s)", newSessionId, sessionId);
        viewModel.getClient(this, newSessionId, client -> {
            if (client == null || !client.isConnected())
            {
                //TODO: ERROR!
                showErrorDialogTerminate(getString(R.string.session_terminated));
                infoLayout.setVisibility(View.GONE);
            }
            else
            {
                //setup the screen
                Timber.tag(TAG).d("Resume Connection session: %s", newSessionId);
                setClient(client);
            }
        });
    }

    private void handleStartFromWCMessage(String sessionIdFromService)
    {
        //is different from current sessionId?
        if (!sessionIdFromService.equals(getSessionId()))
        {
            //restore different session
            session = viewModel.getSession(sessionIdFromService);
            viewModel.getClient(this, sessionIdFromService, this::setClient);
            qrCode = null;
            fromDappBrowser = false;
        }
        else
        {
            //init UI
            displaySessionStatus(sessionIdFromService);
        }
    }

    private void setClient(WCClient receivedClient)
    {
        client = receivedClient;
        displaySessionStatus(client.sessionId());
    }

    private void retrieveQrCode()
    {
        Bundle data = getIntent().getExtras();
        if (data != null)
        {
            String qrCode = data.getString("qrCode");
            String sessionId = data.getString("session");
            chainIdOverride = data.getLong(C.EXTRA_CHAIN_ID, 0);
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
        Timber.d("WCClient: " + qrCode);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(WalletConnectViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.serviceReady().observe(this, this::onServiceReady);

        viewModel.startService(this);
    }

    private void initViews()
    {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        statusText = findViewById(R.id.connection_status);
        textName = findViewById(R.id.text_name);
        chainName = findViewById(R.id.chain_name);
        chainIcon = findViewById(R.id.chain_icon);

        progressBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.GONE);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_end_session)));
        functionBar.setVisibility(View.GONE);
    }

    @Override
    public void handleClick(String action, int id)
    {
        endSessionDialog();
    }

    //TODO: Refactor this into elements - this function is unmaintainable
    private void onDefaultWallet(Wallet wallet)
    {
        Timber.tag(TAG).d("Open Connection: %s", getSessionId());

        String peerId;
        String sessionId = getSessionId();
        String connectionId = viewModel.getRemotePeerId(sessionId);
        if (!TextUtils.isEmpty(viewModel.getWallet().address))
        {
            if (connectionId == null && session != null) //new session request
            {
                Timber.tag(TAG).d("New Session: %s", getSessionId());
                //new connection, create a random ID to identify us to the remotePeer.
                peerId = UUID.randomUUID().toString(); //Create a new ID for our side of this session. The remote peer uses this ID to identify us
                connectionId = null; //connectionId is only relevant for resuming a session
            }
            else //existing session, rebuild the session data
            {
                //try to retrieve the session from database
                final String thisConnectionId = connectionId;
                session = viewModel.getSession(sessionId);
                peerId = viewModel.getPeerId(sessionId);
                displaySessionStatus(sessionId);

                viewModel.getClient(this, sessionId, client -> {
                    Timber.tag(TAG).d("Resume Session: %s", getSessionId());

                    if (client == null && fromSessionActivity)
                    {
                        functionBar.setVisibility(View.GONE);
                        return;
                    }
                    else if (client == null || !client.isConnected())
                    {
                        if (client == null || (!fromSessionActivity && session == null))
                        {
                            showErrorDialogTerminate(getString(R.string.session_terminated));
                            infoLayout.setVisibility(View.GONE);
                        }
                        else
                        {
                            //attempt to restart the session; this will allow session 'force' close
                            initWalletConnectPeerMeta();
                            startClient();
                            initWalletConnectSession(peerId, thisConnectionId);
                        }
                    }
                    else
                    {
//                        getPendingRequest();
                        setClient(client);
                    }
                });

                return;
            }

            Timber.tag(TAG).d("connect: peerID %s", peerId);
            Timber.tag(TAG).d("connect: remotePeerID %s", connectionId);

            initWalletConnectPeerMeta();
            initWalletConnectClient();
            initWalletConnectSession(peerId, connectionId);
        }
    }

    private final BroadcastReceiver walletConnectActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.tag(TAG).d("Received message");
            String action = intent.getAction();
            switch (action)
            {
                case C.WALLET_CONNECT_REQUEST:
                case C.WALLET_CONNECT_NEW_SESSION:
                case C.WALLET_CONNECT_FAIL:
                    Timber.tag(TAG).d("MSG: %s", action);
//                    getPendingRequest();
                    WCRequest wcRequest = (WCRequest) intent.getParcelableExtra("wcrequest");
                    if (wcRequest != null) {
                        executedPendingRequest(wcRequest.id);
                        receiveRequest(wcRequest);
                    } else {
                        // something went wrong
                    }
                    break;
                case C.WALLET_CONNECT_CLIENT_TERMINATE:
                    String sessionId = intent.getStringExtra("sessionid");
                    Timber.tag(TAG).d("MSG: TERMINATE: %s", sessionId);
                    if (getSessionId() != null && getSessionId().equals(sessionId))
                    {
                        setupClient(sessionId);
                        finish();
                    }
                    break;
            }
        }
    };

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void executedPendingRequest(long id)
    {
        viewModel.removePendingRequest(this, id);
    }

    @Override
    public boolean receiveRequest(WCRequest rq)
    {
        if (rq != null)
        {
            requestId = rq.id;
            long useChainId = viewModel.getChainId(getSessionId());
            switch (rq.type)
            {
                case MESSAGE:
                    if (watchOnly(rq.id)) return false;
                    runOnUiThread(() -> {
                        onEthSign(rq.id, rq.sign);
                    });
                    break;
                case SIGN_TX:
                    if (watchOnly(rq.id)) return false;
                    runOnUiThread(() -> {
                        onEthSignTransaction(rq.id, rq.tx, useChainId);
                    });
                    break;
                case SEND_TX:
                    if (watchOnly(rq.id)) return false;
                    runOnUiThread(() -> {
                        onEthSendTransaction(rq.id, rq.tx, useChainId);
                    });
                    break;
                case FAILURE:
                    runOnUiThread(() -> {
                        onFailure(rq.throwable);
                    });
                    break;
                case SESSION_REQUEST:
                    Timber.tag(TAG).d("On Request: %s", rq.peer.getName());
                    runOnUiThread(() -> {
                        onSessionRequest(rq.id, rq.peer, rq.chainId);
                    });
                    break;
            }
        }

        return true;
    }

    private boolean watchOnly(long id)
    {
        if (!viewModel.getWallet().canSign())
        {
            closeErrorDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
            errorDialog = builder.setTitle(R.string.title_dialog_error)
                    .setMessage(R.string.watch_wallet)
                    .setPositiveButton(R.string.action_close, (d, w) -> {
                        //send reject signal
                        viewModel.rejectRequest(getApplication(), getSessionId(), id, getString(R.string.message_authentication_failed));
                        d.dismiss();
                    })
                    .setCancelable(false)
                    .create();
            errorDialog.show();
            return true;
        }
        else
        {
            return false;
        }
    }

    private void closeErrorDialog()
    {
        if (errorDialog != null && errorDialog.isShowing())
        {
            errorDialog.dismiss();
        }
    }

    private void startMessageCheck()
    {
        IntentFilter filter = new IntentFilter(C.WALLET_CONNECT_REQUEST);
        filter.addAction(C.WALLET_CONNECT_NEW_SESSION);
        filter.addAction(C.WALLET_CONNECT_FAIL);
        filter.addAction(C.WALLET_CONNECT_CLIENT_TERMINATE);
        registerReceiver(walletConnectActionReceiver, filter);
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

    private void initWalletConnectSession(String peerId, String connectionId)
    {
        if (session == null)
        {
            //error situation!
            invalidSession();
            return;
        }

        Timber.tag(TAG).d("Connect: %s (%s)", getSessionId(), connectionId);
        client.connect(session, peerMeta, peerId, connectionId);

        client.setOnFailure(throwable -> {
            Timber.tag(TAG).d("On Fail: %s", throwable.getMessage());
            showErrorDialog("Error: " + throwable.getMessage());
            return Unit.INSTANCE;
        });

        handler.postDelayed(() -> {
            //Timeout check
            if (client != null && client.chainIdVal() == 0 && (errorDialog == null || !errorDialog.isShowing()))
            {
                //show timeout
                showTimeoutDialog();
            }
        }, CONNECT_TIMEOUT);
    }

    private void invalidSession()
    {
        closeErrorDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
        errorDialog = builder.setTitle(R.string.invalid_walletconnect_session)
                .setMessage(R.string.restart_walletconnect_session)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> finish())
                .setCancelable(false)
                .create();
        errorDialog.show();
    }

    private void initWalletConnectPeerMeta()
    {
        String name = getString(R.string.app_name);
        String url = "https://www.alphawallet.com";
        String description = viewModel.getWallet().address;
        String[] icons = {C.ALPHAWALLET_LOGO_URI};

        peerMeta = new WCPeerMeta(
                name,
                url,
                description,
                Arrays.asList(icons)
        );
    }

    private void initWalletConnectClient()
    {
        client = startClient();

        client.setOnWCOpen(peerId -> {
            viewModel.putClient(this, getSessionId(), client);
            Timber.tag(TAG).d("On Open: %s", peerId);
            return Unit.INSTANCE;
        });
    }

    private WCClient startClient()
    {
        return new WCClient(new GsonBuilder(),
                new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .pingInterval(10000, TimeUnit.MILLISECONDS)
                        .retryOnConnectionFailure(true)
                        .build());
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle state)
    {
        super.onSaveInstanceState(state);
        //need to preserve the orientation and current signing request
        state.putInt("ORIENTATION", getResources().getConfiguration().orientation);
        state.putLong("SESSIONID", requestId);
        if (confirmationDialog != null) confirmationDialog.closingActionSheet();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //see if the session is active
        setupClient(getSessionId());
        startMessageCheck();
    }

    private void setupClient(final String sessionId)
    {
        viewModel.getClient(this, sessionId, client ->
                runOnUiThread(() -> {
            if (client == null || !client.isConnected())
            {
                statusText.setText(R.string.not_connected);
                statusText.setTextColor(getColor(R.color.cancel_red));
            }
            else
            {
                statusText.setText(R.string.online);
                statusText.setTextColor(getColor(R.color.nasty_green));
            }
        }));
    }

    private void displaySessionStatus(String sessionId)
    {
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        WCPeerMeta remotePeerData = viewModel.getRemotePeer(sessionId);

        if (remotePeerData != null)
        {
            if (remotePeerData.getIcons().isEmpty())
            {
                icon.setImageResource(R.drawable.ic_coin_eth_small);
            }
            else
            {
                Glide.with(this)
                        .load(remotePeerData.getIcons().get(0))
                        .circleCrop()
                        .into(icon);
            }
            peerName.setText(remotePeerData.getName());
            textName.setText(remotePeerData.getName());
            peerUrl.setText(remotePeerData.getUrl());
            chainName.setChainID(viewModel.getChainId(sessionId));
            chainIcon.setVisibility(View.VISIBLE);
            chainIcon.bindData(viewModel.getTokensService().getServiceToken(viewModel.getChainId(sessionId)));
            viewModel.startGasCycle(viewModel.getChainId(sessionId));
        }
    }

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                chainIdOverride = result.getData().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
                Toast.makeText(this, getText(R.string.hint_network_name) + " " + EthereumNetworkBase.getShortChainName(chainIdOverride), Toast.LENGTH_LONG).show();
                walletConnectDialog.updateChain(chainIdOverride);
            });

    private void onSessionRequest(Long id, WCPeerMeta peer, long chainId)
    {
        if (peer == null) { finish(); }

        closeErrorDialog();

        if (walletConnectDialog != null) {
            if (walletConnectDialog.isShowing()) {      // if already opened
                walletConnectDialog.forceDismiss();
            }
        }

        String[] accounts = {viewModel.getWallet().address};
        String displayIcon = (peer.getIcons().size() > 0) ? peer.getIcons().get(0) : DEFAULT_IDON;

        chainIdOverride = chainIdOverride > 0 ? chainIdOverride : (chainId > 0 ? chainId : MAINNET_ID);

        Glide.with(this)
                .load(displayIcon)
                .circleCrop()
                .into(icon);
        peerName.setText(peer.getName());
        textName.setText(peer.getName());
        peerUrl.setText(peer.getUrl());
        chainName.setChainID(chainIdOverride);
        chainIcon.setVisibility(View.VISIBLE);
        chainIcon.bindData(viewModel.getTokensService().getServiceToken(chainIdOverride));
        remotePeerMeta = peer;

        walletConnectDialog = new ActionSheetDialog(this, peer, chainId, displayIcon, this);
        walletConnectDialog.show();
        walletConnectDialog.fullExpand();
    }

    private void onEthSign(Long id, WCEthereumSignMessage message)
    {
        Signable signable = null;
        lastId = id;
        switch (message.getType())
        {
            case MESSAGE:
                // see https://docs.walletconnect.org/json-rpc-api-methods/ethereum
                // WalletConnect doesn't provide access to deprecated eth_sign
                // Instead it uses sign_personal for both
                // signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id, SignMessageType.SIGN_MESSAGE);
                // break;
                // Drop through
            case PERSONAL_MESSAGE:
                signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id, SignMessageType.SIGN_PERSONAL_MESSAGE);
                break;
            case TYPED_MESSAGE:
                signable = new EthereumTypedMessage(message.getData(), peerUrl.getText().toString(), id, new CryptoFunctions());
                break;
        }

        doSignMessage(signable);
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction, long chainId)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        confirmationDialog = generateTransactionRequest(w3Tx, chainId);
        if (confirmationDialog != null)
        {
            confirmationDialog.setSignOnly(); //sign transaction only
            confirmationDialog.show();
        }
    }

    private void onFailure(@NonNull Throwable throwable)
    {
        closeErrorDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
        errorDialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(throwable.getMessage())
                .setPositiveButton(R.string.try_again, (d, w) -> {
                    onDefaultWallet(viewModel.getWallet());
                })
                .setNeutralButton(R.string.action_cancel, (d, w) -> {
                    killSession();
                    finish();
                })
                .setCancelable(false)
                .create();
        errorDialog.show();
    }

    private void doSignMessage(final Signable signable)
    {
        final DAppFunction dappFunction = new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Signable message)
            {
                showErrorDialog(error.getMessage());
                confirmationDialog.dismiss();
                if (fromDappBrowser) switchToDappBrowser();
                requestId = 0;
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                //store sign
                viewModel.recordSign(signable, getSessionId(), () -> {
                    viewModel.approveRequest(getApplication(), getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                    confirmationDialog.success();
                    if (fromDappBrowser)
                    {
                        confirmationDialog.forceDismiss();
                        switchToDappBrowser();
                    }
                    requestId = 0;
                });
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
            public void gotAuthorisationForSigning(boolean gotAuth, Signable messageToSign)
            {
                if (gotAuth)
                {
                    viewModel.signMessage(
                            signable,
                            dappFunction);
                }
                else
                {
                    cancelAuthentication();
                }
            }

            @Override
            public void cancelAuthentication()
            {
                requestId = 0;
                showErrorDialogCancel(getString(R.string.title_dialog_error), getString(R.string.message_authentication_failed));
                viewModel.rejectRequest(getApplication(), getSessionId(), lastId, getString(R.string.message_authentication_failed));
                confirmationDialog.dismiss();
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        confirmationDialog = new ActionSheetDialog(this, this, signCallback, signable);
        confirmationDialog.setCanceledOnTouchOutside(false);
        confirmationDialog.show();
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction, long chainId)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        confirmationDialog = generateTransactionRequest(w3Tx, chainId);
        if (confirmationDialog != null) confirmationDialog.show();
    }

    private ActionSheetDialog generateTransactionRequest(Web3Transaction w3Tx, long chainId)
    {
        ActionSheetDialog confDialog = null;
        try
        {
            //minimum for transaction to be valid: recipient and value or payload
            if ((confirmationDialog == null || !confirmationDialog.isShowing()) &&
                    (w3Tx.recipient.equals(Address.EMPTY) && w3Tx.payload != null) // Constructor
                    || (!w3Tx.recipient.equals(Address.EMPTY) && (w3Tx.payload != null || w3Tx.value != null))) // Raw or Function TX
            {
                WCPeerMeta remotePeerData = viewModel.getRemotePeer(getSessionId());
                Token token = viewModel.getTokensService().getTokenOrBase(chainId, w3Tx.recipient.toString());
                confDialog = new ActionSheetDialog(this, w3Tx, token, "",
                        w3Tx.recipient.toString(), viewModel.getTokensService(), this);
                confDialog.setURL(remotePeerData.getUrl());
                confDialog.setCanceledOnTouchOutside(false);
                confDialog.waitForEstimate();

                viewModel.calculateGasEstimate(viewModel.getWallet(), Numeric.hexStringToByteArray(w3Tx.payload),
                        chainId, w3Tx.recipient.toString(), new BigDecimal(w3Tx.value), w3Tx.gasLimit)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(estimate -> confirmationDialog.setGasEstimate(estimate),
                                Throwable::printStackTrace)
                        .isDisposed();
            }
        }
        catch (Exception e)
        {
            confDialog = null;
            Timber.e(e);
        }

        return confDialog;
    }

    private void killSession()
    {
        Timber.tag(TAG).d(": Terminate Session: %s", getSessionId());
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
        unregisterReceiver(walletConnectActionReceiver);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (viewModel != null) viewModel.onDestroy();
    }

    private void showTimeoutDialog()
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
                errorDialog = builder.setTitle(R.string.title_dialog_error)
                        .setMessage(R.string.walletconnect_timeout)
                        .setPositiveButton(R.string.ok, (d, w) -> {
                            d.dismiss();
                        })
                        .setNegativeButton(R.string.action_close, (d, w) -> {
                            d.dismiss();
                            killSession();
                        })
                        .setCancelable(false)
                        .create();
                errorDialog.show();
            });
        }
    }

    private void showErrorDialog(String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
                errorDialog = builder.setTitle(R.string.title_dialog_error)
                        .setMessage(message)
                        .setPositiveButton(R.string.try_again, (d, w) -> {
                            onDefaultWallet(viewModel.getWallet());
                        })
                        .setNeutralButton(R.string.action_cancel, (d, w) -> {
                            d.dismiss();
                        })
                        .setNegativeButton(R.string.action_close, (d, w) -> {
                            d.dismiss();
                            killSession();
                        })
                        .setCancelable(false)
                        .create();
                errorDialog.show();
            });
        }
    }

    private void showErrorDialogCancel(String title, String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
                errorDialog = builder.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.action_cancel, (d, w) -> {
                            d.dismiss();
                        })
                        .setCancelable(false)
                        .create();
                errorDialog.show();
            });
        }
    }

    @Override
    public void onBackPressed()
    {
        //TODO: If from phone browser then we should return a code that tells main activity to finish
        if (fromDappBrowser)
        {
            switchToDappBrowser();
        }
        else
        {
            //hand back to phone browser
            finish();
        }
    }

    @Override
    public void finish()
    {
        super.finish();
    }

    private void endSessionDialog()
    {
        runOnUiThread(() -> {
            closeErrorDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
            errorDialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        killSession();
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) -> {
                        d.dismiss();
                    })
                    .create();
            errorDialog.show();
        });
    }

    private void showErrorDialogTerminate(String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
                errorDialog = builder.setTitle(R.string.title_dialog_error)
                        .setMessage(message)
                        .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                            finish();
                        })
                        .create();
                errorDialog.show();
            });
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

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            if (confirmationDialog != null && confirmationDialog.isShowing()) confirmationDialog.completeSignRequest(resultCode == RESULT_OK);
        }
        if (resultCode == RESULT_OK)
        {
            if (requestCode == C.REQUEST_TRANSACTION_CALLBACK)
            {
                handleTransactionCallback(resultCode, data);
            }
            else if (requestCode == C.SET_GAS_SETTINGS)
            {
                //will either be an index, or if using custom then it will contain a price and limit
                if (data != null && confirmationDialog != null)
                {
                    int gasSelectionIndex = data.getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
                    long customNonce = data.getLongExtra(C.EXTRA_NONCE, -1);
                    BigDecimal customGasPrice = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_PRICE));
                    BigDecimal customGasLimit = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_LIMIT));
                    long expectedTxTime = data.getLongExtra(C.EXTRA_AMOUNT, 0);
                    confirmationDialog.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
                }
            }
            else if (signCallback != null) signCallback.gotAuthorisation(true);
        }
        else
        {
            showErrorDialogCancel(peerName.getText().toString(), getString(R.string.message_transaction_not_sent));
            if (requestCode == C.REQUEST_TRANSACTION_CALLBACK)
            {
                viewModel.rejectRequest(this, getSessionId(), lastId, getString(R.string.message_authentication_failed));
            }
            else
            {
                viewModel.rejectRequest(this, getSessionId(), lastId, getString(R.string.message_reject_request));
            }
        }
    }

    //return from the openConfirmation above
    public void handleTransactionCallback(int resultCode, Intent data)
    {
        if (data == null) return;
        final Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
        if (resultCode == RESULT_OK && web3Tx != null)
        {
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            viewModel.recordSignTransaction(getApplicationContext(), web3Tx, String.valueOf(viewModel.getChainId(getSessionId())), getSessionId());
            viewModel.approveRequest(this, getSessionId(), lastId, hashData);
        }
        else if (web3Tx != null)
        {
            showErrorDialogCancel(getString(R.string.title_wallet_connect), getString(R.string.message_transaction_not_sent));
            viewModel.rejectRequest(this, getSessionId(), lastId, getString(R.string.message_reject_request));
        }

        if (fromDappBrowser)
        {
            if (confirmationDialog != null) confirmationDialog.forceDismiss();
            switchToDappBrowser();
        }
    }

    private void switchToDappBrowser()
    {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthenticationForSignature(viewModel.getWallet(), this, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        final SendTransactionInterface callback = new SendTransactionInterface()
        {
            @Override
            public void transactionSuccess(Web3Transaction web3Tx, String hashData)
            {
                viewModel.recordSignTransaction(getApplicationContext(), web3Tx, String.valueOf(viewModel.getChainId(getSessionId())), getSessionId());
                viewModel.approveRequest(getApplication(), getSessionId(), web3Tx.leafPosition, hashData);
                confirmationDialog.transactionWritten(getString(R.string.dialog_title_sign_transaction));
                if (fromDappBrowser) switchToDappBrowser();
                confirmationDialog.transactionWritten(hashData);
                requestId = 0;
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                confirmationDialog.dismiss();
                viewModel.rejectRequest(getApplication(), getSessionId(), lastId, getString(R.string.message_authentication_failed));
                requestId = 0;
            }
        };

        viewModel.sendTransaction(finalTx, viewModel.getChainId(getSessionId()), callback);
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            viewModel.rejectRequest(this, getSessionId(), callbackId, getString(R.string.message_reject_request));
        }

        if (fromDappBrowser) switchToDappBrowser();
        requestId = 0;
    }

    @Override
    public void notifyConfirm(String mode)
    {
        viewModel.actionSheetConfirm(mode);
    }

    @Override
    public void signTransaction(Web3Transaction tx)
    {
        DAppFunction dappFunction = new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Signable message)
            {
                showErrorDialog(error.getMessage());
                confirmationDialog.dismiss();
                if (fromDappBrowser) switchToDappBrowser();
                requestId = 0;
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                viewModel.recordSignTransaction(getApplicationContext(), tx, String.valueOf(viewModel.getChainId(getSessionId())), getSessionId());
                viewModel.approveRequest(getApplication(), getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                confirmationDialog.transactionWritten(getString(R.string.dialog_title_sign_transaction));
                if (fromDappBrowser) switchToDappBrowser();
                requestId = 0;
            }
        };

        viewModel.signTransaction(getBaseContext(), tx, dappFunction, peerUrl.getText().toString(), viewModel.getChainId(getSessionId()));
        if (fromDappBrowser) switchToDappBrowser();
    }

    @Override
    public void notifyWalletConnectApproval(long selectedChain)
    {
        client.approveSession(Collections.singletonList(viewModel.getWallet().address), selectedChain);
        //update client in service
        viewModel.putClient(WalletConnectActivity.this, getSessionId(), client);
        viewModel.createNewSession(getSessionId(), client.getPeerId(), client.getRemotePeerId(),
                new Gson().toJson(session), new Gson().toJson(remotePeerMeta), selectedChain);
        chainName.setChainID(selectedChain);
        chainIcon.setVisibility(View.VISIBLE);
        chainIcon.bindData(viewModel.getTokensService().getServiceToken(selectedChain));
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        chainIdOverride = selectedChain;
        setupClient(getSessionId()); //should populate this activity
        if (fromDappBrowser)
        {
            //switch back to dappBrowser
            switchToDappBrowser();
        }
    }

    @Override
    public void denyWalletConnect()
    {
        client.rejectSession(getString(R.string.message_reject_request));
        finish();
    }

    @Override
    public void openChainSelection() {
        ActionSheetCallback.super.openChainSelection();
        Intent intent = new Intent(WalletConnectActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
        getNetwork.launch(intent);
    }
}
