package com.langitwallet.app.walletconnect;

import static com.langitwallet.app.repository.SharedPreferenceRepository.DEVELOPER_OVERRIDE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.langitwallet.app.R;
import com.alphawallet.token.entity.Signable;
import com.langitwallet.app.entity.walletconnect.SignType;
import com.langitwallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.langitwallet.app.repository.EthereumNetworkBase;
import com.langitwallet.app.ui.HomeActivity;
import com.langitwallet.app.ui.WalletConnectV2Activity;
import com.langitwallet.app.ui.widget.entity.ActionSheetCallback;
import com.langitwallet.app.walletconnect.entity.BaseRequest;
import com.langitwallet.app.walletconnect.entity.EthSignRequest;
import com.langitwallet.app.widget.AWalletAlertDialog;
import com.langitwallet.app.widget.ActionSheet;
import com.langitwallet.app.widget.ActionSheetSignDialog;
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
    private WalletConnectV2SessionItem sessionItem;
    private ActionSheet actionSheet;

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
            Signable signable = signRequest.getSignable(sessionRequest.getRequest().getId(),
                Objects.requireNonNull(settledSession.getMetaData()).getUrl());
            if (signable.isDangerous())
            {
                showNotSigning(aCallback, signRequest, signable);
            }
            else if (!validateChainId(signable))
            {
                checkProceed(aCallback, signRequest, signable);
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

    private boolean validateChainId(Signable signable)
    {
        switch (signable.getMessageType())
        {
            case SIGN_MESSAGE:
            case SIGN_PERSONAL_MESSAGE:
            case SIGN_TYPED_DATA:
                return true; //no chain checking
            case SIGN_TYPED_DATA_V3:
            case SIGN_TYPED_DATA_V4: //NB: Many V4 constructs have the wrong chainId specified.
                return (signable.getChainId() == -1 || //if chainId is unspecified treat as no restriction intended
                        getChainListFromSession().contains(signable.getChainId()));
            case ATTESTATION:
                //TODO: Check attestation signing chain
                return true;
            case SIGN_ERROR:
            default:
                return false;
        }
    }

    private WalletConnectV2SessionItem getSessionItem()
    {
        return sessionItem != null ? this.sessionItem : new WalletConnectV2SessionItem(settledSession);
    }

    private List<Long> getChainListFromSession()
    {
        List<Long> chainList = new ArrayList<>();
        for (String chain : getSessionItem().chains)
        {
            if (chain.contains(":"))
            {
                chainList.add(Long.parseLong(chain.split(":")[1]));
            }
        }

        return chainList;
    }

    private void showActionSheet(ActionSheetCallback aCallback, BaseRequest signRequest, Signable signable)
    {
        if (activity instanceof HomeActivity homeActivity)
        {
            homeActivity.clearWalletConnectRequest();
        }

        if (actionSheet != null && actionSheet.isShowing())
        {
            actionSheet.forceDismiss();
        }
        actionSheet = new ActionSheetSignDialog(activity, aCallback, signable);
        actionSheet.setSigningWallet(signRequest.getWalletAddress());
        List<String> icons = Objects.requireNonNull(settledSession.getMetaData()).getIcons();
        if (!icons.isEmpty())
        {
            actionSheet.setIcon(icons.get(0));
        }
        actionSheet.show();
    }

    private void checkProceed(ActionSheetCallback aCallback, BaseRequest signRequest, Signable signable)
    {
        AWalletAlertDialog errorDialog = new AWalletAlertDialog(activity, AWalletAlertDialog.ERROR);
        String networkName = EthereumNetworkBase.isChainSupported(signable.getChainId()) ? EthereumNetworkBase.getShortChainName(signable.getChainId())
                : Long.toString(signable.getChainId());
        String message = activity.getString(R.string.session_not_authorised, networkName);
        errorDialog.setMessage(message);
        errorDialog.setButton(R.string.override, v -> {
            errorDialog.dismiss();
            showActionSheet(aCallback, signRequest, signable);
        });
        errorDialog.setSecondaryButton(R.string.action_cancel, v -> {
            errorDialog.dismiss();
            cancelRequest(aCallback, signable, errorDialog);
        });
        errorDialog.setCancelable(false);
        errorDialog.show();
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
            errorDialog.dismiss();
        });
        errorDialog.setSecondaryButton(R.string.action_cancel, v -> {
            cancelRequest(aCallback, signable, errorDialog);
            errorDialog.dismiss();
        });
        errorDialog.setCancelable(false);
        errorDialog.show();
    }

    private void showNotSigning(ActionSheetCallback aCallback, BaseRequest signRequest, Signable signable)
    {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean hasDeveloperOverride = pref.getBoolean(DEVELOPER_OVERRIDE, false);
        AWalletAlertDialog errorDialog = new AWalletAlertDialog(activity, AWalletAlertDialog.ERROR);
        errorDialog.setMessage(activity.getString(R.string.override_warning_text));
        errorDialog.setButton(R.string.action_cancel, v -> {
            cancelRequest(aCallback, signable, errorDialog);
        });
        if (hasDeveloperOverride)
        {
            errorDialog.setSecondaryButton(R.string.override, v -> {
                showActionSheet(aCallback, signRequest, signable);
                errorDialog.dismiss();
            });
        }
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
