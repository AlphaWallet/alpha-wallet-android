package com.alphawallet.app.walletconnect;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.App;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.walletconnect.NamespaceParser;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.service.WalletConnectV2Service;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.app.walletconnect.util.WCMethodChecker;
import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;
import com.walletconnect.sign.client.SignInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import kotlin.Unit;
import timber.log.Timber;

public class AWWalletConnectClient implements SignClient.WalletDelegate
{
    private static final String TAG = AWWalletConnectClient.class.getName();
    public static AuthenticationCallback authCallback;
    private final WalletConnectInteract walletConnectInteract;
    public static Sign.Model.SessionProposal sessionProposal;

    public static SignMethodDialogViewModel viewModel;
    private final Context context;
    private final MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData = new MutableLiveData<>(Collections.emptyList());

    public AWWalletConnectClient(Context context, WalletConnectInteract walletConnectInteract)
    {
        this.context = context;
        this.walletConnectInteract = walletConnectInteract;
    }

    public void onSessionDelete(@NonNull Sign.Model.DeletedSession deletedSession)
    {
        updateNotification();
    }

    public void onSessionProposal(@NonNull Sign.Model.SessionProposal sessionProposal)
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onSessionRequest(@NonNull Sign.Model.SessionRequest sessionRequest)
    {
        String method = sessionRequest.getRequest().getMethod();

        if (!WCMethodChecker.includes(method))
        {
            reject(sessionRequest);
            return;
        }

        Sign.Model.Session settledSession = getSession(sessionRequest.getTopic());

        Activity topActivity = App.getInstance().getTopActivity();
        WalletConnectV2SessionRequestHandler handler = new WalletConnectV2SessionRequestHandler(sessionRequest, settledSession, topActivity, this);
        handler.handle(method);
    }

    private Sign.Model.Session getSession(String topic)
    {
        List<Sign.Model.Session> listOfSettledSessions;

        try
        {
            listOfSettledSessions = SignClient.INSTANCE.getListOfSettledSessions();
        }
        catch (IllegalStateException e)
        {
            listOfSettledSessions = Collections.emptyList();
            Timber.tag(TAG).e(e);
        }


        for (Sign.Model.Session session : listOfSettledSessions)
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
        Sign.Params.Pair pair = new Sign.Params.Pair(url);
        SignClient.INSTANCE.pair(pair, error -> {
            Timber.e(error.getThrowable());
            callback.accept(error.getThrowable().getMessage());
            return null;
        });
    }

