package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Address;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.SignaturePair;
import com.wallet.crypto.trustapp.entity.Ticket;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.viewmodel.SignatureDisplayModel;
import com.wallet.crypto.trustapp.viewmodel.SignatureDisplayModelFactory;
import com.wallet.crypto.trustapp.viewmodel.UseTokenViewModel;
import com.wallet.crypto.trustapp.widget.SystemView;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import android.util.Base64;

import static com.wallet.crypto.trustapp.C.Key.TICKET;
import static com.wallet.crypto.trustapp.C.Key.WALLET;

/**
 * Created by James on 24/01/2018.
 */

public class SignatureDisplayActivity extends BaseActivity implements View.OnClickListener {
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected SignatureDisplayModelFactory signatureDisplayModelFactory;
    private SignatureDisplayModel viewModel;
    private SystemView systemView;

    public TextView name;
    public TextView ids;
    private EditText idsText;
    private TextInputLayout amountInputLayout;

    private Wallet wallet;
    private Ticket ticket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotating_signature);
        toolbar();

        ticket = getIntent().getParcelableExtra(TICKET);
        wallet = getIntent().getParcelableExtra(WALLET);
        findViewById(R.id.advanced_options).setOnClickListener(this);
        final Bitmap qrCode = createQRImage(wallet.address);
        ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);

        name = findViewById(R.id.textViewName);
        ids = findViewById(R.id.textViewIDs);
        idsText = findViewById(R.id.send_ids);
        amountInputLayout = findViewById(R.id.amount_input_layout);

        name.setText(ticket.ticketInfo.name);
        ids.setText(ticket.ticketInfo.populateIDs(ticket.balanceArray, false));

        viewModel = ViewModelProviders.of(this, signatureDisplayModelFactory)
                .get(SignatureDisplayModel.class);
        viewModel.signature().observe(this, this::onSignatureChanged);
        viewModel.ticket().observe(this, this::onTicket);

        idsText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final String balanceArray = idsText.getText().toString();
                //convert to an index array
                viewModel.newBalanceArray(balanceArray);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private Bitmap createQRImage(String address) {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int imageSize = (int) (size.x * QR_IMAGE_WIDTH_RATIO);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    address,
                    BarcodeFormat.QR_CODE,
                    imageSize,
                    imageSize,
                    null);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_fail_generate_qr), Toast.LENGTH_SHORT)
                    .show();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(ticket.ticketInfo.address);
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, wallet.address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void onSignatureChanged(SignaturePair sigPair) {
        try
        {
            ByteArrayOutputStream qrMessage = new ByteArrayOutputStream();
            qrMessage.write(sigPair.selection);
            qrMessage.write(sigPair.signature);
            byte[] sigBytes = Base64.encode(qrMessage.toByteArray(), Base64.DEFAULT);
            String sig = new String(sigBytes);
            final Bitmap qrCode = createQRImage(sig);
            ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);
        }
        catch (Exception e)
        {

        }
    }

    private void onTicket(Ticket ticket) {
        name.setText(ticket.tokenInfo.name);
        String idStr = ticket.tokenInfo.populateIDs(ticket.balanceArray, false);
        ids.setText(idStr);
    }
}
