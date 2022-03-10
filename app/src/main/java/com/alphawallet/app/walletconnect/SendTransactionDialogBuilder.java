package com.alphawallet.app.walletconnect;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AWWalletConnectClient;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.walletconnect.walletconnectv2.client.WalletConnect;

import org.web3j.utils.Numeric;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SendTransactionDialogBuilder
{
    private Activity activity;
    private WalletConnect.Model.SessionRequest sessionRequest;
    private WalletConnect.Model.SettledSession settledSession;
    private WalletConnectViewModel viewModel;
    private ActionSheetDialog actionSheetDialog;

    public SendTransactionDialogBuilder(Activity activity, WalletConnect.Model.SessionRequest sessionRequest, WalletConnect.Model.SettledSession settledSession)
    {
        this.activity = activity;
        this.sessionRequest = sessionRequest;
        this.settledSession = settledSession;

        initViewModel();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity)
                .get(WalletConnectViewModel.class);
    }

    public Dialog build(AWWalletConnectClient awWalletConnectClient)
    {
        Type listType = new TypeToken<ArrayList<WCEthereumTransaction>>()
        {
        }.getType();
        List<WCEthereumTransaction> list = new Gson().fromJson(sessionRequest.getRequest().getParams(), listType);
        WCEthereumTransaction wcTx = list.get(0);
        final Web3Transaction w3Tx = new Web3Transaction(wcTx, 0);
        Wallet wallet = viewModel.findWallet(wcTx.getFrom());
        Token token = viewModel.getTokensService().getTokenOrBase(getChainId(), w3Tx.recipient.toString());
        actionSheetDialog = new ActionSheetDialog(activity, w3Tx, token, "",
                w3Tx.recipient.toString(), viewModel.getTokensService(), new ActionSheetCallback()
        {
            @Override
            public void getAuthorisation(SignAuthenticationCallback callback)
            {
                Log.d("seaborn", "getAuthorisation: " + callback);
                viewModel.getAuthenticationForSignature(wallet, activity, callback);
            }

            @Override
            public void sendTransaction(Web3Transaction tx)
            {
                Log.d("seaborn", "sendTransaction: ");
                viewModel.sendTransaction(tx, wallet, getChainId(), new SendTransactionInterface()
                {
                    @Override
                    public void transactionSuccess(Web3Transaction web3Tx, String hashData)
                    {
                        Log.d("seaborn", "transactionSuccess: " + hashData  );

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            awWalletConnectClient.approve(sessionRequest, hashData);
                        }, 5000);
                        actionSheetDialog.dismiss();
                    }

                    @Override
                    public void transactionError(long callbackId, Throwable error)
                    {
                        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d("seaborn", "transactionError: ");
                        awWalletConnectClient.reject(sessionRequest);
                        actionSheetDialog.dismiss();
                    }
                });
            }

            @Override
            public void dismissed(String txHash, long callbackId, boolean actionCompleted)
            {
                Log.d("seaborn", "dismissed: ");
            }

            @Override
            public void notifyConfirm(String mode)
            {
                Log.d("seaborn", "notifyConfirm: " + mode);
            }
        });
        actionSheetDialog.setURL(settledSession.getPeerAppMetaData().getUrl());
        actionSheetDialog.setCanceledOnTouchOutside(false);
        actionSheetDialog.waitForEstimate();

        viewModel.calculateGasEstimate(wallet, Numeric.hexStringToByteArray(w3Tx.payload),
                getChainId(), w3Tx.recipient.toString(), new BigDecimal(w3Tx.value), w3Tx.gasLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(actionSheetDialog::setGasEstimate,
                        Throwable::printStackTrace)
                .isDisposed();
        return actionSheetDialog;
    }

    private long getChainId()
    {
        return Long.parseLong(sessionRequest.getChainId().split(":")[1]);
    }

}
