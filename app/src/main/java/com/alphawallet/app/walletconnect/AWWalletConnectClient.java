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
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;
import com.walletconnect.walletconnectv2.core.exceptions.WalletConnectException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class AWWalletConnectClient implements WalletConnectClient.WalletDelegate
{
    public static AuthenticationCallback authCallback;
    private final WalletConnectInteract walletConnectInteract;
    public static WalletConnect.Model.SessionProposal sessionProposal;

    public static SignMethodDialogViewModel viewModel;
    private final Context context;
    private final MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData = new MutableLiveData<>(Collections.emptyList());

    public AWWalletConnectClient(Context context, WalletConnectInteract walletConnectInteract)
    {
        this.context = context;
        this.walletConnectInteract = walletConnectInteract;
    }

    @Override
    public void onSessionDelete(@NonNull WalletConnect.Model.DeletedSession deletedSession)
    {
        updateNotification();
    }

    @Override
    public void onSessionNotification(@NonNull WalletConnect.Model.SessionNotification sessionNotification)
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSessionProposal(@NonNull WalletConnect.Model.SessionProposal sessionProposal)
    {
        AWWalletConnectClient.sessionProposal = sessionProposal;
        Intent intent = new Intent(context, WalletConnectV2Activity.class);
        intent.putExtra("session", WalletConnectV2SessionItem.from(sessionProposal));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSessionRequest(@NonNull WalletConnect.Model.SessionRequest sessionRequest)
    {
        String method = sessionRequest.getRequest().getMethod();

        if (!WCMethodChecker.includes(method))
        {
            reject(sessionRequest);
            return;
        }

        WalletConnect.Model.SettledSession settledSession = getSession(sessionRequest.getTopic());

        Activity topActivity = App.getInstance().getTopActivity();
        WalletConnectV2SessionRequestHandler handler = new WalletConnectV2SessionRequestHandler(sessionRequest, settledSession, topActivity, this);
        handler.handle(method);
    }

    private WalletConnect.Model.SettledSession getSession(String topic)
    {
        List<WalletConnect.Model.SettledSession> listOfSettledSessions = WalletConnectClient.INSTANCE.getListOfSettledSessions();
        for (WalletConnect.Model.SettledSession session : listOfSettledSessions)
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
        WalletConnect.Params.Pair pair = new WalletConnect.Params.Pair(url);
        try
        {
            WalletConnectClient.INSTANCE.pair(pair, new WalletConnect.Listeners.Pairing()
            {
                @Override
                public void onSuccess(@NonNull WalletConnect.Model.SettledPairing settledPairing)
                {
                    Timber.i("onSuccess");
                }

                @Override
                public void onError(@NonNull Throwable throwable)
                {
                    Timber.e(throwable);
                }
            });
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
    }

    public void approve(WalletConnect.Model.SessionRequest sessionRequest, String result)
    {
        WalletConnect.Model.JsonRpcResponse jsonRpcResponse = new WalletConnect.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.getRequest().getId(), result);
        WalletConnect.Params.Response response = new WalletConnect.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        try
        {
            WalletConnectClient.INSTANCE.respond(response, Timber::e);
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
    }

    public void reject(WalletConnect.Model.SessionRequest sessionRequest)
    {
        reject(sessionRequest, context.getString(R.string.message_reject_request));
    }

    public void approve(WalletConnect.Model.SessionProposal sessionProposal, List<String> accounts, WalletConnectV2Callback callback)
    {
        WalletConnect.Params.Approve approve = new WalletConnect.Params.Approve(sessionProposal, accounts);
        WalletConnectClient.INSTANCE.approve(approve, new WalletConnect.Listeners.SessionApprove()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.SettledSession settledSession)
            {
                callback.onSessionProposalApproved();
                updateNotification();
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {
                Timber.e(throwable);
            }
        });
    }

    public MutableLiveData<List<WalletConnectSessionItem>> sessionItemMutableLiveData()
    {
        return sessionItemMutableLiveData;
    }

    public void updateNotification()
    {
        sessionItemMutableLiveData.postValue(walletConnectInteract.getSessions());
    }

    public void reject(WalletConnect.Model.SessionProposal sessionProposal, WalletConnectV2Callback callback)
    {

        WalletConnectClient.INSTANCE.reject(new WalletConnect.Params.Reject(context.getString(R.string.message_reject_request), sessionProposal.getTopic()), new WalletConnect.Listeners.SessionReject()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.RejectedSession rejectedSession)
            {
                callback.onSessionProposalRejected();
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {
                Timber.e(throwable);
            }
        });
    }

    public void disconnect(String sessionId, WalletConnectV2Callback callback)
    {
        try
        {
            WalletConnectClient.INSTANCE.disconnect(new WalletConnect.Params.Disconnect(sessionId, context.getString(R.string.wc_disconnect)), new WalletConnect.Listeners.SessionDelete()
            {
                @Override
                public void onSuccess(@NonNull WalletConnect.Model.DeletedSession deletedSession)
                {
                    callback.onSessionDisconnected();
                    updateNotification();
                }

                @Override
                public void onError(@NonNull Throwable throwable)
                {
                    Timber.e(throwable);
                }
            });
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
    }

    public void reject(WalletConnect.Model.SessionRequest sessionRequest, String failMessage)
    {
        WalletConnect.Model.JsonRpcResponse jsonRpcResponse = new WalletConnect.Model.JsonRpcResponse.JsonRpcError(sessionRequest.getRequest().getId(), new WalletConnect.Model.JsonRpcResponse.Error(0, failMessage));
        WalletConnect.Params.Response response = new WalletConnect.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        try
        {
            WalletConnectClient.INSTANCE.respond(response, Timber::e);
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
    }

    public void init(Application application)
    {
        WalletConnect.Model.AppMetaData appMetaData = getAppMetaData(application);
        WalletConnect.Params.Init init = new WalletConnect.Params.Init(application,
                String.format("%s/?projectId=%s", C.WALLET_CONNECT_REACT_APP_RELAY_URL, BuildConfig.WALLETCONNECT_PROJECT_ID),
                true,
                appMetaData);

        try
        {
            WalletConnectClient.INSTANCE.initialize(init, e ->
            {
                Timber.i("Init failed: %s", e.getMessage());
                return null;
            });

            WalletConnectClient.INSTANCE.setWalletDelegate(this);
        }
        catch (Exception e)
        {
            Timber.tag("seaborn").e(e);
        }
    }

    @NonNull
    private WalletConnect.Model.AppMetaData getAppMetaData(Application application)
    {
        String name = application.getString(R.string.app_name);
        String url = C.ALPHAWALLET_WEBSITE;
        String[] icons = {C.ALPHA_WALLET_LOGO_URL};
        String description = "The ultimate Web3 Wallet to power your tokens.";
        return new WalletConnect.Model.AppMetaData(name, description, url, Arrays.asList(icons));
    }

    public void shutdown()
    {
        WalletConnectClient.INSTANCE.shutdown();
    }

    public interface WalletConnectV2Callback {
        default void onSessionProposalApproved() {};
        default void onSessionProposalRejected() {};
        default void onSessionDisconnected() {};
    }
}

