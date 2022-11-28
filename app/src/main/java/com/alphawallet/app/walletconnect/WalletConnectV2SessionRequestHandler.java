package com.alphawallet.app.walletconnect;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.EthSignRequest;
import com.alphawallet.app.widget.ActionSheet;
import com.alphawallet.app.widget.ActionSheetSignDialog;
import com.alphawallet.token.entity.Signable;
import com.walletconnect.sign.client.Sign;

import java.util.List;
import java.util.Objects;

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

    public void handle(String method, ActionSheetCallback aCallback)
    {
        activity.runOnUiThread(() -> {
            showDialog(method, aCallback);
        });
    }

    public Sign.Model.SessionRequest getSessionRequest()
    {
        return sessionRequest;
    }

    private void showDialog(String method, ActionSheetCallback aCallback)
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

        BaseRequest signRequest = EthSignRequest.getSignRequest(sessionRequest);
        if (signRequest != null)
        {
            Signable signable = signRequest.getSignable(sessionRequest.getRequest().getId(), settledSession.getMetaData().getUrl());
            ActionSheet actionSheet = new ActionSheetSignDialog(activity, aCallback, signable);
            actionSheet.setSigningWallet(signRequest.getWalletAddress());
            List<String> icons = Objects.requireNonNull(settledSession.getMetaData()).getIcons();
            if (!icons.isEmpty())
            {
                actionSheet.setIcon(icons.get(0));
            }
            actionSheet.show();
        }
        else
        {
            Timber.e("Method %s not supported.", method);
        }
    }
}
