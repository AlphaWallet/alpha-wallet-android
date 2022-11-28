package com.alphawallet.app.ui;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.ActionSheetSource;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.walletconnect.entity.WCUtils;
import com.alphawallet.app.walletconnect.entity.WalletConnectCallback;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheet;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ActionSheetSignDialog;
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

import org.jetbrains.annotations.NotNull;
import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import timber.log.Timber;

@AndroidEntryPoint
public class WalletConnectActivity extends BaseActivity implements ActionSheetCallback, StandardFunctionInterface, WalletConnectCallback
{
    public static final String WC_LOCAL_PREFIX = "wclocal:";
    public static final String WC_INTENT = "wcintent:";
    private static final String TAG = "WCClient";
    private static final String DEFAULT_IDON = "https://example.walletconnect.org/favicon.ico";
    private static final long CONNECT_TIMEOUT = 10 * DateUtils.SECOND_IN_MILLIS; // 10 Seconds timeout
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long switchChainDialogCallbackId = 1;
    private WalletConnectViewModel viewModel;
    private LocalBroadcastManager broadcastManager;
    private WCClient client;
    private WCSession session;
    private WCPeerMeta peerMeta;
    private WCPeerMeta remotePeerMeta;
    private ActionSheet confirmationDialog;
    ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> confirmationDialog.setCurrentGasIndex(result));
    private AddEthereumChainPrompt addEthereumChainPrompt;
    // data for switch chain request
    private long switchChainRequestId;  // rpc request id
    private long switchChainId;         // new chain to switch to
    private String name;                // remote peer name
    private String currentSessionId;    // sessionId for which chain is switched
    private boolean chainAvailable;     // flag denoting chain available in AW or not
    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView statusText;
    private TextView textName;
    private TextView txCount;
    private ChainName chainName;
    private TokenIcon chainIcon;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private LinearLayout txCountLayout;
    private FunctionButtonBar functionBar;
    private boolean fromDappBrowser = false;  //if using this from dappBrowser (which is a bit strange but could happen) then return back to browser once signed
    private boolean fromPhoneBrowser = false; //if from phone browser, clicking 'back' should take user back to dapp running on the phone's browser,
    // -- according to WalletConnect's expected UX docs.
    private boolean fromSessionActivity = false;
    private String qrCode;
    private SignAuthenticationCallback signCallback;
    private long lastId;
    private String signData;
    private WCEthereumSignMessage.WCSignType signType;
    private long chainIdOverride;

    @Inject
    AWWalletConnectClient awWalletConnectClient;

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                chainIdOverride = result.getData().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
                Toast.makeText(this, getText(R.string.hint_network_name) + " " + EthereumNetworkBase.getShortChainName(chainIdOverride), Toast.LENGTH_LONG).show();
                confirmationDialog.updateChain(chainIdOverride);
            });
    private boolean waitForWalletConnectSession = false;
    private long requestId = 0;
    private AWalletAlertDialog dialog = null;
    private final BroadcastReceiver walletConnectActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
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
                    if (wcRequest != null)
                    {
                        executedPendingRequest(wcRequest.id);
                        receiveRequest(wcRequest);
                    }
                    else
                    {
                        // something went wrong
                    }
                    break;
                case C.WALLET_CONNECT_CLIENT_TERMINATE:
                    String sessionId = intent.getStringExtra("sessionid");
                    Timber.tag(TAG).d("MSG: TERMINATE: %s", sessionId);
                    if (viewModel != null)
                    {
                        viewModel.endSession(sessionId);
                    }
                    if (getSessionId() != null && getSessionId().equals(sessionId))
                    {
                        setupClient(sessionId);
                        finish();
                    }
                    break;
                case C.WALLET_CONNECT_SWITCH_CHAIN:
                    Timber.tag(TAG).d("MSG: SWITCH CHAIN: ");
                    onSwitchChainRequest(intent);
                    break;
                case C.WALLET_CONNECT_ADD_CHAIN:
                    Timber.tag(TAG).d("MSG: ADD CHAIN");
                    onAddChainRequest(intent);
                    break;
            }
        }
    };

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

        if (savedInstanceState != null) restoreState(savedInstanceState);
    }

    private void restoreState(Bundle savedInstance)
    {
        //Orientation change?

        if (savedInstance.containsKey("ORIENTATION") && savedInstance.containsKey("SESSIONID"))
        {
            int oldOrientation = savedInstance.getInt("ORIENTATION");
            int newOrientation = getResources().getConfiguration().orientation;

            if (oldOrientation != newOrientation)
            {
                requestId = savedInstance.getLong("SESSIONID");
                String sessionId = savedInstance.getString("SESSIONIDSTR");
                session = viewModel.getSession(sessionId);

                if (savedInstance.containsKey("TRANSACTION"))
                {
                    Web3Transaction w3Tx = savedInstance.getParcelable("TRANSACTION");
                    chainIdOverride = savedInstance.getLong("CHAINID");

                    //kick off transaction
                    final ActionSheetDialog confDialog = generateTransactionRequest(w3Tx, chainIdOverride);
                    if (confDialog != null)
                    {
                        confirmationDialog = confDialog;
                        confirmationDialog.show();
                    }
                }
                else if (savedInstance.containsKey("SIGNDATA"))
                {
                    signData = savedInstance.getString("SIGNDATA");
                    signType = WCEthereumSignMessage.WCSignType.values()[savedInstance.getInt("SIGNTYPE")];
                    lastId = savedInstance.getLong("LASTID");
                    String peerUrl = savedInstance.getString("PEERURL");
                    Signable signable = null;

                    //kick off sign
                    switch (signType)
                    {
                        case MESSAGE:
                        case PERSONAL_MESSAGE:
                            signable = new EthereumMessage(signData, peerUrl, lastId, SignMessageType.SIGN_PERSONAL_MESSAGE);
                            break;
                        case TYPED_MESSAGE:
                            signable = new EthereumTypedMessage(signData, peerUrl, lastId, new CryptoFunctions());
                            break;
                    }

                    doSignMessage(signable);
                }
            }
        }
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
        if (wcCode != null && wcCode.startsWith(WC_LOCAL_PREFIX))
        {
            wcCode = wcCode.replace(WC_LOCAL_PREFIX, "");
            fromDappBrowser = true;
        }
        else if (wcCode != null && wcCode.startsWith(WC_INTENT))
        {
            wcCode = wcCode.replace(WC_INTENT, "");
            fromPhoneBrowser = true; //don't use this yet, but could use it for switching between apps
        }
        this.qrCode = wcCode;
        session = WCSession.Companion.from(qrCode);
        Timber.d("WCClient: %s", qrCode);
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
        txCountLayout = findViewById(R.id.layout_tx_count);
        txCount = findViewById(R.id.tx_count);
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
        if (id == R.string.action_end_session)
        {
            endSessionDialog();
        }
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
                session = viewModel.getSession(sessionId);
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
                            restartSession(session);
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

    private void restartSession(WCSession session)
    {
        String sessionId = session.getTopic();
        client = WCUtils.createWalletConnectSession(this, viewModel.getWallet(),
                session, viewModel.getPeerId(sessionId), viewModel.getRemotePeerId(sessionId));
        viewModel.putClient(this, sessionId, client);
    }

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
            dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
            dialog.setTitle(R.string.watch_wallet);
            dialog.setButton(R.string.action_close, v -> {
                viewModel.rejectRequest(getApplication(), getSessionId(),
                        id, getString(R.string.message_authentication_failed));
                dialog.dismiss();
            });
            dialog.setCancelable(false);
            dialog.show();
            return true;
        }
        else
        {
            return false;
        }
    }

    private void closeErrorDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    private void startMessageCheck()
    {
        IntentFilter filter = new IntentFilter(C.WALLET_CONNECT_REQUEST);
        filter.addAction(C.WALLET_CONNECT_NEW_SESSION);
        filter.addAction(C.WALLET_CONNECT_FAIL);
        filter.addAction(C.WALLET_CONNECT_CLIENT_TERMINATE);
        filter.addAction(C.WALLET_CONNECT_SWITCH_CHAIN);
        filter.addAction(C.WALLET_CONNECT_ADD_CHAIN);
        if (broadcastManager == null) broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(walletConnectActionReceiver, filter);
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
            if (client != null && client.chainIdVal() == 0 && (dialog == null || !dialog.isShowing()))
            {
                //show timeout
                showTimeoutDialog();
            }
        }, CONNECT_TIMEOUT);
    }

    private void invalidSession()
    {
        closeErrorDialog();
        dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.invalid_walletconnect_session);
        dialog.setMessage(R.string.restart_walletconnect_session);
        dialog.setButton(R.string.dialog_ok, v -> {
            dialog.dismiss();
            finish();
        });
        dialog.setCancelable(false);
        dialog.show();

        viewModel.trackError(Analytics.Error.WALLET_CONNECT, getString(R.string.invalid_walletconnect_session));
    }

    private void initWalletConnectPeerMeta()
    {
        peerMeta = new WCPeerMeta(
                getString(R.string.app_name),
                C.ALPHAWALLET_WEB,
                viewModel.getWallet().address,
                new ArrayList<>(Collections.singleton(C.ALPHAWALLET_LOGO_URI))
        );
    }

    private void initWalletConnectClient()
    {
        client = new WCClient();

        client.setOnWCOpen(peerId -> {
            viewModel.putClient(this, getSessionId(), client);
            Timber.tag(TAG).d("On Open: %s", peerId);
            return Unit.INSTANCE;
        });
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle state)
    {
        super.onSaveInstanceState(state);
        //need to preserve the orientation and current signing request
        state.putInt("ORIENTATION", getResources().getConfiguration().orientation);
        state.putLong("SESSIONID", requestId);
        state.putString("SESSIONIDSTR", getSessionId());
        if (confirmationDialog != null && confirmationDialog.isShowing() && confirmationDialog.getTransaction() != null)
        {
            state.putParcelable("TRANSACTION", confirmationDialog.getTransaction());
            state.putLong("CHAINID", viewModel.getChainId(getSessionId()));
        }
        if (confirmationDialog != null && confirmationDialog.isShowing() && signData != null)
        {
            state.putString("SIGNDATA", signData);
            state.putInt("SIGNTYPE", signType.ordinal());
            state.putLong("LASTID", lastId);
            state.putString("PEERURL", peerUrl.getText().toString());
        }
    }

    private void setupClient(final String sessionId)
    {
        viewModel.getClient(this, sessionId, client ->
                runOnUiThread(() -> {
                    if (client == null || !client.isConnected())
                    {
                        statusText.setText(R.string.not_connected);
                        statusText.setTextColor(getColor(R.color.error));
                    }
                    else
                    {
                        statusText.setText(R.string.online);
                        statusText.setTextColor(getColor(R.color.positive));
                    }
                }));
    }

    private void displaySessionStatus(String sessionId)
    {
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        WCPeerMeta remotePeerData = viewModel.getRemotePeer(sessionId);
        this.remotePeerMeta = remotePeerData;   // init meta to access in other places
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
            chainIcon.bindData(viewModel.getChainId(sessionId));
            viewModel.startGasCycle(viewModel.getChainId(sessionId));
            updateSignCount();
        }
    }

    private void onSessionRequest(Long id, WCPeerMeta peer, long chainId)
    {
        if (peer == null)
        {
            finish();
        }

        closeErrorDialog();

        if (confirmationDialog != null)
        {
            if (confirmationDialog.isShowing())
            {      // if already opened
                confirmationDialog.forceDismiss();
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
        txCount.setText(R.string.empty);
        chainName.setChainID(chainIdOverride);
        chainIcon.setVisibility(View.VISIBLE);
        chainIcon.bindData(chainIdOverride);
        remotePeerMeta = peer;

        confirmationDialog = new ActionSheetDialog(this, peer, chainId, displayIcon, this);
        confirmationDialog.show();
        confirmationDialog.fullExpand();

        viewModel.track(Analytics.Action.WALLET_CONNECT_SESSION_REQUEST);
    }

    private void onEthSign(Long id, WCEthereumSignMessage message)
    {
        Signable signable = null;
        lastId = id;
        signData = message.getData();
        signType = message.getType();

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

    private void onFailure(@NonNull Throwable throwable)
    {
        closeErrorDialog();
        dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(throwable.getMessage());
        dialog.setButton(R.string.try_again, v -> onDefaultWallet(viewModel.getWallet()));
        dialog.setSecondaryButton(R.string.action_cancel, v -> {
            dialog.dismiss();
            killSession();
            finish();
        });
        dialog.setCancelable(false);
        dialog.show();

        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ERROR_MESSAGE, throwable.getMessage());
        viewModel.track(Analytics.Action.WALLET_CONNECT_TRANSACTION_FAILED, props);
    }

    private void doSignMessage(final Signable signable)
    {
        confirmationDialog = new ActionSheetSignDialog(this, this, signable);
        confirmationDialog.show();

        viewModel.track(Analytics.Action.WALLET_CONNECT_SIGN_MESSAGE_REQUEST);
    }

    @Override
    public void signingComplete(SignatureFromKey signature, Signable signable)
    {
        viewModel.recordSign(signable, getSessionId(), () -> {
            viewModel.approveRequest(getApplication(), getSessionId(), signable.getCallbackId(), Numeric.toHexString(signature.signature));
            confirmationDialog.success();
            if (fromDappBrowser)
            {
                confirmationDialog.forceDismiss();
                switchToDappBrowser();
            }
            requestId = 0;
            lastId = 0;
            signData = null;
            updateSignCount();
        });
    }

    @Override
    public void signingFailed(Throwable error, Signable message)
    {
        showErrorDialog(error.getMessage());
        confirmationDialog.dismiss();
        if (fromDappBrowser) switchToDappBrowser();
        requestId = 0;
        lastId = 0;
        signData = null;
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction, long chainId)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        final ActionSheetDialog confDialog = generateTransactionRequest(w3Tx, chainId);
        if (confDialog != null)
        {
            confirmationDialog = confDialog;
            confirmationDialog.setSignOnly(); //sign transaction only
            confirmationDialog.show();

            viewModel.track(Analytics.Action.WALLET_CONNECT_SIGN_TRANSACTION_REQUEST);
        }
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction, long chainId)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        final ActionSheetDialog confDialog = generateTransactionRequest(w3Tx, chainId);
        if (confDialog != null)
        {
            confirmationDialog = confDialog;
            confirmationDialog.show();

            viewModel.track(Analytics.Action.WALLET_CONNECT_SEND_TRANSACTION_REQUEST);
        }
    }

    private ActionSheetDialog generateTransactionRequest(Web3Transaction w3Tx, long chainId)
    {
        try
        {
            if ((confirmationDialog == null || !confirmationDialog.isShowing()) &&
                    (w3Tx.recipient.equals(Address.EMPTY) && w3Tx.payload != null) // Constructor
                    || (!w3Tx.recipient.equals(Address.EMPTY) && (w3Tx.payload != null || w3Tx.value != null))) // Raw or Function TX
            {
                WCPeerMeta remotePeerData = viewModel.getRemotePeer(getSessionId());
                Token token = viewModel.getTokensService().getTokenOrBase(chainId, w3Tx.recipient.toString());
                final ActionSheetDialog confDialog = new ActionSheetDialog(this, w3Tx, token, "",
                        w3Tx.recipient.toString(), viewModel.getTokensService(), this);
                confDialog.setURL(remotePeerData.getUrl());
                confDialog.setCanceledOnTouchOutside(false);
                confDialog.waitForEstimate();

                viewModel.calculateGasEstimate(viewModel.getWallet(), w3Tx, chainId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(confDialog::setGasEstimate,
                                Throwable::printStackTrace)
                        .isDisposed();

                return confDialog;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return null;
    }

    private void killSession()
    {
        Timber.tag(TAG).d(": Terminate Session: %s", getSessionId());
        if (client != null && session != null && client.isConnected())
        {
            viewModel.track(Analytics.Action.WALLET_CONNECT_SESSION_ENDED);
            client.killSession();
            viewModel.disconnectSession(this, client.sessionId());
            awWalletConnectClient.updateNotification();
            handler.postDelayed(this::finish, 5000);
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
        broadcastManager.unregisterReceiver(walletConnectActionReceiver);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        viewModel.track(Analytics.Navigation.WALLET_CONNECT_SESSION_DETAIL);
        //see if the session is active
        setupClient(getSessionId());
        startMessageCheck();
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
                dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
                dialog.setTitle(R.string.title_dialog_error);
                dialog.setMessage(R.string.walletconnect_timeout);
                dialog.setButton(R.string.ok, v -> dialog.dismiss());
                dialog.setSecondaryButton(R.string.action_close, v -> {
                    dialog.dismiss();
                    finish();
                });
                dialog.setCancelable(false);
                dialog.show();

                viewModel.track(Analytics.Action.WALLET_CONNECT_CONNECTION_TIMEOUT);
            });
        }
    }

    private void showErrorDialog(String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
                dialog.setTitle(R.string.title_dialog_error);
                dialog.setMessage(message);
                dialog.setButton(R.string.try_again, v -> onDefaultWallet(viewModel.getWallet()));
                dialog.setSecondaryButton(R.string.action_close, v -> {
                    dialog.dismiss();
                    killSession();
                });
                dialog.setCancelable(false);
                dialog.show();

                viewModel.trackError(Analytics.Error.WALLET_CONNECT, message);
            });
        }
    }

    private void showErrorDialogCancel(String title, String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
                dialog.setTitle(title);
                dialog.setMessage(message);
                dialog.setButton(R.string.action_cancel, v -> dialog.dismiss());
                dialog.setCancelable(false);
                dialog.show();

                viewModel.trackError(Analytics.Error.WALLET_CONNECT, message);
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
            dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
            dialog.setTitle(R.string.dialog_title_disconnect_session);
            dialog.setButton(R.string.action_close, v -> {
                dialog.dismiss();
                killSession();
            });
            dialog.setSecondaryButton(R.string.action_cancel, v -> dialog.dismiss());
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    private void showErrorDialogTerminate(String message)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
                dialog.setTitle(R.string.title_dialog_error);
                dialog.setMessage(message);
                dialog.setButton(R.string.dialog_ok, v -> {
                    dialog.dismiss();
                    finish();
                });
                dialog.setCancelable(false);
                dialog.show();

                viewModel.trackError(Analytics.Error.WALLET_CONNECT, message);
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
            if (confirmationDialog != null && confirmationDialog.isShowing())
                confirmationDialog.completeSignRequest(resultCode == RESULT_OK);
        }
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
                updateSignCount();

                viewModel.track(Analytics.Action.WALLET_CONNECT_TRANSACTION_SUCCESS);
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                displayTransactionError(error);
                confirmationDialog.dismiss();
                viewModel.rejectRequest(getApplication(), getSessionId(), lastId, getString(R.string.message_authentication_failed));
                requestId = 0;
            }
        };

        viewModel.sendTransaction(finalTx, viewModel.getChainId(getSessionId()), callback);
    }

    //Transaction failed to be sent
    private void displayTransactionError(final Throwable throwable)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            runOnUiThread(() -> {
                closeErrorDialog();
                dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
                dialog.setTitle(R.string.invalid_walletconnect_session);
                dialog.setTitle(R.string.error_transaction_failed);
                dialog.setMessage(throwable.getMessage());
                dialog.setButtonText(R.string.button_ok);
                dialog.setButtonListener(v -> dialog.dismiss());
                dialog.show();

                AnalyticsProperties props = new AnalyticsProperties();
                props.put(Analytics.PROPS_ERROR_MESSAGE, throwable.getMessage());
                viewModel.track(Analytics.Action.WALLET_CONNECT_TRANSACTION_FAILED, props);
            });
        }
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            viewModel.rejectRequest(this, getSessionId(), callbackId, getString(R.string.message_reject_request));
            viewModel.track(Analytics.Action.WALLET_CONNECT_TRANSACTION_CANCELLED);
        }

        if (fromDappBrowser) switchToDappBrowser();
        requestId = 0;
        confirmationDialog = null;
    }

    @Override
    public void notifyConfirm(String mode)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        props.put(Analytics.PROPS_ACTION_SHEET_SOURCE, ActionSheetSource.WALLET_CONNECT.getValue());
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, props);
    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
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
                updateSignCount();
            }
        };

        viewModel.signTransaction(getBaseContext(), tx, dappFunction, peerUrl.getText().toString(), viewModel.getChainId(getSessionId()), viewModel.defaultWallet().getValue());
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
        chainIcon.bindData(selectedChain);
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        chainIdOverride = selectedChain;
        setupClient(getSessionId()); //should populate this activity
        viewModel.track(Analytics.Action.WALLET_CONNECT_SESSION_APPROVED);
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
        viewModel.track(Analytics.Action.WALLET_CONNECT_SESSION_REJECTED);
        finish();
    }

    @Override
    public void openChainSelection()
    {
        ActionSheetCallback.super.openChainSelection();
        Intent intent = new Intent(WalletConnectActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
        getNetwork.launch(intent);
    }

    private void showSwitchChainDialog()
    {
        try
        {
            Token baseToken = viewModel.getTokenService().getTokenOrBase(switchChainId, viewModel.defaultWallet().getValue().address);
            NetworkInfo newNetwork = EthereumNetworkBase.getNetworkInfo(switchChainId);
            NetworkInfo activeNetwork = EthereumNetworkBase.getNetworkInfo(client.chainIdVal());
            if (confirmationDialog != null && confirmationDialog.isShowing())
                return;
            confirmationDialog = new ActionSheetDialog(this, this, R.string.switch_chain_request, R.string.switch_and_reload,
                    switchChainDialogCallbackId, baseToken, activeNetwork, newNetwork);
            confirmationDialog.setOnDismissListener(dialog -> {
                viewModel.approveSwitchEthChain(WalletConnectActivity.this, switchChainRequestId, currentSessionId, switchChainId, false, chainAvailable);
                confirmationDialog.setOnDismissListener(null);         // remove from here as dialog is multi-purpose
                confirmationDialog = null;
            });
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
            confirmationDialog.fullExpand();

            viewModel.track(Analytics.Action.WALLET_CONNECT_SWITCH_NETWORK_REQUEST);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    private void onSwitchChainRequest(Intent intent)
    {
        name = intent.getStringExtra(C.EXTRA_NAME);
        switchChainRequestId = intent.getLongExtra(C.EXTRA_WC_REQUEST_ID, -1);
        switchChainId = intent.getLongExtra(C.EXTRA_CHAIN_ID, -1);
        currentSessionId = intent.getStringExtra(C.EXTRA_SESSION_ID);
        Timber.tag(TAG).d("MSG: SWITCH CHAIN: name: %s, chainId: %s", name, switchChainId);

        if (currentSessionId == null || !session.getTopic().equals(currentSessionId))
        {
            Timber.tag(TAG).d("Wrong session");
            return;
        }
        if (switchChainId == -1 || requestId == -1)
        {
            Timber.tag(TAG).d("Cant find data");
            return;
        }
        chainAvailable = EthereumNetworkBase.getNetworkInfo(switchChainId) != null;
        // reject with error message as the chain is not added
        if (!chainAvailable)
        {
            viewModel.approveSwitchEthChain(WalletConnectActivity.this, requestId, currentSessionId, switchChainId, false, false);
        }
        else
        {
            showSwitchChainDialog();
        }
    }

    private void onAddChainRequest(Intent intent)
    {
        long requestId = intent.getLongExtra(C.EXTRA_WC_REQUEST_ID, -1);
        String currentSessionId = intent.getStringExtra(C.EXTRA_SESSION_ID);
        WalletAddEthereumChainObject chainObject = intent.getParcelableExtra(C.EXTRA_CHAIN_OBJ);
        if (chainObject != null)
        {
            // showing dialog because chain is not added
            addEthereumChainPrompt = new AddEthereumChainPrompt(
                    this,
                    chainObject,
                    chainObject1 -> {
                        this.addEthereumChainPrompt.setOnDismissListener(null);
                        this.addEthereumChainPrompt.dismiss();
                        viewModel.approveAddEthereumChain(
                                WalletConnectActivity.this,
                                requestId,
                                currentSessionId,
                                chainObject,
                                true
                        );
                        viewModel.updateSession(currentSessionId, chainObject.getChainId());
                        displaySessionStatus(currentSessionId);
                    }
            );

            addEthereumChainPrompt.setOnDismissListener(dialog -> {
                viewModel.approveAddEthereumChain(
                        WalletConnectActivity.this,
                        requestId,
                        currentSessionId,
                        chainObject,
                        false
                );
            });
            addEthereumChainPrompt.show();
        }
        else
        {
            viewModel.approveAddEthereumChain(
                    WalletConnectActivity.this,
                    requestId,
                    currentSessionId,
                    chainObject,
                    false
            );
        }
    }

    @Override
    public void buttonClick(long callbackId, Token baseToken)
    {
        if (callbackId == switchChainDialogCallbackId && confirmationDialog != null)
        {
            confirmationDialog.setOnDismissListener(null);
            confirmationDialog.dismiss();
            viewModel.approveSwitchEthChain(WalletConnectActivity.this, switchChainRequestId, currentSessionId, switchChainId, true, chainAvailable);
            viewModel.updateSession(currentSessionId, switchChainId);
            displaySessionStatus(session.getTopic());
        }
    }

    private void updateSignCount()
    {
        ArrayList<SignRecord> recordList = viewModel.getSignRecords(getSessionId());
        txCount.setText(String.valueOf(recordList.size()));
        if (recordList.size() > 0)
        {
            txCountLayout.setOnClickListener(v -> {
                Intent intent = new Intent(getApplication(), SignDetailActivity.class);
                intent.putParcelableArrayListExtra(C.EXTRA_STATE, recordList);
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent);
            });
        }
    }
}
