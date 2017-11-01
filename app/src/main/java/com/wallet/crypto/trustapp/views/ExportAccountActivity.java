package com.wallet.crypto.trustapp.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;

public class ExportAccountActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 1;
    private Controller mController;

    private String mAddress;
    private EditText mPasswordText;
    private EditText mConfirmPasswordText;
    private Button mExportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_account);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAddress = getIntent().getStringExtra(getString(R.string.address_keyword));

        getSupportActionBar().setTitle(getString(R.string.title_backup) + ": " + mAddress.substring(0, 5) + "...");

        mController = Controller.get();

        mPasswordText = findViewById(R.id.export_password);
        mConfirmPasswordText = findViewById(R.id.confirm_password);
        mExportButton = findViewById(R.id.export_account_button);
        mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String pwd = mPasswordText.getText().toString();
                if (!isPasswordLongEnough(pwd)) {
                    mPasswordText.setError(String.format(getString(R.string.min_pwd_length), MIN_PASSWORD_LENGTH));
                }

                final String pwdConfirm = mConfirmPasswordText.getText().toString();
                if (!isPasswordLongEnough(pwdConfirm)) {
                    mConfirmPasswordText.setError(String.format(getString(R.string.min_pwd_length), MIN_PASSWORD_LENGTH));
                } else if (!pwd.equals(pwdConfirm)) {
                    mConfirmPasswordText.setError(getString(R.string.error_passwords_must_match));
                }

                if (!isPasswordLongEnough(pwd) || !isPasswordLongEnough(pwdConfirm) || !pwd.equals(pwdConfirm)) {
                    return;
                }

                String keystoreJson = mController.clickExportAccount(ExportAccountActivity.this, mAddress, mPasswordText.getText().toString());
                if (keystoreJson.isEmpty()) {
                    Toast.makeText(ExportAccountActivity.this, "Unable to export", Toast.LENGTH_SHORT).show();
                } else {
                    mController.shareKeystore(ExportAccountActivity.this, keystoreJson);
                }
            }
        });
    }

    boolean isPasswordLongEnough(String password) {
        return password.length() >= MIN_PASSWORD_LENGTH;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Controller.SHARE_RESULT) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
