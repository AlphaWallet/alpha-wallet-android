package com.wallet.crypto.trust.views;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.controller.OnTaskCompleted;

public class ImportAccountActivity extends AppCompatActivity {

    private EditText mKeystore;
    private EditText mPassword;
    private Controller mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_import));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mController = Controller.get();

        mKeystore = (EditText) findViewById(R.id.import_keystore);
        mPassword = (EditText) findViewById(R.id.import_password);

        Button mImportButton = (Button) findViewById(R.id.import_account_button);
        mImportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.clickImport(
                    mKeystore.getText().toString(),
                    mPassword.getText().toString(),
                    new OnTaskCompleted() {
                        @Override
                        public void onTaskCompleted() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ImportAccountActivity.this.finish();
                                }
                            });
                        }
                    }
                );
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
