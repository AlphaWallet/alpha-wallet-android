package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.content.Context;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.ActionSheetStatus;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Created by JB on 20/11/2022.
 */
public abstract class ActionSheet extends BottomSheetDialog implements ActionSheetInterface
{
    private ActionSheetStatus actionSheetStatus = ActionSheetStatus.OK;

    public ActionSheet(@NonNull Context context)
    {
        super(context);
    }

    public void forceDismiss()
    {
        setOnDismissListener(v -> {
            // Do nothing
        });
        dismiss();
    }

    public void fullExpand()
    {
        FrameLayout bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).setState(STATE_EXPANDED);
    }

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

    public void gotAuthorisation(boolean gotAuth)
    {

    }

    public void setActionSheetStatus(ActionSheetStatus actionSheetStatus)
    {
        this.actionSheetStatus = actionSheetStatus;
    }

    public ActionSheetStatus getActionSheetStatus()
    {
        return this.actionSheetStatus;
    }
}
