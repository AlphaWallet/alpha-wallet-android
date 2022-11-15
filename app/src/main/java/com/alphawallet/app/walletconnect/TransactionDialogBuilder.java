package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.app.Dialog;
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

import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.token.entity.Signable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.walletconnect.sign.client.Sign;

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
    private final Sign.Model.SessionRequest sessionRequest;
    private final Sign.Model.Session settledSession;
    private final AWWalletConnectClient awWalletConnectClient;
    private final boolean signOnly;
    private WalletConnectViewModel viewModel;
    private ActionSheetDialog actionSheetDialog;
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> actionSheetDialog.setCurrentGasIndex(result));

    public TransactionDialogBuilder(Activity activity, Sign.Model.SessionRequest sessionRequest, Sign.Model.Session settledSession, AWWalletConnectClient awWalletConnectClient, boolean signOnly)
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
        final Web3Transaction w3Tx = new Web3Transaction(wcTx, 0);
        Wallet fromWallet = viewModel.findWallet(wcTx.getFrom());
        Token token = viewModel.getTokensService().getTokenOrBase(WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), w3Tx.recipient.toString());
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
                signMessage(fromWallet, tx, awWalletConnectClient);
            }

            @Override
            public void sendTransaction(Web3Transaction tx)
            {
                TransactionDialogBuilder.this.sendTransaction(fromWallet, tx, awWalletConnectClient);
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
            public ActivityResultLauncher<Intent> gasSelectLauncher()
            {
                return activityResultLauncher;
            }
        });
        if (signOnly)
        {
            actionSheetDialog.setSignOnly();
        }
        String url = Objects.requireNonNull(settledSession.getMetaData()).getUrl();
        actionSheetDialog.setURL(url);
        actionSheetDialog.setCanceledOnTouchOutside(false);
        actionSheetDialog.waitForEstimate();

        viewModel.calculateGasEstimate(fromWallet, Numeric.hexStringToByteArray(w3Tx.payload),
                        WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), w3Tx.recipient.toString(), new BigDecimal(w3Tx.value), w3Tx.gasLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(actionSheetDialog::setGasEstimate,
                        Throwable::printStackTrace)
                .isDisposed();

        return actionSheetDialog;
    }

    private void signMessage(Wallet fromWallet, Web3Transaction tx, AWWalletConnectClient awWalletConnectClient)
    {
        viewModel.signTransaction(actionSheetDialog.getContext(), tx, new DAppFunction()
        {
            @Override
            public void DAppError(Throwable error, Signable message)
            {
                reject(error, awWalletConnectClient);
            }

            @Override
            public void DAppReturn(byte[] data, Signable message)
            {
                approve(Numeric.toHexString(data), awWalletConnectClient);
                actionSheetDialog.transactionWritten(".");
            }
        }, Objects.requireNonNull(settledSession.getMetaData()).getUrl(), WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), fromWallet);
    }

    private void sendTransaction(Wallet wallet, Web3Transaction tx, AWWalletConnectClient awWalletConnectClient)
    {
        viewModel.sendTransaction(tx, wallet, WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())), new SendTransactionInterface()
        {
            @Override
            public void transactionSuccess(Web3Transaction web3Tx, String hashData)
            {
                approve(hashData, awWalletConnectClient);
                actionSheetDialog.transactionWritten(hashData);
            }

            @Override
            public void transactionError(long callbackId, Throwable error)
            {
                reject(error, awWalletConnectClient);
            }
        });
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

}
