package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.alphawallet.app.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

public class SignMethodDialog extends BottomSheetDialog
{
    public SignMethodDialog(@NonNull Context context)
    {
        super(context, R.style.FullscreenBottomSheetDialogStyle);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sign_method, null);
        setContentView(view);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }
}
