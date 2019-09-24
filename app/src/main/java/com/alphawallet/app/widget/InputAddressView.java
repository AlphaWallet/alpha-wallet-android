package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.ui.zxing.QRScanningActivity;

import com.alphawallet.app.R;

public class InputAddressView extends LinearLayout {
    public static final int BARCODE_READER_REQUEST_CODE = 1;
    private Context context;
    private ImageView scanQrIcon;
    private TextView error;
    private EditText address;


    public InputAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        inflate(context, R.layout.item_input_address, this);

        scanQrIcon = findViewById(R.id.img_scan_qr);
        error = findViewById(R.id.to_address_error);
        address = findViewById(R.id.edit_to_address);

        scanQrIcon.setOnClickListener(v -> {
            Intent intent = new Intent(context, QRScanningActivity.class);
            ((Activity) context).startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
//            Intent intent = new Intent(context, BarcodeCaptureActivity.class);
//            ((Activity) context).startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });
    }

    public void addTextChangedListener(TextWatcher watcher) {
        this.address.addTextChangedListener(watcher);
    }

    public String getAddress() {
        return this.address.getText().toString();
    }

    public void setAddress(CharSequence addressString) {
        this.address.setText(addressString);
    }

    public void setError(int resId) {
        if (resId == R.string.empty) {
            error.setText(resId);
            error.setVisibility(View.GONE);
        } else {
            error.setText(resId);
            error.setVisibility(View.VISIBLE);
        }
    }

    public void setError(CharSequence message) {
        if (message.toString().isEmpty()) {
            error.setText(message);
            error.setVisibility(View.GONE);
        } else {
            error.setText(message);
            error.setVisibility(View.VISIBLE);
        }
    }
}
