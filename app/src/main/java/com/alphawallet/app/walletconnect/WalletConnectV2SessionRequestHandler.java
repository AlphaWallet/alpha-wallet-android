package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.SignPersonalMessageRequest;
import com.alphawallet.app.walletconnect.entity.SignTypedDataRequest;
import com.alphawallet.app.widget.SignMethodDialog;
import com.walletconnect.walletconnectv2.client.Sign;

import timber.log.Timber;

public class WalletConnectV2SessionRequestHandler
{
    private final Sign.Model.SessionRequest sessionRequest;
    private final Sign.Model.Session settledSession;
    private final Activity activity;
    private final AWWalletConnectClient client;

    public WalletConnectV2SessionRequestHandler(Sign.Model.SessionRequest sessionRequest, Sign.Model.Session settledSession, Activity activity, AWWalletConnectClient client)
    {
        this.sessionRequest = sessionRequest;
        this.settledSession = settledSession;
        this.activity = activity;
        this.client = client;
    }

    public void handle(String method)
    {
        activity.runOnUiThread(() -> {
            showDialog(method);
        });
    }

    private void showDialog(String method)
    {
        boolean isSignTransaction = "eth_signTransaction".equals(method);
        boolean isSendTransaction = "eth_sendTransaction".equals(method);
        if (isSendTransaction || isSignTransaction)
        {
            TransactionDialogBuilder transactionDialogBuilder = new TransactionDialogBuilder(activity, sessionRequest, settledSession, client, isSignTransaction);
            FragmentManager fragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
            transactionDialogBuilder.show(fragmentManager, "wc_call");
            return;
        }

        if ("personal_sign".equals(method))
        {
            personalSign().show();
            return;
        }

        if ("eth_sign".equals(method))
        {
            ethSign().show();
            return;
        }

        if ("eth_signTypedData".equals(method))
        {
            ethSignTypedData().show();
            return;
        }

        Timber.e("Method %s not support.", method);
    }

    private Dialog ethSign()
    {
        BaseRequest request = new SignRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, sessionRequest, request, settledSession.getMetaData());
    }

    @NonNull
    private SignMethodDialog ethSignTypedData()
    {
        BaseRequest request = new SignTypedDataRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, sessionRequest, request, settledSession.getMetaData());
    }

    @NonNull
    private SignMethodDialog personalSign()
    {
        BaseRequest request = new SignPersonalMessageRequest(sessionRequest.getRequest().getParams());
        return new SignMethodDialog(activity, sessionRequest, request, settledSession.getMetaData());
    }

}
