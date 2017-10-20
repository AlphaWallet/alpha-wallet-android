package com.wallet.crypto.trust.views;

import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.controller.OnTaskCompleted;
import com.wallet.crypto.trust.controller.TaskResult;
import com.wallet.crypto.trust.controller.TaskStatus;
import com.wallet.crypto.trust.controller.Utils;
import com.wallet.crypto.trust.model.VMAccount;
import com.wallet.crypto.trust.views.barcode.BarcodeCaptureActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {

    private Controller mController;

    private Spinner mFromSpinner;
    private EditText mTo;
    private EditText mAmount;
    private EditText mPassword;
    private static final String LOG_TAG = SendActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private TextView mResultTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mController = Controller.get();

        List<VMAccount> accounts = mController.getAccounts();

        mTo = (EditText) findViewById(R.id.date);
        mAmount = (EditText) findViewById(R.id.amount);
        mPassword = (EditText) findViewById(R.id.password);

        String toAddress = getIntent().getStringExtra(getString(R.string.address_keyword));
        if (toAddress != null) {
            mTo.setText(toAddress);
        }

        Button mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.clickSend(
                    SendActivity.this,
                    mController.getCurrentAccount().getAddress(),
                    mTo.getText().toString(),
                    mAmount.getText().toString(), mPassword.getText().toString(),
                    new OnTaskCompleted() {
                        public void onTaskCompleted(final TaskResult result) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (result.getStatus() == TaskStatus.SUCCESS) {
                                        SendActivity.this.finish();
                                    }
                                    Toast.makeText(SendActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                );
            }
        });

        mResultTextView = (TextView) findViewById(R.id.result_textview);

        ImageButton scanBarcodeButton = (ImageButton) findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    String extracted_address = Utils.extractAddressFromQrString(barcode.displayValue);
                    if (extracted_address == null) {
                        Toast.makeText(this, "QR code doesn't contain account address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Point[] p = barcode.cornerPoints;
                    mTo.setText(extracted_address);
                } else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
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
