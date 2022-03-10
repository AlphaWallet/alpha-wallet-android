package com.alphawallet.app.service;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alphawallet.app.App;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.app.walletconnect.TransactionDialogBuilder;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.SignPersonalMessageRequest;
import com.alphawallet.app.walletconnect.entity.SignTypedDataRequest;
import com.alphawallet.app.widget.SignMethodDialog;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;
import com.walletconnect.walletconnectv2.core.exceptions.WalletConnectException;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class AWWalletConnectClient implements WalletConnectClient.WalletDelegate
{
    public static Intent data;
    public static AuthenticationCallback authCallback;
    private final WalletConnectInteract walletConnectInteract;
    public static WalletConnect.Model.SessionProposal sessionProposal;

    public static SignMethodDialogViewModel viewModel;
    private Context context;

    public AWWalletConnectClient(Context context, WalletConnectInteract walletConnectInteract)
    {
        this.context = context;
        this.walletConnectInteract = walletConnectInteract;
    }

    @Override
    public void onSessionDelete(@NonNull WalletConnect.Model.DeletedSession deletedSession)
    {
    }

    @Override
    public void onSessionNotification(@NonNull WalletConnect.Model.SessionNotification sessionNotification)
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSessionProposal(@NonNull WalletConnect.Model.SessionProposal sessionProposal)
    {
        Log.d("seaborn", "onSessionProposal: ");
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

        Timber.tag("seaborn").d("onSessionRequest - method: " + sessionRequest.getRequest().getMethod() + ", params:" + sessionRequest.getRequest().getParams());

        WalletConnect.Model.SettledSession settledSession = getSession(sessionRequest.getTopic());

        Activity topActivity = App.getInstance().getTopActivity();
        topActivity.runOnUiThread(() -> {
            Dialog dialog = createDialog(method, sessionRequest, settledSession, topActivity);
            dialog.show();
        });
    }

    private Dialog createDialog(String method, @NonNull WalletConnect.Model.SessionRequest sessionRequest, WalletConnect.Model.SettledSession settledSession, Activity topActivity)
    {
        boolean sendTransaction = "eth_sendTransaction".equals(method);
        boolean signTransaction = "eth_signTransaction".equals(method);
        if (sendTransaction || signTransaction)
        {
            return new TransactionDialogBuilder(topActivity, sessionRequest, settledSession).build(this, signTransaction);
        }

        BaseRequest request = null;
        if ("personal_sign".equals(method))
        {
            request = new SignPersonalMessageRequest(sessionRequest.getRequest().getParams());
        } else if ("eth_signTypedData".equals(method))
        {
            request = new SignTypedDataRequest(sessionRequest.getRequest().getParams());
        }
        return new SignMethodDialog(topActivity, settledSession, sessionRequest, request);
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
        WalletConnect.Model.JsonRpcResponse jsonRpcResponse = new WalletConnect.Model.JsonRpcResponse.JsonRpcError(sessionRequest.getRequest().getId(), new WalletConnect.Model.JsonRpcResponse.Error(0, "User rejected."));
        WalletConnect.Params.Response response = new WalletConnect.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        try
        {
            Log.d("seaborn", "reject: " + sessionRequest.getTopic());
            WalletConnectClient.INSTANCE.respond(response, t ->
            {
                Log.d("seaborn", "respond: " + t);
                Timber.e(t);
            });
        } catch (WalletConnectException e)
        {
            Timber.e(e);
            Log.d("seaborn", "reject: " + e);
        }
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

            }
        });
    }

    public void updateNotification()
    {
        if (walletConnectInteract.getSessionsCount() > 0)
        {
            Intent service = new Intent(context, WalletConnectV2Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } else
        {
            context.stopService(new Intent(context, WalletConnectV2Service.class));
        }
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
            }
        });
    }

    public void disconnect(String sessionId, WalletConnectV2Callback callback)
    {
        try
        {
            WalletConnectClient.INSTANCE.disconnect(new WalletConnect.Params.Disconnect(sessionId, "User disconnect the session."), new WalletConnect.Listeners.SessionDelete()
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

    public interface WalletConnectV2Callback {
        default void onSessionProposalApproved() {};
        default void onSessionProposalRejected() {};
        default void onSessionDisconnected() {};
    }
}

