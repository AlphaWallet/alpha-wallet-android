package com.wallet.crypto.trustapp.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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

    private Controller mController;

    private String mAddress;
    private EditText mPasswordText;
    private Button mExportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAddress = getIntent().getStringExtra(getString(R.string.address_keyword));

        getSupportActionBar().setTitle(getString(R.string.action_export) + ": " + mAddress.substring(0, 5));

        mController = Controller.get();

        mPasswordText = (EditText) findViewById(R.id.export_password);
        mExportButton = (Button) findViewById(R.id.export_account_button);
        mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String keystoreJson = mController.clickExportAccount(ExportAccountActivity.this, mAddress, mPasswordText.getText().toString());
                if (keystoreJson.isEmpty()) {
                    Toast.makeText(ExportAccountActivity.this, "Unable to export", Toast.LENGTH_SHORT).show();
                } else {
                    mController.shareKeystore(ExportAccountActivity.this, keystoreJson);
                }
                finish();
            }
        });
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
