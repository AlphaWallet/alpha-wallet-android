package com.alphawallet.app.widget;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SignDialogViewModel;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.hardware.SignatureReturnType;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by JB on 20/11/2022.
 */
public class ActionSheetSignDialog extends ActionSheet implements StandardFunctionInterface, SignAuthenticationCallback
{
    private final SignDialogViewModel viewModel;
    private final BottomSheetToolbarView toolbar;
    private final ConfirmationWidget confirmationWidget;
    private final AddressDetailView requesterDetail;
    private final AddressDetailView addressDetail;
    private final FunctionButtonBar functionBar;
    private final ActionSheetCallback actionSheetCallback;
    private final Activity activity;
    private final long callbackId;
    private boolean actionCompleted;
    private WalletType walletType;

    public ActionSheetSignDialog(@NonNull Activity callingActivity, ActionSheetCallback aCallback, Signable message)
    {
        super(callingActivity);
        View view = View.inflate(callingActivity, R.layout.dialog_action_sheet_sign, null);
        setContentView(view);
        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        confirmationWidget = findViewById(R.id.confirmation_view);
        requesterDetail = findViewById(R.id.requester);
        addressDetail = findViewById(R.id.wallet);
        functionBar = findViewById(R.id.layoutButtons);
        callbackId = message.getCallbackId();
        activity = callingActivity;

        actionSheetCallback = aCallback;

        requesterDetail.setupRequester(message.getOrigin());
        SignDataWidget signWidget = findViewById(R.id.sign_widget);
        signWidget.setupSignData(message);

        toolbar.setTitle(Utils.getSigningTitle(message));

        setupCancelListeners();
        actionCompleted = false;

        //ensure view fully expanded when locking scroll. Otherwise we may not be able to see our expanded view
        fullExpand();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(SignDialogViewModel.class);
        viewModel.completed().observe((LifecycleOwner) activity, this::signComplete);
        viewModel.message().observe((LifecycleOwner) activity, this::onMessage);
        viewModel.onWallet().observe((LifecycleOwner) activity, this::onWallet);

        setCanceledOnTouchOutside(false);
    }

    private void onWallet(Wallet wallet)
    {
        walletType = wallet.type;
        if (walletType == WalletType.HARDWARE)
        {
            functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.use_hardware_card)));
            functionBar.setClickable(false);
            //push signing request
            viewModel.getAuthentication(activity, this);
        }
        else
        {
            functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        }

        functionBar.revealButtons();
    }

    @Override
    public void setIcon(String icon)
    {
        ImageView iconView = findViewById(R.id.logo);
        Glide.with(activity)
                .load(icon)
                .circleCrop()
                .into(iconView);
    }

    @Override
    public void handleClick(String action, int id)
    {
        if (walletType == WalletType.HARDWARE)
        {
            //TODO: Hardware - popup to tell user to apply hardware card
            return;
        }

        //get authentication
        functionBar.setVisibility(View.GONE);
        viewModel.getAuthentication(activity, this);
    }

    // Set for locked signing account, which WalletConnect v2 requires
    @Override
    public void setSigningWallet(String account)
    {
        viewModel.setSigningWallet(account);
        addressDetail.setVisibility(View.VISIBLE);
        addressDetail.setupAddress(account, "", null);
    }

    private void onMessage(Pair<Integer, Integer> res)
    {
        addressDetail.addMessage(getContext().getString(res.first), res.second);
    }

    public void success()
    {
        if (isShowing() && confirmationWidget != null && confirmationWidget.isShown())
        {
            confirmationWidget.completeProgressMessage(".", this::dismiss);
        }
    }

    private void setupCancelListeners()
    {
        toolbar.setCloseListener(v -> dismiss());

        setOnDismissListener(v -> {
            actionSheetCallback.dismissed("", callbackId, actionCompleted);
        });
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        final SignDataWidget signWidget = findViewById(R.id.sign_widget);
        if (gotAuth)
        {
            if (walletType != WalletType.HARDWARE)
            {
                //start animation
                confirmationWidget.startProgressCycle(1);
                actionSheetCallback.notifyConfirm(ActionSheetMode.SIGN_MESSAGE.getValue());
            }

            viewModel.signMessage(signWidget.getSignable(), actionSheetCallback);
        }
        else
        {
            Toast.makeText(activity, activity.getString(R.string.error_while_signing_transaction), Toast.LENGTH_SHORT).show();
            cancelAuthentication();
        }
    }

    @Override
    public void cancelAuthentication()
    {
        dismiss();
    }

    @Override
    public void gotSignature(SignatureFromKey signature)
    {
        if (signature.sigType == SignatureReturnType.SIGNATURE_GENERATED)
        {
            functionBar.setVisibility(View.GONE);
            confirmationWidget.startProgressCycle(1);
            actionSheetCallback.notifyConfirm(ActionSheetMode.SIGN_MESSAGE.getValue());
            //actionSheetCallback.completeSendTransaction(signature);
            viewModel.completeSignMessage(signature, actionSheetCallback);
        }
        else
        {
            //TODO: Hardware - report error in a better way
            activity.runOnUiThread(() -> Toast.makeText(activity, "ERROR: " + signature.failMessage, Toast.LENGTH_SHORT).show());
        }
    }

    private void signComplete(Boolean success)
    {
        if (success)
        {
            actionCompleted = true;
            success();
        }
        else
        {
            dismiss();
        }
    }
}
