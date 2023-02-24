package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.walletconnect.SignType;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.EthSignRequest;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheet;
import com.alphawallet.app.widget.ActionSheetSignDialog;
import com.alphawallet.token.entity.Signable;
import com.walletconnect.web3.wallet.client.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class WalletConnectV2SessionRequestHandler
{
    private final Wallet.Model.SessionRequest sessionRequest;
    private final Wallet.Model.Session settledSession;
    private final Activity activity;
    private final AWWalletConnectClient client;

    public WalletConnectV2SessionRequestHandler(Wallet.Model.SessionRequest sessionRequest, Wallet.Model.Session settledSession, Activity activity, AWWalletConnectClient client)
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

    public Wallet.Model.SessionRequest getSessionRequest()
    {
        return sessionRequest;
    }

    private void showDialog(String method, ActionSheetCallback aCallback)
    {
        boolean isSignTransaction = "eth_signTransaction".equals(method);
        boolean isSendTransaction = "eth_sendTransaction".equals(method);
        if (isSendTransaction || isSignTransaction)
        {
            TransactionDialogBuilder transactionDialogBuilder = new TransactionDialogBuilder(activity, sessionRequest, settledSession, client, isSignTransaction ? SignType.SIGN_TX : SignType.SEND_TX);
            FragmentManager fragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
            transactionDialogBuilder.show(fragmentManager, "wc_call");
            return;
        }

        BaseRequest signRequest = EthSignRequest.getSignRequest(sessionRequest);
        if (signRequest != null)
        {
            Signable signable = signRequest.getSignable(sessionRequest.getRequest().getId(), Objects.requireNonNull(settledSession.getMetaData()).getUrl());
            WalletConnectV2SessionItem session = new WalletConnectV2SessionItem(settledSession);

            List<Long> chainList = new ArrayList<>();
            for (String chain : session.chains)
            {
                if (chain.contains(":"))
                {
                    chainList.add(Long.parseLong(chain.split(":")[1]));
                }
            }

            if (!chainList.contains(signable.getChainId()))
            {
                showErrorDialog(aCallback, signable, session);
            }
            else
            {
                showActionSheet(aCallback, signRequest, signable);
            }
        }
        else
        {
            Timber.e("Method %s not supported.", method);
        }
    }

    private void showActionSheet(ActionSheetCallback aCallback, BaseRequest signRequest, Signable signable)
    {
        ActionSheet actionSheet = new ActionSheetSignDialog(activity, aCallback, signable);
        actionSheet.setSigningWallet(signRequest.getWalletAddress());
        List<String> icons = Objects.requireNonNull(settledSession.getMetaData()).getIcons();
        if (!icons.isEmpty())
        {
            actionSheet.setIcon(icons.get(0));
        }
        actionSheet.show();
    }

    private void showErrorDialog(ActionSheetCallback aCallback, Signable signable, WalletConnectV2SessionItem session)
    {
        AWalletAlertDialog errorDialog = new AWalletAlertDialog(activity, AWalletAlertDialog.ERROR);
        String message = EthereumNetworkBase.isChainSupported(signable.getChainId()) ?
            activity.getString(R.string.error_eip712_wc2_disabled_network,
                EthereumNetworkBase.getShortChainName(signable.getChainId())) :
            activity.getString(R.string.error_eip712_unsupported_network, String.valueOf(signable.getChainId()));
        errorDialog.setMessage(message);
        errorDialog.setButton(R.string.action_view_session, v -> {
            openSessionDetail(session);
            cancelRequest(aCallback, signable, errorDialog);
        });
        errorDialog.setSecondaryButton(R.string.action_cancel, v -> {
            cancelRequest(aCallback, signable, errorDialog);
        });
        errorDialog.setCancelable(false);
        errorDialog.show();
    }

    private void openSessionDetail(WalletConnectV2SessionItem session)
    {
        Intent intent = new Intent(activity, WalletConnectV2Activity.class);
        intent.putExtra("session", session);
        activity.startActivity(intent);
    }

    private void cancelRequest(ActionSheetCallback aCallback, Signable signable, AWalletAlertDialog errorDialog)
    {
        errorDialog.dismiss();
        aCallback.dismissed("", signable.getCallbackId(), false);
    }
}
