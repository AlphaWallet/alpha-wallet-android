package io.awallet.crypto.alphawallet.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.FrameLayout;

import io.awallet.crypto.alphawallet.R;

public class BackupView extends FrameLayout {
    private EditText password;

    public BackupView(@NonNull Context context) {
        super(context);

        init();
    }

    private void init() {
        LayoutInflater.from(getContext())
                .inflate(R.layout.layout_dialog_backup, this, true);
        password = findViewById(R.id.password);
    }

    public String getPassword() {
        return password.getText().toString();
    }

    public void showKeyBoard() {
        password.requestFocus();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        showKeyBoard();
    }
}
