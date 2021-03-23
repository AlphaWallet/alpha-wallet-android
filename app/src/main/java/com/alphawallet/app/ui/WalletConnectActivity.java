package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

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
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.viewmodel.WalletConnectViewModelFactory;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS;

public class WalletConnectActivity extends BaseActivity implements ActionSheetCallback, StandardFunctionInterface
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
    private ActionSheetDialog confirmationDialog;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView address;
    private ChainName chainName;
    private TextView signCount;
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
    private int chainIdOverride;

    private boolean startup = false;
    private boolean switchConnection = false;
    private boolean terminateSession = false;
    private boolean sessionStarted = false;
    private boolean waitForWalletConnectSession = false;

    private final Handler handler = new Handler();

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
        super.onNewIntent(intent);
        Bundle data = intent.getExtras();
        chainIdOverride = 0;
        sessionStarted = false;
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
        Log.d(TAG, "Received New Intent: " + newSessionId + " (" + sessionId + ")");
        client = viewModel.getClient(newSessionId);

        if (!viewModel.connectedToService())
        {
            //wait for service to connect
            waitForWalletConnectSession = true;
        }
        else if (client == null || !client.isConnected())
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

    private void handleStartFromWCMessage(String sessionIdFromService)
    {
        sessionStarted = true;
        //is different from current sessionId?
        if (!sessionIdFromService.equals(getSessionId()))
        {
            //restore different session
            session = viewModel.getSession(sessionIdFromService);
            client = viewModel.getClient(sessionIdFromService);
            setClientDisconnect(client);
            qrCode = null;
            fromDappBrowser = false;
        }

        //init UI
        displaySessionStatus(sessionIdFromService);
    }

    private void retrieveQrCode()
    {
        Bundle data = getIntent().getExtras();
        if (data != null)
        {
            String qrCode = data.getString("qrCode");
            String sessionId = data.getString("session");
            chainIdOverride = data.getInt(C.EXTRA_CHAIN_ID, 0);
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
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(WalletConnectViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.serviceReady().observe(this, this::onServiceReady);
    }

    private void initViews()
    {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        address = findViewById(R.id.address);
        chainName = findViewById(R.id.chain_name);
        signCount = findViewById(R.id.tx_count);

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
        address.setText(viewModel.getWallet().address);

        Log.d(TAG, "Open Connection: " + getSessionId());

        String peerId;
        String sessionId = getSessionId();
        String connectionId = viewModel.getRemotePeerId(sessionId);
        if (!TextUtils.isEmpty(viewModel.getWallet().address))
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
                        onEthSignTransaction(rq.id, rq.tx, rq.chainId);
                    });
                    break;
                case SEND_TX:
                    runOnUiThread(() -> {
                        onEthSendTransaction(rq.id, rq.tx, rq.chainId);
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
                        onSessionRequest(rq.id, rq.peer, rq.chainId);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
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

        setClientDisconnect(client);
    }

    public void setClientDisconnect(WCClient thisClient)
    {
        thisClient.setOnDisconnect((code, reason) -> {
            Log.d(TAG, "Terminate session?");
            if (viewModel != null) viewModel.pruneSession(client.sessionId());
            if (terminateSession)
            {
                Log.d(TAG, "WalletConnect terminated from dialog");
                shutDown();
            }
            else if (!sessionStarted)
            {
                runOnUiThread(() -> {
                    showErrorDialog("WalletConnect refused session.");
                });
            }
            else
            {
                //normal disconnect
                Log.d(TAG, "WalletConnect terminated from peer");
                shutDown();
            }
            sessionStarted = false;
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
            chainName.setChainID(viewModel.getChainId(sessionId));
            viewModel.startGasCycle(viewModel.getChainId(sessionId));
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

    private void onSessionRequest(Long id, WCPeerMeta peer, int chainIdHint)
    {
        String[] accounts = {viewModel.getWallet().address};

        Glide.with(this)
                .load(peer.getIcons().get(0))
                .into(icon);
        peerName.setText(peer.getName());
        peerUrl.setText(peer.getUrl());
        signCount.setText(R.string.empty);
        final int chainId = chainIdOverride > 0 ? chainIdOverride : chainIdHint;
        chainName.setChainID(chainId);

        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
        AlertDialog dialog = builder
                .setIcon(icon.getDrawable())
                .setTitle(peer.getName())
                .setMessage(peer.getUrl())
                .setPositiveButton(R.string.dialog_approve, (d, w) -> {
                    sessionStarted = true;
                    client.approveSession(Arrays.asList(accounts), chainId);
                    viewModel.createNewSession(getSessionId(), client.getPeerId(), client.getRemotePeerId(),
                            new Gson().toJson(session), new Gson().toJson(peer), chainId);
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
        Signable signable = null;
        lastId = id;
        switch (message.getType())
        {
            case MESSAGE:
                signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id, SignMessageType.SIGN_MESSAGE);
                break;
            case PERSONAL_MESSAGE:
                signable = new EthereumMessage(message.getData(), peerUrl.getText().toString(), id, SignMessageType.SIGN_PERSONAL_MESSAGE);
                break;
            case TYPED_MESSAGE:
                signable = new EthereumTypedMessage(message.getData(), peerUrl.getText().toString(), id, new CryptoFunctions());
                break;
        }

        doSignMessage(signable);
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction, int chainId)
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

    private void onFailure(Throwable throwable)
    {
        if (!checkValidity())
        {
            killSession();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
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
        dialog.show();
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
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                //store sign
                viewModel.recordSign(signable, getSessionId());
                viewModel.approveRequest(getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                confirmationDialog.success();
                updateSignCount();
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        SignAuthenticationCallback signCallback = new SignAuthenticationCallback()
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
                confirmationDialog.dismiss();
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        if (confirmationDialog == null || !confirmationDialog.isShowing())
        {
            confirmationDialog = new ActionSheetDialog(this, this, signCallback, signable);
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
        }
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction, int chainId)
    {
        lastId = id;
        final Web3Transaction w3Tx = new Web3Transaction(transaction, id);
        confirmationDialog = generateTransactionRequest(w3Tx, chainId);
        if (confirmationDialog != null) confirmationDialog.show();
    }

    private ActionSheetDialog generateTransactionRequest(Web3Transaction w3Tx, int chainId)
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

                viewModel.calculateGasEstimate(viewModel.getWallet(), com.alphawallet.token.tools.Numeric.hexStringToByteArray(w3Tx.payload),
                        chainId, w3Tx.recipient.toString(), new BigDecimal(w3Tx.value))
                        .map(limit -> convertToGasLimit(limit, w3Tx))
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
            e.printStackTrace();
        }

        return confDialog;
    }

    private BigInteger convertToGasLimit(EthEstimateGas estimate, Web3Transaction w3Tx)
    {
        if (!estimate.hasError() && estimate.getAmountUsed().compareTo(BigInteger.ZERO) > 0)
        {
            return estimate.getAmountUsed();
        }
        else if (w3Tx.gasLimit.equals(BigInteger.ZERO))
        {
            return new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS); //cautious gas limit
        }
        else
        {
            return w3Tx.gasLimit;
        }
    }

    private void monitorForShutdown()
    {
        if (client == null || session == null || !client.isConnected())
        {
            finish();
        }
        else
        {
            handler.postDelayed(this::monitorForShutdown, 500);
        }
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
        if (viewModel != null) viewModel.onDestroy();
    }

    private void showErrorDialog(String message)
    {
        if (!checkValidity())
        {
            killSession();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
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
        dialog.show();
    }

    private void showErrorDialogCancel(String title, String message)
    {
        if (!checkValidity())
        {
            killSession();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
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
        else
        {
            //hand back to phone browser
            finish();
        }
    }

    private void shutDown()
    {
        finish();
        handler.postDelayed(this::shutDown, 500);
    }

    private void endSessionDialog()
    {
        if (!checkValidity())
        {
            killSession();
            return;
        }

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
            AlertDialog dialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        killSession();
                        monitorForShutdown();
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) -> {
                        d.dismiss();
                    })
                    .create();
            dialog.show();
        });
    }

    private boolean checkValidity()
    {
        return (WalletConnectActivity.this != null && !WalletConnectActivity.this.isDestroyed() && !WalletConnectActivity.this.isFinishing());
    }

    private void showErrorDialogTerminate(String message)
    {
        if (!checkValidity())
        {
            killSession();
            return;
        }

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectActivity.this);
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
        if (data == null) return;
        final Web3Transaction web3Tx = data.getParcelableExtra(C.EXTRA_WEB3TRANSACTION);
        if (resultCode == RESULT_OK && web3Tx != null)
        {
            String hashData = data.getStringExtra(C.EXTRA_TRANSACTION_DATA);
            viewModel.recordSignTransaction(getApplicationContext(), web3Tx, client.getChainId(), getSessionId());
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
                viewModel.recordSignTransaction(getApplicationContext(), web3Tx, client.getChainId(), getSessionId());
                updateSignCount();
                viewModel.approveRequest(getSessionId(), web3Tx.leafPosition, hashData);
                confirmationDialog.transactionWritten(getString(R.string.dialog_title_sign_transaction));
                if (fromDappBrowser) switchToDappBrowser();
                confirmationDialog.transactionWritten(hashData);
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                confirmationDialog.dismiss();
                viewModel.rejectRequest(getSessionId(), lastId, getString(R.string.message_authentication_failed));
            }
        };

        viewModel.sendTransaction(finalTx, client.getChainId(), callback);
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            viewModel.rejectRequest(getSessionId(), callbackId, getString(R.string.message_reject_request));
        }

        if (fromDappBrowser) switchToDappBrowser();
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
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                viewModel.recordSignTransaction(getApplicationContext(), tx, client.getChainId(), getSessionId());
                updateSignCount();
                viewModel.approveRequest(getSessionId(), message.getCallbackId(), Numeric.toHexString(data));
                confirmationDialog.transactionWritten(getString(R.string.dialog_title_sign_transaction));
                if (fromDappBrowser) switchToDappBrowser();
            }
        };

        viewModel.signTransaction(getBaseContext(), tx, dappFunction, peerUrl.getText().toString(), client.getChainId());
        if (fromDappBrowser) switchToDappBrowser();
    }
}
