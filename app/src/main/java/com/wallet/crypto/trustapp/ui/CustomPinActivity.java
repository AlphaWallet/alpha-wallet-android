package com.wallet.crypto.trustapp.ui;

import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.omadahealth.lollipin.lib.managers.AppLockActivity;

public class CustomPinActivity extends AppLockActivity {

    @Override
    public void showForgotDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Warning")
                .setMessage("Please get in touch with the devs.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onPinFailure(int attempts) {

    }

    @Override
    public void onPinSuccess(int attempts) {

    }

    @Override
    public int getPinLength() {
        return super.getPinLength();
    }
}
