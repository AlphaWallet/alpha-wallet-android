package com.alphawallet.app.walletconnect;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.alphawallet.hardware.SignatureReturnType.SIGNATURE_GENERATED;
import static com.walletconnect.web3.wallet.client.Wallet.Model;
import static com.walletconnect.web3.wallet.client.Wallet.Params;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.App;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.WalletConnectV2Service;
import com.alphawallet.app.ui.WalletConnectSessionActivity;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.walletconnect.util.WCMethodChecker;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheet;
import com.alphawallet.app.widget.ActionSheetSignDialog;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.walletconnect.android.Core;
import com.walletconnect.android.CoreClient;
import com.walletconnect.android.cacao.signature.SignatureType;
import com.walletconnect.android.relay.ConnectionType;
import com.walletconnect.android.relay.NetworkClientTimeout;
import com.walletconnect.web3.wallet.client.Wallet;
import com.walletconnect.web3.wallet.client.Wallet.Model.Session;
import com.walletconnect.web3.wallet.client.Web3Wallet;

import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import kotlin.Unit;
import timber.log.Timber;

public class AWWalletConnectClient implements Web3Wallet.WalletDelegate
{
    private static final String TAG = AWWalletConnectClient.class.getName();
    private static final String ISS_DID_PREFIX = "did:pkh:";
    private final WalletConnectInteract walletConnectInteract;
    public static Model.SessionProposal sessionProposal;

    private final Context context;
    private final MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData = new MutableLiveData<>(Collections.emptyList());
    private final KeyProvider keyProvider = KeyProviderFactory.get();
    private final LongSparseArray<WalletConnectV2SessionRequestHandler> requestHandlers = new LongSparseArray<>();
    private final GasService gasService;
    private ActionSheetCallback actionSheetCallback;
    private boolean hasConnection;
    private Application application;
    private final PreferenceRepositoryType preferenceRepository;

    public AWWalletConnectClient(Context context, WalletConnectInteract walletConnectInteract, PreferenceRepositoryType preferenceRepository, GasService gasService)
    {
        this.context = context;
        this.walletConnectInteract = walletConnectInteract;
        this.preferenceRepository = preferenceRepository;
        this.gasService = gasService;
        hasConnection = false;
    }

    public void onSessionDelete(@NonNull Model.SessionDelete deletedSession)
    {
        updateNotification(null);
    }

    public boolean hasWalletConnectSessions()
    {
        return !walletConnectInteract.getSessions().isEmpty();
    }

