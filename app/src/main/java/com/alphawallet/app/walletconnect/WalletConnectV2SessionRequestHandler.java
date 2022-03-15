package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.app.Dialog;

import com.alphawallet.app.service.AWWalletConnectClient;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.SignPersonalMessageRequest;
import com.alphawallet.app.walletconnect.entity.SignTypedDataRequest;
import com.alphawallet.app.widget.SignMethodDialog;
import com.walletconnect.walletconnectv2.client.WalletConnect;

import androidx.annotation.NonNull;

public class WalletConnectV2SessionRequestHandler
{
    private final WalletConnect.Model.SessionRequest sessionRequest;
    private final WalletConnect.Model.SettledSession settledSession;
    private final Activity activity;
    private final AWWalletConnectClient client;

    public WalletConnectV2SessionRequestHandler(WalletConnect.Model.SessionRequest sessionRequest, WalletConnect.Model.SettledSession settledSession, Activity activity, AWWalletConnectClient client)
    {
        this.sessionRequest = sessionRequest;
        this.settledSession = settledSession;
        this.activity = activity;
        this.client = client;
    }

    public void handle(String method)
    {
        activity.runOnUiThread(() -> {
            Dialog dialog = createDialog(method);
            if (dialog != null)
            {
                dialog.show();
            }
        });
    }

    private Dialog createDialog(String method)
    {
        switch (method)
        {
            case "eth_sendTransaction":
                return ethSendTransaction();
            case "eth_signTransaction":
                return ethSignTransaction();
            case "personal_sign":
                return personalSign();
            case "eth_sign":
                return ethSign();
            case "eth_signTypedData":
                return ethSignTypedData();
            default:
                return null;
        }
    }

    private Dialog ethSign()
    {
        BaseRequest request = new SignRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, settledSession, sessionRequest, request);
    }

    @NonNull
    private SignMethodDialog ethSignTypedData()
    {
        BaseRequest request = new SignTypedDataRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, settledSession, sessionRequest, request);
    }

    @NonNull
    private SignMethodDialog personalSign()
    {
        BaseRequest request = new SignPersonalMessageRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, settledSession, sessionRequest, request);
    }

    private Dialog ethSignTransaction()
    {
        return new TransactionDialogBuilder(activity, sessionRequest, settledSession).build(client, true);
    }

    private Dialog ethSendTransaction()
    {
        return new TransactionDialogBuilder(activity, sessionRequest, settledSession).build(client, false);
    }
}
