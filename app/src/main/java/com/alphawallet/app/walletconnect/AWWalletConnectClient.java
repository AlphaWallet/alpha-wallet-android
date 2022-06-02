package com.alphawallet.app.walletconnect;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.App;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.app.walletconnect.util.WCMethodChecker;
import com.walletconnect.walletconnectv2.client.Sign;
import com.walletconnect.walletconnectv2.client.SignClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import timber.log.Timber;

public class AWWalletConnectClient implements SignClient.WalletDelegate
{
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

    @Override
    public void onSessionDelete(@NonNull Sign.Model.DeletedSession deletedSession)
    {
        updateNotification();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSessionProposal(@NonNull Sign.Model.SessionProposal sessionProposal)
    {
        AWWalletConnectClient.sessionProposal = sessionProposal;
        Intent intent = new Intent(context, WalletConnectV2Activity.class);
        intent.putExtra("session", WalletConnectV2SessionItem.from(sessionProposal));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
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
        List<Sign.Model.Session> listOfSettledSessions = SignClient.INSTANCE.getListOfSettledSessions();
        for (Sign.Model.Session session : listOfSettledSessions)
        {
            if (session.getTopic().equals(topic))
            {
                return session;
            }
        }
        return null;
    }

    public void pair(String url)
    {
        Sign.Params.Pair pair = new Sign.Params.Pair(url);
        SignClient.INSTANCE.pair(pair, this::onPairError);
    }

    private Unit onPairError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void approve(Sign.Model.SessionRequest sessionRequest, String result)
    {
        Sign.Model.JsonRpcResponse jsonRpcResponse = new Sign.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.getRequest().getId(), result);
        Sign.Params.Response response = new Sign.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        SignClient.INSTANCE.respond(response, this::onSessionRequestApproveeError);
    }

    private Unit onSessionRequestApproveeError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public void reject(Sign.Model.SessionRequest sessionRequest)
    {
        reject(sessionRequest, context.getString(R.string.message_reject_request));
    }

    public void approve(Sign.Model.SessionProposal sessionProposal, WalletConnectV2Callback callback)
    {
        String proposerPublicKey = sessionProposal.getProposerPublicKey();
        Sign.Params.Approve approve = new Sign.Params.Approve(proposerPublicKey, getNamespaces(sessionProposal), sessionProposal.getRelayProtocol());
        SignClient.INSTANCE.approveSession(approve, this::onSessionApproveError);
        callback.onSessionProposalApproved();
        updateNotification();
    }

    private Map<String, Sign.Model.Namespace.Session> getNamespaces(Sign.Model.SessionProposal sessionProposal)
    {
        
        return null;
    }

    private Unit onSessionApproveError(Sign.Model.Error error)
    {
        Timber.e(error.getThrowable());
        return null;
    }

    public MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData()
    {
        return sessionItemMutableLiveData;
    }

    public void updateNotification()
    {
        sessionItemMutableLiveData.postValue(walletConnectInteract.getSessions());
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
        SignClient.INSTANCE.disconnect(new Sign.Params.Disconnect(sessionId, context.getString(R.string.wc_disconnect), 0), this::onDisconnectError);
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
            Timber.i("Init failed: %s", e.getThrowable().getMessage());
            return null;
        });

        SignClient.INSTANCE.setWalletDelegate(this);
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
    }

    @Override
    public void onConnectionStateChange(@NonNull Sign.Model.ConnectionState connectionState)
    {

    }

    @Override
    public void onSessionSettleResponse(@NonNull Sign.Model.SettledSessionResponse settledSessionResponse)
    {

    }

    @Override
    public void onSessionUpdateResponse(@NonNull Sign.Model.SessionUpdateResponse sessionUpdateResponse)
    {

    }

    public interface WalletConnectV2Callback {
        default void onSessionProposalApproved() {};
        default void onSessionProposalRejected() {};
        default void onSessionDisconnected() {};
    }
}