    public void approve(Sign.Model.SessionRequest sessionRequest, String result)
    {
        Sign.Model.JsonRpcResponse jsonRpcResponse = new Sign.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.getRequest().getId(), result);
        Sign.Params.Response response = new Sign.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        SignClient.INSTANCE.respond(response, this::onSessionRequestApproveError);
    }

    private Unit onSessionRequestApproveError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void reject(Sign.Model.SessionRequest sessionRequest)
    {
        reject(sessionRequest, context.getString(R.string.message_reject_request));
    }

    public void approve(Sign.Model.SessionProposal sessionProposal, List<String> selectedAccounts, WalletConnectV2Callback callback)
    {
        String proposerPublicKey = sessionProposal.getProposerPublicKey();
        Sign.Params.Approve approve = new Sign.Params.Approve(proposerPublicKey, buildNamespaces(sessionProposal, selectedAccounts), sessionProposal.getRelayProtocol());
        SignClient.INSTANCE.approveSession(approve, this::onSessionApproveError);
        callback.onSessionProposalApproved();
        new Handler().postDelayed(this::updateNotification, 3000);
    }

    private Map<String, Sign.Model.Namespace.Session> buildNamespaces(Sign.Model.SessionProposal sessionProposal, List<String> selectedAccounts)
    {
        Map<String, Sign.Model.Namespace.Session> result = new HashMap<>();
        Map<String, Sign.Model.Namespace.Proposal> namespaces = sessionProposal.getRequiredNamespaces();
        NamespaceParser namespaceParser = new NamespaceParser();
        namespaceParser.parseProposal(namespaces);
        List<String> accounts = toCAIP10(namespaceParser.getChains(), selectedAccounts);
        for (Map.Entry<String, Sign.Model.Namespace.Proposal> entry : namespaces.entrySet())
        {
            Sign.Model.Namespace.Session session = new Sign.Model.Namespace.Session(accounts, namespaceParser.getMethods(), namespaceParser.getEvents(), null);
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

    private Unit onSessionApproveError(Sign.Model.Error error)
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


    public void reject(Sign.Model.SessionProposal sessionProposal, WalletConnectV2Callback callback)
    {

        SignClient.INSTANCE.rejectSession(
                new Sign.Params.Reject(
                        sessionProposal.getProposerPublicKey(), context.getString(R.string.message_reject_request), 0),
                this::onSessionRejectError);
        callback.onSessionProposalRejected();
    }

    private Unit onSessionRejectError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void disconnect(String sessionId, WalletConnectV2Callback callback)
    {
        SignClient.INSTANCE.disconnect(new Sign.Params.Disconnect(sessionId), this::onDisconnectError);
        callback.onSessionDisconnected();
        updateNotification();
    }

    private Unit onDisconnectError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void reject(Sign.Model.SessionRequest sessionRequest, String failMessage)
    {
        Sign.Model.JsonRpcResponse jsonRpcResponse = new Sign.Model.JsonRpcResponse.JsonRpcError(sessionRequest.getRequest().getId(), 0, failMessage);
        Sign.Params.Response response = new Sign.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        SignClient.INSTANCE.respond(response, this::onSessionRequestRejectError);
    }

    private Unit onSessionRequestRejectError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void init(Application application)
    {
        Sign.Model.AppMetaData appMetaData = getAppMetaData(application);
        Sign.Params.Init init = new Sign.Params.Init(application,
                String.format("%s/?projectId=%s", C.WALLET_CONNECT_REACT_APP_RELAY_URL, BuildConfig.WALLETCONNECT_PROJECT_ID),
                appMetaData,
                null,
                Sign.ConnectionType.AUTOMATIC);

        SignClient.INSTANCE.initialize(init, e ->
        {
            Timber.tag(TAG).e("Init failed: %s", e.getThrowable().getMessage());
            return null;
        });

        try
        {
            SignClient.INSTANCE.setWalletDelegate((SignInterface.WalletDelegate) this);
        }
        catch (Exception e)
        {
            Timber.tag(TAG).e(e);
        }
    }

    @NonNull
    private Sign.Model.AppMetaData getAppMetaData(Application application)
    {
        String name = application.getString(R.string.app_name);
        String url = C.ALPHAWALLET_WEBSITE;
        String[] icons = {C.ALPHA_WALLET_LOGO_URL};
        String description = "The ultimate Web3 Wallet to power your tokens.";
        return new Sign.Model.AppMetaData(name, description, url, Arrays.asList(icons));
    }

    public void shutdown()
    {
        Timber.tag(TAG).i("shutdown");
    }

    public void onConnectionStateChange(@NonNull Sign.Model.ConnectionState connectionState)
    {
        Timber.tag(TAG).i("onConnectionStateChange");
    }

    public void onSessionSettleResponse(@NonNull Sign.Model.SettledSessionResponse settledSessionResponse)
    {
        Timber.tag(TAG).i("onSessionSettleResponse");
    }

    public void onSessionUpdateResponse(@NonNull Sign.Model.SessionUpdateResponse sessionUpdateResponse)
    {
        Timber.tag(TAG).i("onSessionUpdateResponse");
    }

    public void onError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
    }

    public interface WalletConnectV2Callback
    {
        default void onSessionProposalApproved()
        {
        }

        ;

        default void onSessionProposalRejected()
        {
        }

        ;

        default void onSessionDisconnected()
        {
        }

        ;
    }
}

