package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.walletconnect.SignType;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.hardware.SignatureFromKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.web3j.utils.Numeric;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TransactionDialogBuilder extends DialogFragment
{
    private final Activity activity;
    private final com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest sessionRequest;
    private final com.walletconnect.web3.wallet.client.Wallet.Model.Session settledSession;
    private final AWWalletConnectClient awWalletConnectClient;
    private final boolean signOnly;
    private WalletConnectViewModel viewModel;
    private ActionSheetDialog actionSheetDialog;
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> actionSheetDialog.setCurrentGasIndex(result));

    public TransactionDialogBuilder(Activity activity, com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest sessionRequest, com.walletconnect.web3.wallet.client.Wallet.Model.Session settledSession, AWWalletConnectClient awWalletConnectClient, boolean signOnly)
    {
        this.activity = activity;
        this.sessionRequest = sessionRequest;
        this.settledSession = settledSession;
        this.awWalletConnectClient = awWalletConnectClient;
        this.signOnly = signOnly;

        initViewModel();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity)
                .get(WalletConnectViewModel.class);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionSigned().observe(this, this::txSigned);
        viewModel.transactionError().observe(this, this::txError);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        Type listType = new TypeToken<ArrayList<WCEthereumTransaction>>()
        {
        }.getType();
        List<WCEthereumTransaction> list = new Gson().fromJson(sessionRequest.getRequest().getParams(), listType);
        WCEthereumTransaction wcTx = list.get(0);
        final Web3Transaction w3Tx = new Web3Transaction(wcTx, wcTx.hashCode(), signOnly ? SignType.SIGN_TX : SignType.SEND_TX);
        final Wallet fromWallet = viewModel.findWallet(wcTx.getFrom());
        final Token token = viewModel.getTokensService().getTokenOrBase(WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), w3Tx.recipient.toString());
        actionSheetDialog = new ActionSheetDialog(activity, w3Tx, token, "", w3Tx.recipient.toString(), viewModel.getTokensService(), new ActionSheetCallback()
        {
            @Override
            public void getAuthorisation(SignAuthenticationCallback callback)
            {
                viewModel.getAuthenticationForSignature(fromWallet, activity, callback);
            }

            @Override
            public void signTransaction(Web3Transaction tx)
            {
                viewModel.requestSignatureOnly(tx, fromWallet, WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())));
            }

            @Override
            public WalletType getWalletType()
            {
                return fromWallet.type;
            }

            @Override
            public void sendTransaction(Web3Transaction tx)
            {
                viewModel.requestSignature(tx, fromWallet, WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())));
            }

            @Override
            public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
            {
                //This is the return from the hardware sign
                //it could be either a send transaction or a sign transaction.
                if (signOnly)
                {
                    txSigned(signature);
                }
                else
                {
                    viewModel.sendTransaction(fromWallet, token.tokenInfo.chainId, tx, signature);
                }
            }

            @Override
            public void dismissed(String txHash, long callbackId, boolean actionCompleted)
            {
                if (!actionCompleted)
                {
                    awWalletConnectClient.reject(sessionRequest);
                }
            }

            @Override
            public void notifyConfirm(String mode)
            {
            }

            @Override
            public void denyWalletConnect()
            {
                awWalletConnectClient.reject(sessionRequest);
            }

            @Override
            public ActivityResultLauncher<Intent> gasSelectLauncher()
            {
                return activityResultLauncher;
            }
        });
        actionSheetDialog.setSigningWallet(fromWallet.address);
        if (signOnly)
        {
            actionSheetDialog.setSignOnly();
        }
        String url = Objects.requireNonNull(settledSession.getMetaData()).getUrl();
        actionSheetDialog.setURL(url);
        actionSheetDialog.setCanceledOnTouchOutside(false);
        actionSheetDialog.waitForEstimate();

        byte[] payload = w3Tx.payload != null ? Numeric.hexStringToByteArray(w3Tx.payload) : null;

        viewModel.calculateGasEstimate(fromWallet, payload,
                        WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), w3Tx.recipient.toString(), new BigDecimal(w3Tx.value), w3Tx.gasLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(actionSheetDialog::setGasEstimate,
                        Throwable::printStackTrace)
                .isDisposed();

        return actionSheetDialog;
    }

    private void txWritten(TransactionReturn txData)
    {
        approve(txData.hash, awWalletConnectClient);
        actionSheetDialog.transactionWritten(txData.hash);
    }

    private void txSigned(SignatureFromKey sigData)
    {
        approve(Numeric.toHexString(sigData.signature), awWalletConnectClient);
        actionSheetDialog.transactionWritten(Numeric.toHexString(sigData.signature).substring(0, 66));
    }

    private void txError(TransactionReturn txError)
    {
        reject(txError.throwable, awWalletConnectClient);
    }

    private void reject(Throwable error, AWWalletConnectClient awWalletConnectClient)
    {
        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
        awWalletConnectClient.reject(sessionRequest);
        actionSheetDialog.dismiss();
    }

    private void approve(String hashData, AWWalletConnectClient awWalletConnectClient)
    {
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                awWalletConnectClient.approve(sessionRequest, hashData), 5000);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog)
    {
        super.onCancel(dialog);
        awWalletConnectClient.reject(sessionRequest);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog)
    {
        super.onDismiss(dialog);
        awWalletConnectClient.reject(sessionRequest);
    }
}
