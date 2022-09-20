package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.widget.AWBottomSheetDialog;

public class TestNetTipsHolder extends BinderViewHolder<Boolean>
{
    public static final int VIEW_TYPE = 1001;
    private final TextView whereAreTokens;
    private TokensAdapterCallback tokenAdapterCallback;
    private AWBottomSheetDialog dialog;

    public TestNetTipsHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        whereAreTokens = findViewById(R.id.where_are_tokens);
    }

    @Override
    public void bind(@Nullable Boolean isMainNetActive, @NonNull Bundle addition)
    {
        whereAreTokens.setOnClickListener(this::showDialog);
    }

    private void showDialog(View view)
    {
        if (dialog == null)
        {
            dialog = createDialog();
        }

        if (!dialog.isShowing())
        {
            dialog.show();
        }
    }

    private AWBottomSheetDialog createDialog()
    {
        AWBottomSheetDialog dialog = new AWBottomSheetDialog(getContext(), new AWBottomSheetDialog.Callback()
        {
            @Override
            public void onClosed()
            {

            }

            @Override
            public void onConfirmed()
            {
                tokenAdapterCallback.onSwitchClicked();
            }

            @Override
            public void onCancelled()
            {

            }
        });
        dialog.setTitle(getString(R.string.title_dialog_where_are_tokens));
        dialog.setContent(getString(R.string.content_dialog_where_are_tokens));
        dialog.setConfirmButton(getString(R.string.button_switch_to_mainnet));
        return dialog;
    }

    @Override
    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback)
    {
        this.tokenAdapterCallback = tokensAdapterCallback;
    }
}
