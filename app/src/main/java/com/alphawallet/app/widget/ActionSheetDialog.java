package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.TransactionSuccessActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.Signable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import io.realm.Realm;

/**
 * Created by JB on 17/11/2020.
 */
public class ActionSheetDialog extends BottomSheetDialog implements StandardFunctionInterface, ActionSheetInterface
{
    private final ImageView cancelButton;
    private final GasWidget gasWidget;
    private final BalanceDisplayWidget balanceDisplay;
    private final ConfirmationWidget confirmationWidget;
    private final AddressDetailView addressDetail;
    private final AmountDisplayWidget amountDisplay;
    private final AssetDetailView assetDetailView;
    private final FunctionButtonBar functionBar;
    private final TransactionDetailWidget detailWidget;
    private final Activity activity;

    private final Token token;
    private final TokensService tokensService;

    private final Web3Transaction candidateTransaction;
    private final ActionSheetCallback actionSheetCallback;
    private SignAuthenticationCallback signCallback;
    private ActionSheetMode mode;
    private final long callbackId;

    private String txHash = null;
    private boolean actionCompleted;

    public ActionSheetDialog(@NonNull Activity activity, Web3Transaction tx, Token t,
                             String destName, String destAddress, TokensService ts,
                             ActionSheetCallback aCallBack)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet);

        gasWidget = findViewById(R.id.gas_widgetx);
        balanceDisplay = findViewById(R.id.balance);
        cancelButton = findViewById(R.id.image_close);
        confirmationWidget = findViewById(R.id.confirmation_view);
        detailWidget = findViewById(R.id.detail_widget);
        addressDetail = findViewById(R.id.recipient);
        amountDisplay = findViewById(R.id.amount_display);
        assetDetailView = findViewById(R.id.asset_detail);
        functionBar = findViewById(R.id.layoutButtons);
        this.activity = activity;
        if (activity instanceof HomeActivity)
        {
            mode = ActionSheetMode.SEND_TRANSACTION_DAPP;
        }
        else if (activity instanceof WalletConnectActivity)
        {
            mode = ActionSheetMode.SEND_TRANSACTION_WC;
        }
        else
        {
            mode = ActionSheetMode.SEND_TRANSACTION;
        }

        signCallback = null;

        actionSheetCallback = aCallBack;
        actionCompleted = false;

        token = t;
        tokensService = ts;
        candidateTransaction = tx;
        callbackId = tx.leafPosition;

        balanceDisplay.setupBalance(token, tokensService);

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();

        gasWidget.setupWidget(ts, token, candidateTransaction, this, activity);
        updateAmount();

        addressDetail.setupAddress(destAddress, destName);

        if (token.isERC721())
        {
            assetDetailView.setupAssetDetail(token, getERC721TokenId(), this);
            assetDetailView.setVisibility(View.VISIBLE);
            balanceDisplay.setVisibility(View.GONE);
            amountDisplay.setVisibility(View.GONE);
        }

        setupCancelListeners();
        isAttached = true;
    }

    public ActionSheetDialog(@NonNull Activity activity, ActionSheetCallback aCallback, SignAuthenticationCallback sCallback, Signable message)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet_sign);

        gasWidget = findViewById(R.id.gas_widgetx);
        balanceDisplay = findViewById(R.id.balance);
        cancelButton = findViewById(R.id.image_close);
        confirmationWidget = findViewById(R.id.confirmation_view);
        addressDetail = findViewById(R.id.requester);
        amountDisplay = findViewById(R.id.amount_display);
        assetDetailView = findViewById(R.id.asset_detail);
        functionBar = findViewById(R.id.layoutButtons);
        detailWidget = null;
        mode = ActionSheetMode.SIGN_MESSAGE;
        callbackId = message.getCallbackId();
        this.activity = activity;

        actionSheetCallback = aCallback;
        signCallback = sCallback;

        token = null;
        tokensService = null;
        candidateTransaction = null;
        actionCompleted = false;

        addressDetail.setupRequester(message.getOrigin());
        SignDataWidget signWidget = findViewById(R.id.sign_widget);
        signWidget.setupSignData(message);
        signWidget.setLockCallback(this);

        TextView signTitle = findViewById(R.id.text_sign_title);
        signTitle.setText(Utils.getSigningTitle(message));

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();
        setupCancelListeners();
        isAttached = true;
    }

    public void setSignOnly()
    {
        //sign only, and return signature to process
        mode = ActionSheetMode.SIGN_TRANSACTION;
    }

    public void onDestroy()
    {
        gasWidget.onDestroy();
        assetDetailView.onDestroy();
    }

    public void setURL(String url)
    {
        AddressDetailView requester = findViewById(R.id.requester);
        requester.setupRequester(url);
        detailWidget.setupTransaction(candidateTransaction, token.tokenInfo.chainId, tokensService.getCurrentAddress(),
                tokensService.getNetworkSymbol(token.tokenInfo.chainId));
        if (candidateTransaction.isConstructor())
        {
            addressDetail.setVisibility(View.GONE);
        }

        detailWidget.setLockCallback(this);
    }

    public void setCurrentGasIndex(int gasSelectionIndex, BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime, long nonce)
    {
        gasWidget.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, nonce);
        updateAmount();
    }

    private boolean isSendingTransaction()
    {
        return (mode == ActionSheetMode.SEND_TRANSACTION || mode == ActionSheetMode.SEND_TRANSACTION_DAPP || mode == ActionSheetMode.SEND_TRANSACTION_WC
         || mode == ActionSheetMode.SIGN_TRANSACTION);
    }

    @Override
    public void updateAmount()
    {
        String amountVal = BalanceUtils.getScaledValueScientific(getTransactionAmount(), token.tokenInfo.decimals);
        showAmount(amountVal);
    }

    @Override
    public void handleClick(String action, int id)
    {
        switch (mode)
        {
            case SEND_TRANSACTION_WC:
            case SEND_TRANSACTION:
            case SEND_TRANSACTION_DAPP:
                //check gas and warn user
                if (!gasWidget.checkSufficientGas())
                {
                    askUserForInsufficientGasConfirm();
                }
                else
                {
                    sendTransaction();
                }
                break;
            case SIGN_MESSAGE:
                signMessage();
                break;
            case SIGN_TRANSACTION:
                signTransaction();
                break;
        }

        actionSheetCallback.notifyConfirm(mode.toString());
    }

    private BigDecimal getTransactionAmount()
    {
        BigDecimal txAmount;
        if (token.isEthereum())
        {
            txAmount = new BigDecimal(gasWidget.getValue());
        }
        else if (isSendingTransaction())
        {
            //Decode tx
            TransactionInput transactionInput = Transaction.decoder.decodeInput(candidateTransaction, token.tokenInfo.chainId, token.getWallet());
            txAmount = new BigDecimal(token.getTransferValueRaw(transactionInput));
        }
        else
        {
            txAmount = BigDecimal.ZERO;
        }

        return txAmount;
    }

    private String getERC721TokenId()
    {
        if (!token.isERC721()) return "";
        TransactionInput transactionInput = Transaction.decoder.decodeInput(candidateTransaction, token.tokenInfo.chainId, token.getWallet());
        return token.getTransferValueRaw(transactionInput).toString();
    }

    private void signMessage()
    {
        //get authentication
        functionBar.setVisibility(View.GONE);

        //authentication screen
        SignAuthenticationCallback localSignCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                actionCompleted = true;
                //display success and hand back to calling function
                if (gotAuth) confirmationWidget.startProgressCycle(1);
                signCallback.gotAuthorisation(gotAuth);
            }

            @Override
            public void cancelAuthentication()
            {
                confirmationWidget.hide();
                signCallback.gotAuthorisation(false);
            }
        };

        actionSheetCallback.getAuthorisation(localSignCallback);
    }

    /**
     * Popup a dialogbox to ask user if they really want to try to send this transaction,
     * as we calculate it will fail due to insufficient gas. User knows best though.
     */
    private void askUserForInsufficientGasConfirm()
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(getContext());
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.insufficient_gas);
        dialog.setMessage(getContext().getString(R.string.not_enough_gas_message));
        dialog.setButtonText(R.string.action_send);
        dialog.setSecondaryButtonText(R.string.cancel_transaction);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
            sendTransaction();
        });
        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    public void transactionWritten(String tx)
    {
        txHash = tx;
        //dismiss on message completion
        confirmationWidget.completeProgressMessage(txHash, this::showTransactionSuccess);
        if (!TextUtils.isEmpty(tx) && tx.startsWith("0x"))
        {
            updateRealmTransactionFinishEstimate(tx);
        }
    }

    private void showTransactionSuccess()
    {
        switch (mode)
        {
            case SEND_TRANSACTION:
                //Display transaction success dialog
                Intent intent = new Intent(getContext(), TransactionSuccessActivity.class);
                intent.putExtra(C.EXTRA_TXHASH, txHash);
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                activity.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
                dismiss();
                break;

            case SEND_TRANSACTION_WC:
            case SEND_TRANSACTION_DAPP:
                //return to dapp
                dismiss();
                break;

            case SIGN_TRANSACTION:
                dismiss();
                break;
        }
    }

    private void updateRealmTransactionFinishEstimate(String txHash)
    {
        final long expectedTime = System.currentTimeMillis() + gasWidget.getExpectedTransactionTime() * 1000;
        try (Realm realm = tokensService.getWalletRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                RealmTransaction rt = r.where(RealmTransaction.class)
                        .equalTo("hash", txHash)
                        .findFirst();

                if (rt != null)
                {
                    rt.setExpectedCompletion(expectedTime);
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    private void setupCancelListeners()
    {
        cancelButton.setOnClickListener(v -> {
            dismiss();
        });

        setOnDismissListener(v -> {
            actionSheetCallback.dismissed(txHash, callbackId, actionCompleted);
        });
    }

    private void signTransaction()
    {
        functionBar.setVisibility(View.GONE);

        //get approval and push transaction
        //authentication screen
        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                actionCompleted = true;
                confirmationWidget.startProgressCycle(4);
                //send the transaction
                actionSheetCallback.signTransaction(formTransaction());
            }

            @Override
            public void cancelAuthentication()
            {
                confirmationWidget.hide();
                functionBar.setVisibility(View.VISIBLE);
            }
        };

        actionSheetCallback.getAuthorisation(signCallback);
    }

    public void completeSignRequest(boolean gotAuth)
    {
        if (signCallback != null)
        {
            actionCompleted = true;

            switch (mode)
            {
                case SEND_TRANSACTION_WC:
                case SEND_TRANSACTION:
                case SEND_TRANSACTION_DAPP:
                    signCallback.gotAuthorisation(gotAuth);
                    break;

                case SIGN_MESSAGE:
                    actionCompleted = true;
                    //display success and hand back to calling function
                    confirmationWidget.startProgressCycle(1);
                    signCallback.gotAuthorisation(gotAuth);
                    break;

                case SIGN_TRANSACTION:
                    signCallback.gotAuthorisation(gotAuth);
                    break;
            }
        }
    }

    private Web3Transaction formTransaction()
    {
        //form Web3Transaction
        //get user gas settings
        return new Web3Transaction(
                candidateTransaction.recipient,
                candidateTransaction.contract,
                gasWidget.getValue(),
                gasWidget.getGasPrice(candidateTransaction.gasPrice),
                gasWidget.getGasLimit(),
                gasWidget.getNonce(),
                candidateTransaction.payload,
                candidateTransaction.leafPosition
        );
    }

    private void sendTransaction()
    {
        functionBar.setVisibility(View.GONE);

        //get approval and push transaction
        //authentication screen
        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                actionCompleted = true;
                if (!gotAuth) { cancelAuthentication(); return; }
                confirmationWidget.startProgressCycle(4);
                //send the transaction
                actionSheetCallback.sendTransaction(formTransaction());
            }

            @Override
            public void cancelAuthentication()
            {
                confirmationWidget.hide();
                functionBar.setVisibility(View.VISIBLE);
            }
        };

        actionSheetCallback.getAuthorisation(signCallback);
    }

    @Override
    public void lockDragging(boolean lock)
    {
        getBehavior().setDraggable(!lock);

        //ensure view fully expanded when locking scroll. Otherwise we may not be able to see our expanded view
        if (lock)
        {
            FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void fullExpand()
    {
        FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void setGasEstimate(BigInteger estimate)
    {
        //String amountVal = BalanceUtils.getScaledValueScientific(new BigDecimal(candidateTransaction.value.add(estimate)), token.tokenInfo.decimals);
        gasWidget.setGasEstimate(estimate);
    }

    private void showAmount(String amountVal)
    {
        String displayStr = getContext().getString(R.string.total_cost, amountVal, token.getSymbol());

        //fetch ticker if required
        if (gasWidget.getValue().compareTo(BigInteger.ZERO) > 0)
        {
            try (Realm realm = tokensService.getTickerRealmInstance())
            {
                RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                        .findFirst();

                if (rtt != null)
                {
                    //calculate equivalent fiat
                    double cryptoRate = Double.parseDouble(rtt.getPrice());
                    double cryptoAmount = Double.parseDouble(amountVal);
                    displayStr = getContext().getString(R.string.fiat_format, amountVal, token.getSymbol(),
                            TickerService.getCurrencyString(cryptoAmount * cryptoRate),
                            rtt.getCurrencySymbol()) ;
                }
            }
            catch (Exception e)
            {
                //
            }
        }

        amountDisplay.setAmountFromString(displayStr);

        BigInteger networkFee = gasWidget.getGasPrice(candidateTransaction.gasPrice).multiply(gasWidget.getGasLimit());
        BigInteger balanceAfterTransaction = token.balance.toBigInteger().subtract(gasWidget.getValue());
        balanceDisplay.setNewBalanceText(token, getTransactionAmount(), networkFee, balanceAfterTransaction);
    }

    private boolean isAttached;
    public void closingActionSheet()
    {
        isAttached = false;
    }

    public void success()
    {
        if (!activity.isFinishing() && !activity.isDestroyed() && isAttached)
        {
            confirmationWidget.completeProgressMessage(".", this::dismiss);
        }
    }
}