    private boolean validChainId(List<String> chains)
    {
        for (String chainId : chains)
        {
            try
            {
                Long.parseLong(chainId.split(":")[1]);
            }
            catch (Exception e)
            {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, String.format(context.getString(R.string.chain_not_support), chainId), Toast.LENGTH_SHORT).show());
                return false;
            }
        }
        return true;
    }

    private Session getSession(String topic)
    {
        List<Session> listOfSettledSessions;

        try
        {
            listOfSettledSessions = Web3Wallet.INSTANCE.getListOfActiveSessions();
        }
        catch (IllegalStateException e)
        {
            listOfSettledSessions = Collections.emptyList();
            Timber.tag(TAG).e(e);
        }

        for (Session session : listOfSettledSessions)
        {
            if (session.getTopic().equals(topic))
            {
                return session;
            }
        }
        return null;
    }

    public void pair(String url, Consumer<String> callback)
    {
        Core.Params.Pair pair = new Core.Params.Pair(url);
        CoreClient.INSTANCE.getPairing().pair(pair, p -> null, error -> {
            Timber.e(error.getThrowable());
            callback.accept(error.getThrowable().getMessage());
            return null;
        });
    }

    public void approve(Model.SessionRequest sessionRequest, String result)
    {
        Model.JsonRpcResponse jsonRpcResponse = new Model.JsonRpcResponse.JsonRpcResult(sessionRequest.getRequest().getId(), result);
        Params.SessionRequestResponse response = new Params.SessionRequestResponse(sessionRequest.getTopic(), jsonRpcResponse);
        Web3Wallet.INSTANCE.respondSessionRequest(response, srr -> null, this::onSessionRequestApproveError);
    }

    private Unit onSessionRequestApproveError(Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
        return null;
    }

    public void reject(Model.SessionRequest sessionRequest)
    {
        reject(sessionRequest, context.getString(R.string.message_reject_request));
    }

    public void approve(Model.SessionProposal sessionProposal, List<String> selectedAccounts, WalletConnectV2Callback callback)
    {
        String proposerPublicKey = sessionProposal.getProposerPublicKey();
        Params.SessionApprove approve = new Params.SessionApprove(proposerPublicKey, buildNamespaces(sessionProposal, selectedAccounts), sessionProposal.getRelayProtocol());
        Web3Wallet.INSTANCE.approveSession(approve, sessionApprove -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updateNotification(sessionProposal);
                callback.onSessionProposalApproved();
            }, 500);
            return null;
        }, this::onSessionApproveError);
    }

    private Map<String, Model.Namespace.Session> buildNamespaces(Model.SessionProposal sessionProposal, List<String> selectedAccounts)
    {
        Map<String, Model.Namespace.Session> supportedNamespaces = Collections.singletonMap("eip155", new Model.Namespace.Session(
                getSupportedChains(),
                toCAIP10(getSupportedChains(), selectedAccounts),
                getSupportedMethods(),
                getSupportedEvents()));
        try
        {
            return Web3Wallet.INSTANCE.generateApprovedNamespaces(sessionProposal, supportedNamespaces);
        }
        catch (Exception e)
        {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            Timber.tag(TAG).e(e);
        }
        return new HashMap<>();
    }

    private List<String> getSupportedMethods()
    {
        return Arrays.asList("eth_sendTransaction", "eth_signTransaction", "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4", "personal_sign", "eth_sign");
    }

    private List<String> getSupportedEvents()
    {
        return Arrays.asList("chainChanged", "accountsChanged");
    }

    private List<String> getSupportedChains()
    {
        return EthereumNetworkBase.getAllNetworks().stream().map(chainId -> "eip155:" + String.valueOf(chainId)).collect(Collectors.toList());
    }

    private List<String> toCAIP10(List<String> chains, List<String> selectedAccounts)
    {
        List<String> result = new ArrayList<>();
        for (String chain : chains)
        {
            for (String account : selectedAccounts)
            {
                result.add(formatCAIP10(chain, account));
            }
        }
        return result;
    }

    @NonNull
    private String formatCAIP10(String chain, String account)
    {
        return chain + ":" + account;
    }

    private Unit onSessionApproveError(Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
        Toast.makeText(context, error.getThrowable().getLocalizedMessage(), Toast.LENGTH_LONG).show();
        return null;
    }

    public MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData()
    {
        return sessionItemMutableLiveData;
    }

    public void updateNotification(Model.SessionProposal sessionProposal)
    {
        walletConnectInteract.fetchSessions(items -> {
            if (sessionProposal != null && items.isEmpty())
            {
                items.add(WalletConnectV2SessionItem.from(sessionProposal));
            }

            updateService(items);
            sessionItemMutableLiveData.postValue(items);
        });
    }

    private void updateService(List<WalletConnectSessionItem> items)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                if (items.isEmpty())
                {
                    context.stopService(new Intent(context, WalletConnectV2Service.class));
                    //now signal
                }
                else
                {
                    Intent serviceIntent = new Intent(context, WalletConnectV2Service.class);
                    ContextCompat.startForegroundService(context, serviceIntent);
                }
            }
        }
        catch (Exception e)
        {
            //Unable to update
            Timber.e(e);
        }
    }


    public void reject(Model.SessionProposal sessionProposal, WalletConnectV2Callback callback)
    {
        Web3Wallet.INSTANCE.rejectSession(
                new Params.SessionReject(sessionProposal.getProposerPublicKey(), context.getString(R.string.message_reject_request)),
                sessionReject -> null,
                this::onSessionRejectError);
        callback.onSessionProposalRejected();
    }

    private Unit onSessionRejectError(Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
        return null;
    }

    public void disconnect(String sessionId, WalletConnectV2Callback callback)
    {
        Web3Wallet.INSTANCE.disconnectSession(new Params.SessionDisconnect(sessionId), sd -> null, this::onDisconnectError);
        callback.onSessionDisconnected();
    }

    private Unit onDisconnectError(Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
        return null;
    }

    public void reject(Model.SessionRequest sessionRequest, String failMessage)
    {
        Model.JsonRpcResponse.JsonRpcError jsonRpcResponse = new Model.JsonRpcResponse.JsonRpcError(sessionRequest.getRequest().getId(), 0, failMessage);
        Params.SessionRequestResponse response = new Params.SessionRequestResponse(sessionRequest.getTopic(), jsonRpcResponse);
        Web3Wallet.INSTANCE.respondSessionRequest(response, srr -> null, this::onSessionRequestRejectError);
    }

    private Unit onSessionRequestRejectError(Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
        return null;
    }

    public void setCallback(ActionSheetCallback actionSheetCallback)
    {
        this.actionSheetCallback = actionSheetCallback;
    }

    public String getRelayServer()
    {
        return String.format("%s/?projectId=%s", C.WALLET_CONNECT_REACT_APP_RELAY_URL, keyProvider.getWalletConnectProjectId());
    }

    public void init(Application application)
    {
        if (keyProvider.getWalletConnectProjectId().isEmpty())
        {
            //Early return for no wallet connect
            return;
        }

        this.application = application;
        Core.Model.AppMetaData appMetaData = getAppMetaData(application);
        String relayServer = String.format("%s/?projectId=%s", C.WALLET_CONNECT_REACT_APP_RELAY_URL, keyProvider.getWalletConnectProjectId());
        CoreClient coreClient = CoreClient.INSTANCE;
        coreClient.initialize(appMetaData, relayServer, ConnectionType.AUTOMATIC, application, null, null, new NetworkClientTimeout(30, TimeUnit.SECONDS), error -> {
            Timber.w(error.throwable);
            return null;
        });

        Web3Wallet.INSTANCE.initialize(new Params.Init(coreClient), () -> {
            Timber.tag(TAG).i("Wallet Connect init success");
            return null;
        }, e ->
        {
            Timber.tag(TAG).e("Init failed: %s", e.getThrowable().getMessage());
            return null;
        });

        try
        {
            Web3Wallet.INSTANCE.setWalletDelegate(this);
            //ensure notification is displayed if session is active
            updateNotification(null);
        }
        catch (Exception e)
        {
            Timber.tag(TAG).e(e);
        }
    }

    @NonNull
    public Core.Model.AppMetaData getAppMetaData(Application application)
    {
        String name = application.getString(R.string.app_name);
        String url = C.ALPHAWALLET_WEBSITE;
        String[] icons = {C.ALPHA_WALLET_LOGO_URL};
        String description = "The ultimate Web3 Wallet to power your tokens.";
        String redirect = "kotlin-responder-wc:/request";
        return new Core.Model.AppMetaData(name, description, url, Arrays.asList(icons), redirect, null);
    }

    public void shutdown()
    {
        Timber.tag(TAG).i("shutdown");
    }

    public void onConnectionStateChange(@NonNull Model.ConnectionState connectionState)
    {
        Timber.tag(TAG).i("onConnectionStateChange");
        hasConnection = connectionState.isAvailable();
    }

    public void signComplete(SignatureFromKey signatureFromKey, Signable signable)
    {
        if (hasConnection)
        {
            onSign(signatureFromKey, getHandler(signable.getCallbackId())); //have valid connection, can send response
        }
        else
        {
            new Handler().postDelayed(() -> signComplete(signatureFromKey, signable), 1000); //Delay by 1 second and check again
        }
    }

    public void signFail(String error, Signable signable)
    {
        final WalletConnectV2SessionRequestHandler requestHandler = getHandler(signable.getCallbackId());

        Timber.i("sign fail: %s", error);
        reject(requestHandler.getSessionRequest(), error);
    }

    //Sign Dialog (and later tx dialog) was dismissed
    public void dismissed(long callbackId)
    {
        final WalletConnectV2SessionRequestHandler requestHandler = getHandler(callbackId);
        if (requestHandler != null)
        {
            reject(requestHandler.getSessionRequest(), application.getString(R.string.message_reject_request));
        }
    }

    private WalletConnectV2SessionRequestHandler getHandler(long callbackId)
    {
        WalletConnectV2SessionRequestHandler handler = requestHandlers.get(callbackId);
        requestHandlers.remove(callbackId);
        return handler;
    }

    private void onSign(SignatureFromKey signatureFromKey, WalletConnectV2SessionRequestHandler requestHandler)
    {
        if (signatureFromKey.sigType == SIGNATURE_GENERATED)
        {
            String result = Numeric.toHexString(signatureFromKey.signature);
            approve(requestHandler.getSessionRequest(), result);
        }
        else
        {
            Timber.i("sign fail: %s", signatureFromKey.failMessage);
            reject(requestHandler.getSessionRequest(), signatureFromKey.failMessage);
        }
    }

    private void showApprovalDialog(Model.AuthRequest authRequest)
    {
        String activeWallet = preferenceRepository.getCurrentWalletAddress();
        String issuer = ISS_DID_PREFIX + formatCAIP10(authRequest.payloadParams.chainId, activeWallet);
        String message = Web3Wallet.INSTANCE.formatMessage(new Params.FormatMessage(authRequest.payloadParams, issuer));
        String origin = authRequest.payloadParams.domain;

        new Handler(Looper.getMainLooper()).post(() -> doShowApprovalDialog(activeWallet, message, authRequest.getId(), origin, issuer));
    }

    private void doShowApprovalDialog(String walletAddress, String message, long requestId, String origin, String issuer)
    {
        EthereumMessage ethereumMessage = new EthereumMessage(message, origin, 0,
                SignMessageType.SIGN_MESSAGE);
        Activity topActivity = App.getInstance().getTopActivity();
        if (topActivity != null)
        {
            ActionSheet actionSheet = new ActionSheetSignDialog(topActivity, newActionSheetCallback(requestId, issuer), ethereumMessage);
            actionSheet.setSigningWallet(walletAddress);
            actionSheet.show();
        }
    }

    private ActionSheetCallback newActionSheetCallback(long requestId, String issuer)
    {
        return new ActionSheetCallback()
        {
            @Override
            public void getAuthorisation(SignAuthenticationCallback callback)
            {
            }

            @Override
            public void sendTransaction(Web3Transaction tx)
            {
            }

            @Override
            public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
            {
            }

            @Override
            public GasService getGasService()
            {
                return gasService;
            }

            @Override
            public void dismissed(String txHash, long callbackId, boolean actionCompleted)
            {
                if (actionCompleted)
                {
                    closeWalletConnectActivity();
                }
                else
                {
                    Web3Wallet.INSTANCE.respondAuthRequest(new Params.AuthRequestResponse.Error(requestId, 0, "User rejected request."), (authRequestResponse) -> {
                        closeWalletConnectActivity();
                        return null;
                    }, (error) -> {
                        closeWalletConnectActivity();
                        return null;
                    });
                }
            }

            @Override
            public void notifyConfirm(String mode)
            {

            }

            @Override
            public ActivityResultLauncher<Intent> gasSelectLauncher()
            {
                return null;
            }

            @Override
            public WalletType getWalletType()
            {
                return null;
            }

            @Override
            public void signingComplete(SignatureFromKey signature, Signable message)
            {
                Web3Wallet.INSTANCE.respondAuthRequest(
                        new Wallet.Params.AuthRequestResponse.Result(
                                requestId,
                                new Model.Cacao.Signature(SignatureType.EIP191.header, Numeric.toHexString(signature.signature), null),
                                issuer
                        ), (authRequestResponse) -> {
                            Timber.i("Sign in with Ethereum succeed.");
                            return null;
                        }, (error) -> {
                            Timber.w("Sign in with Ethereum failed.");
                            Timber.w(error.throwable);
                            return null;
                        });
            }
        };
    }

    private void closeWalletConnectActivity()
    {
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity topActivity = App.getInstance().getTopActivity();
            if (topActivity != null)
            {
                topActivity.onBackPressed();
            }
        });
    }


    @Override
    public void onError(@NonNull Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
    }

    @Override
    public void onSessionSettleResponse(@NonNull Model.SettledSessionResponse settledSessionResponse)
    {
        Timber.tag(TAG).i("onSessionSettleResponse: %s", settledSessionResponse.toString());
    }

    @Override
    public void onSessionUpdateResponse(@NonNull Model.SessionUpdateResponse sessionUpdateResponse)
    {
        Timber.tag(TAG).i("onSessionUpdateResponse");
    }

    @Override
    public void onAuthRequest(@NonNull Model.AuthRequest authRequest, @NonNull Model.VerifyContext verifyContext)
    {
        showApprovalDialog(authRequest);
    }

    @Override
    public void onSessionProposal(@NonNull Model.SessionProposal sessionProposal, @NonNull Model.VerifyContext verifyContext)
    {
        WalletConnectV2SessionItem sessionItem = WalletConnectV2SessionItem.from(sessionProposal);
        if (!validChainId(sessionItem.chains))
        {
            return;
        }
        AWWalletConnectClient.sessionProposal = sessionProposal;
        Intent intent = new Intent(context, WalletConnectV2Activity.class);
        intent.putExtra("session", sessionItem);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onSessionRequest(@NonNull Model.SessionRequest sessionRequest, @NonNull Model.VerifyContext verifyContext)
    {
        String checkMethod;
        String method = sessionRequest.getRequest().getMethod();
        if (method.startsWith("eth_signTypedData"))
        {
            checkMethod = "eth_signTypedData";
        }
        else
        {
            checkMethod = method;
        }

        if (!WCMethodChecker.includes(checkMethod))
        {
            reject(sessionRequest);
            return;
        }

        Model.Session settledSession = getSession(sessionRequest.getTopic());

        if (settledSession == null)
        {
            return;
        }

        Activity topActivity = App.getInstance().getTopActivity();
        if (topActivity != null)
        {
            WalletConnectV2SessionRequestHandler handler = new WalletConnectV2SessionRequestHandler(sessionRequest, settledSession, topActivity, this);
            handler.handle(method, actionSheetCallback);
            requestHandlers.append(sessionRequest.getRequest().getId(), handler);
        }
    }

    public Intent getSessionIntent(Context appContext)
    {
        Intent intent;
        List<WalletConnectSessionItem> sessions = walletConnectInteract.getSessions();
        if (sessions.size() == 1)
        {
            intent = WalletConnectSessionActivity.newIntent(appContext, sessions.get(0));
        }
        else
        {
            intent = new Intent(appContext, WalletConnectSessionActivity.class);
        }

        return intent;
    }

    public interface WalletConnectV2Callback
    {
        default void onSessionProposalApproved()
        {
        }

        default void onSessionProposalRejected()
        {
        }

        default void onSessionDisconnected()
        {
        }
    }
}
