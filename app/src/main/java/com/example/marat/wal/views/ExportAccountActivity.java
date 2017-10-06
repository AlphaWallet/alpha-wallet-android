package com.example.marat.wal.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.marat.wal.R;
import com.example.marat.wal.controller.Controller;

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

        mAddress = getIntent().getStringExtra(getString(R.string.address_key));

        getSupportActionBar().setTitle(getString(R.string.action_export) + mAddress);

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
                    showKeystore(keystoreJson);
                }
            }
        });
    }

    private void showKeystore(final String keystoreJson) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(keystoreJson)
                .setTitle(getString(R.string.message_save_this));

        // Add the buttons
        builder.setPositiveButton(R.string.copy, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.keystore_keyword), keystoreJson);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(ExportAccountActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
