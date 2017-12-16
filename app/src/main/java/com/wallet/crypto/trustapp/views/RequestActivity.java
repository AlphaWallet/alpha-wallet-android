package com.wallet.crypto.trustapp.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.model.VMAccount;

public class RequestActivity extends AppCompatActivity {

    private static final String TAG = "REQUEST_ACTIVITY";
    private static final double QR_CODE_WIDTH_RATIO = 0.9;

    ImageView imageView;
    Button copyButton;
    TextView addressTextView;
    private int QRcodeWidth = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.title_request));
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        QRcodeWidth = (int) (size.x * QR_CODE_WIDTH_RATIO);
        Log.d("QR WIDTH", Integer.toString(QRcodeWidth));

        imageView = (ImageView)findViewById(R.id.imageView);
        addressTextView = (TextView)findViewById(R.id.addressTextView);
        copyButton = findViewById(R.id.copy_button);

        VMAccount account = Controller.with(getApplicationContext()).getCurrentAccount();

        addressTextView.setText(account.getAddress());

        copyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(Controller.KEY_ADDRESS, addressTextView.getText().toString());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(RequestActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            final Bitmap qrCode = TextToImageEncode(Controller.with(this).getCurrentAccount().getAddress());
            imageView.setImageBitmap(qrCode);
        } catch(Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }

        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

        return bitmap;
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

    private class GenerateQRCodeTask extends AsyncTask<Void,Void,Void> {
        String value;

        public GenerateQRCodeTask(String value) {
            this.value = value;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                final Bitmap qrCode = TextToImageEncode(value);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(qrCode);
                    }
                });
            } catch (WriterException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }
}
