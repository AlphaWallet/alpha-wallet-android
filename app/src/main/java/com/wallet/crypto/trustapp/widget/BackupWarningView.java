package com.wallet.crypto.trustapp.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.OnBackupClickListener;


public class BackupWarningView extends FrameLayout implements View.OnClickListener {

    private OnBackupClickListener onPositiveClickListener;
    private OnBackupClickListener onNegativeClickListener;
    private Wallet wallet;

    public BackupWarningView(@NonNull Context context) {
        this(context, null);
    }

    public BackupWarningView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackupWarningView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(R.layout.layout_dialog_warning_backup);
    }

    private void init(@LayoutRes int layoutId) {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.backup_action).setOnClickListener(this);
        findViewById(R.id.later_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backup_action: {
                if (onPositiveClickListener != null) {
                    onPositiveClickListener.onBackupClick(v, wallet);
                }
            } break;
            case
            R.id.later_action: {
                if (onNegativeClickListener != null) {
                    onNegativeClickListener.onBackupClick(v, wallet);
                }
            }
        }
    }

    public void setOnNegativeClickListener(OnBackupClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
    }

    public void setOnPositiveClickListener(OnBackupClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
    }

    public void show(Wallet wallet) {
        setVisibility(VISIBLE);
        this.wallet = wallet;
    }

    public void hide() {
        setVisibility(GONE);
    }
}
