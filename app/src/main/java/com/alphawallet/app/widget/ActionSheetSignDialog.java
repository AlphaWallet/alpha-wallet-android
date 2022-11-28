package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SignDialogViewModel;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

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
    private final AddressDetailView addressDetail;
    private final FunctionButtonBar functionBar;
    private final ActionSheetCallback actionSheetCallback;
    private final Activity activity;
    private final long callbackId;
    private boolean actionCompleted;

    public ActionSheetSignDialog(@NonNull Activity callingActivity, ActionSheetCallback aCallback, Signable message)
    {
        super(callingActivity);
        View view = LayoutInflater.from(callingActivity).inflate(R.layout.dialog_action_sheet_sign, null);
        setContentView(view);

        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        confirmationWidget = findViewById(R.id.confirmation_view);
        addressDetail = findViewById(R.id.requester);
        functionBar = findViewById(R.id.layoutButtons);
        callbackId = message.getCallbackId();
        activity = callingActivity;

        actionSheetCallback = aCallback;

        addressDetail.setupRequester(message.getOrigin());
        SignDataWidget signWidget = findViewById(R.id.sign_widget);
        signWidget.setupSignData(message);

        toolbar.setTitle(Utils.getSigningTitle(message));

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();
        setupCancelListeners();
        actionCompleted = false;

        //ensure view fully expanded when locking scroll. Otherwise we may not be able to see our expanded view
        fullExpand();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(SignDialogViewModel.class);
        viewModel.completed().observe((LifecycleOwner) activity, this::signComplete);
        setCanceledOnTouchOutside(false);
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
        //get authentication
        functionBar.setVisibility(View.GONE);
        viewModel.getAuthentication(activity, this);
    }

    // Set for locked signing account, which WalletConnect v2 requires
    @Override
    public void setSigningWallet(String account)
    {
        viewModel.setSigningWallet(account);
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
            //start animation
            confirmationWidget.startProgressCycle(1);
            actionSheetCallback.notifyConfirm(ActionSheetMode.SIGN_MESSAGE.getValue());
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
