package com.alphawallet.app.walletconnect;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.alphawallet.app.entity.cryptokeys.SignatureReturnType.SIGNATURE_GENERATED;
import static com.walletconnect.web3.wallet.client.Wallet.Model;
import static com.walletconnect.web3.wallet.client.Wallet.Params;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.walletconnect.NamespaceParser;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.service.WalletConnectV2Service;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.walletconnect.util.WCMethodChecker;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;
import com.walletconnect.android.Core;
import com.walletconnect.android.CoreClient;
import com.walletconnect.android.relay.ConnectionType;
import com.walletconnect.web3.wallet.client.Wallet.Model.Session;
import com.walletconnect.web3.wallet.client.Web3Wallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import kotlin.Unit;
import timber.log.Timber;

public class AWWalletConnectClient implements Web3Wallet.WalletDelegate
{
    private static final String TAG = AWWalletConnectClient.class.getName();
    private final WalletConnectInteract walletConnectInteract;
    public static Model.SessionProposal sessionProposal;

    private final Context context;
    private final MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData = new MutableLiveData<>(Collections.emptyList());
    private final KeyProvider keyProvider = KeyProviderFactory.get();
    private final LongSparseArray<WalletConnectV2SessionRequestHandler> requestHandlers = new LongSparseArray<>();
    private HomeActivity activity;
    private boolean hasConnection;

    public AWWalletConnectClient(Context context, WalletConnectInteract walletConnectInteract)
    {
        this.context = context;
        this.walletConnectInteract = walletConnectInteract;
        hasConnection = false;
    }

    public void onSessionDelete(@NonNull Model.SessionDelete deletedSession)
    {
        updateNotification();
    }

    public void onSessionProposal(@NonNull Model.SessionProposal sessionProposal)
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

    public void onSessionRequest(@NonNull Model.SessionRequest sessionRequest)
    {
        String method = sessionRequest.getRequest().getMethod();
        if ("eth_signTypedData_v4".equals(method))
        {
            method = "eth_signTypedData";
        }

        if (!WCMethodChecker.includes(method))
        {
            reject(sessionRequest);
            return;
        }

        Model.Session settledSession = getSession(sessionRequest.getTopic());

        WalletConnectV2SessionRequestHandler handler = new WalletConnectV2SessionRequestHandler(sessionRequest, settledSession, activity, this);
        handler.handle(method, activity);
        requestHandlers.append(sessionRequest.getRequest().getId(), handler);
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
        Web3Wallet.INSTANCE.approveSession(approve, sessionApprove -> null, this::onSessionApproveError);
        callback.onSessionProposalApproved();
        new Handler().postDelayed(this::updateNotification, 3000);
    }

    private Map<String, Model.Namespace.Session> buildNamespaces(Model.SessionProposal sessionProposal, List<String> selectedAccounts)
    {
        Map<String, Model.Namespace.Session> result = new HashMap<>();
        Map<String, Model.Namespace.Proposal> namespaces = sessionProposal.getRequiredNamespaces();
        NamespaceParser namespaceParser = new NamespaceParser();
        namespaceParser.parseProposal(namespaces);
        List<String> accounts = toCAIP10(namespaceParser.getChains(), selectedAccounts);
        for (Map.Entry<String, Model.Namespace.Proposal> entry : namespaces.entrySet())
        {
            Model.Namespace.Session session = new Model.Namespace.Session(accounts, namespaceParser.getMethods(), namespaceParser.getEvents(), null);
            result.put(entry.getKey(), session);
        }
        return result;
    }

    private List<String> toCAIP10(List<String> chains, List<String> selectedAccounts)
    {
        List<String> result = new ArrayList<>();
        for (String chain : chains)
        {
            for (String account : selectedAccounts)
            {
                result.add(chain + ":" + account);
            }
        }
        return result;
    }

    private Unit onSessionApproveError(Model.Error error)
    {
        Timber.e(error.getThrowable());
        Toast.makeText(context, error.getThrowable().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        return null;
    }

    public MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData()
    {
        return sessionItemMutableLiveData;
    }

    public void updateNotification()
    {
        walletConnectInteract.fetchSessions(context, items -> {
            sessionItemMutableLiveData.postValue(items);
            updateService(context, items);
        });
    }

    private void updateService(Context context, List<WalletConnectSessionItem> walletConnectSessionItems)
    {
        if (walletConnectSessionItems.isEmpty())
        {
            context.stopService(new Intent(context, WalletConnectV2Service.class));
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Intent service = new Intent(context, WalletConnectV2Service.class);
            context.startForegroundService(service);
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
        updateNotification();
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

    public void init(HomeActivity homeActivity)
    {
        activity = homeActivity;
    }

    public void init(Application application)
    {
        Core.Model.AppMetaData appMetaData = getAppMetaData(application);
        String relayServer = String.format("%s/?projectId=%s", C.WALLET_CONNECT_REACT_APP_RELAY_URL, keyProvider.getWalletConnectProjectId());
        CoreClient coreClient = CoreClient.INSTANCE;
        coreClient.initialize(appMetaData, relayServer, ConnectionType.AUTOMATIC, application, null, error -> {
            Timber.w(error.throwable);
            return null;
        });

        Web3Wallet.INSTANCE.initialize(new Params.Init(coreClient), e ->
        {
            Timber.tag(TAG).e("Init failed: %s", e.getThrowable().getMessage());
            return null;
        });

        try
        {
            Web3Wallet.INSTANCE.setWalletDelegate(this);
        }
        catch (Exception e)
        {
            Timber.tag(TAG).e(e);
        }
    }

    @NonNull
    private Core.Model.AppMetaData getAppMetaData(Application application)
    {
        String name = application.getString(R.string.app_name);
        String url = C.ALPHAWALLET_WEBSITE;
        String[] icons = {C.ALPHA_WALLET_LOGO_URL};
        String description = "The ultimate Web3 Wallet to power your tokens.";
        String redirect = "kotlin-responder-wc:/request";
        return new Core.Model.AppMetaData(name, description, url, Arrays.asList(icons), redirect);
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
            reject(requestHandler.getSessionRequest(), activity.getString(R.string.message_reject_request));
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

    @Override
    public void onAuthRequest(@NonNull Model.AuthRequest authRequest)
    {

    }


    @Override
    public void onError(@NonNull Model.Error error)
    {
        Timber.tag(TAG).e(error.getThrowable());
    }

    @Override
    public void onSessionSettleResponse(@NonNull Model.SettledSessionResponse settledSessionResponse)
    {
        Timber.tag(TAG).i("onSessionSettleResponse");
    }

    @Override
    public void onSessionUpdateResponse(@NonNull Model.SessionUpdateResponse sessionUpdateResponse)
    {
        Timber.tag(TAG).i("onSessionUpdateResponse");
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
