package com.wallet.crypto.trust.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.model.VMAccount;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class ReceiveActivity extends AppCompatActivity {

    final static String ETHEREUM_PREFIX = "ethereum:";

    ImageView imageView;
    Button generateButton;
    Button copyButton;
    TextView addressTextView;
    String AddressTextValue;
    Thread thread ;
    public final static int QRcodeWidth = 200 ;
    Bitmap bitmap ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        imageView = (ImageView)findViewById(R.id.imageView);
        addressTextView = (TextView)findViewById(R.id.addressTextView);
        generateButton = (Button)findViewById(R.id.generate_button);
        copyButton = findViewById(R.id.copy_button);

        VMAccount account = Controller.get().getCurrentAccount();

        addressTextView.setText(account.getAddress());

        copyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.address_keyword), addressTextView.getText().toString());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(ReceiveActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        generateButton.setOnClickListener(new View.OnClickListener() {
                              @Override
                              public void onClick(View view) {

                AddressTextValue = addressTextView.getText().toString();

                try {
                    bitmap = TextToImageEncode(ETHEREUM_PREFIX + AddressTextValue);

                    imageView.setImageBitmap(bitmap);

                } catch (WriterException e) {
                    e.printStackTrace();
                }

        }
    });
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
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.QRCodeBlackColor):getResources().getColor(R.color.QRCodeWhiteColor);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, QRcodeWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);
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
}
