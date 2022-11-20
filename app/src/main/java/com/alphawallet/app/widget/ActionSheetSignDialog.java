package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.Signable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by JB on 20/11/2022.
 */
public class ActionSheetSignDialog extends ActionSheet implements StandardFunctionInterface, ActionSheetInterface
{
    private final BottomSheetToolbarView toolbar;
    private final ConfirmationWidget confirmationWidget;
    private final AddressDetailView addressDetail;
    private final FunctionButtonBar functionBar;
    private final SignAuthenticationCallback signCallback;
    private final ActionSheetCallback actionSheetCallback;
    private final long callbackId;
    private boolean actionCompleted;

    public ActionSheetSignDialog(@NonNull Activity activity, ActionSheetCallback aCallback, SignAuthenticationCallback sCallback, Signable message)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet_sign);

        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        confirmationWidget = findViewById(R.id.confirmation_view);
        addressDetail = findViewById(R.id.requester);
        functionBar = findViewById(R.id.layoutButtons);
        callbackId = message.getCallbackId();

        actionSheetCallback = aCallback;
        signCallback = sCallback;

        addressDetail.setupRequester(message.getOrigin());
        SignDataWidget signWidget = findViewById(R.id.sign_widget);
        signWidget.setupSignData(message);
        signWidget.setLockCallback(this);

        toolbar.setTitle(Utils.getSigningTitle(message));

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();
        setupCancelListeners();
        actionCompleted = false;
    }

    @Override
    public void handleClick(String action, int id)
    {
        //get authentication
        functionBar.setVisibility(View.GONE);

        //authentication screen
        SignAuthenticationCallback localSignCallback = new SignAuthenticationCallback()
        {
            final SignDataWidget signWidget = findViewById(R.id.sign_widget);

            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                actionCompleted = true;
                //display success and hand back to calling function
                if (gotAuth)
                {
                    confirmationWidget.startProgressCycle(1);
                    signCallback.gotAuthorisationForSigning(gotAuth, signWidget.getSignable());
                    actionSheetCallback.notifyConfirm(ActionSheetMode.SIGN_MESSAGE.getValue());
                }
                else
                {
                    cancelAuthentication();
                }
            }

            @Override
            public void cancelAuthentication()
            {
                confirmationWidget.hide();
                signCallback.gotAuthorisationForSigning(false, signWidget.getSignable());
            }
        };

        actionSheetCallback.getAuthorisation(localSignCallback);
    }

    public void setURL(String url)
    {
        addressDetail.setupRequester(url);
    }

    @Override
    public void lockDragging(boolean lock)
    {
        getBehavior().setDraggable(!lock);

        //ensure view fully expanded when locking scroll. Otherwise we may not be able to see our expanded view
        if (lock)
        {
            FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(STATE_EXPANDED);
        }
    }

    @Override
    public void fullExpand()
    {
        FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(STATE_EXPANDED);
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
}
