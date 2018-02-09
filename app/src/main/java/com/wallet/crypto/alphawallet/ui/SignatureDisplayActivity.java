package com.wallet.crypto.alphawallet.ui;

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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.SignaturePair;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.util.KeyboardUtils;
import com.wallet.crypto.alphawallet.viewmodel.SignatureDisplayModel;
import com.wallet.crypto.alphawallet.viewmodel.SignatureDisplayModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

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
    private TextView selected;
    private TextInputLayout amountInputLayout;
    private TextView selection;

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

        inviteUserToAddIDs();

        name = findViewById(R.id.textViewName);
        ids = findViewById(R.id.textViewIDs);
        idsText = findViewById(R.id.send_ids);
        selection = findViewById(R.id.textViewSelection);
        amountInputLayout = findViewById(R.id.amount_input_layout);

        name.setText(ticket.ticketInfo.name);
        ids.setText(ticket.ticketInfo.populateIDs(ticket.balanceArray, false));

        viewModel = ViewModelProviders.of(this, signatureDisplayModelFactory)
                .get(SignatureDisplayModel.class);
        viewModel.signature().observe(this, this::onSignatureChanged);
        viewModel.ticket().observe(this, this::onTicket);
        viewModel.selection().observe(this, this::onSelected);

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

        idsText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                {
                    if (keyEvent.getKeyCode() == keyEvent.KEYCODE_ENTER)
                    {
                        final String balanceArray = idsText.getText().toString();
                        viewModel.generateNewSelection(balanceArray);
                    }
                }

                return true;
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

    private void inviteUserToAddIDs()
    {
        final Bitmap qrCode = createQRImage(wallet.address);
        ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);
        findViewById(R.id.qr_image).setAlpha(0.1f);

        TextView tv = findViewById(R.id.textAddIDs);
        tv.setVisibility(View.VISIBLE);
    }

    private void onSignatureChanged(SignaturePair sigPair) {
        try
        {
            if (sigPair.selectionStr == null) return;
            String qrMessage = sigPair.selectionStr + sigPair.signatureStr;
            final Bitmap qrCode = createQRImage(qrMessage);
            ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);
            findViewById(R.id.qr_image).setAlpha(1.0f);
            findViewById(R.id.textAddIDs).setVisibility(View.GONE);
        }
        catch (Exception e)
        {

        }
    }

    private void onTicket(Ticket ticket) {
        name.setText(ticket.tokenInfo.name);
        String idStr = ticket.tokenInfo.populateIDs(ticket.getValidIndicies(), false);
        ids.setText(idStr);

        //check current list of IDs is still valid
        String currentList = selection.getText().toString();
        if (currentList.length() > 0) {
            String correctList = ticket.checkBalance(currentList);

            if (!correctList.equals(currentList))
            {
                // ticket was burned, used or transferred
                selection.setText(correctList);
                idsText.setText(correctList);
                viewModel.newBalanceArray(correctList);
                if (correctList.length() == 0) {
                    inviteUserToAddIDs();
                }
            }
        }
    }

    private void onSelected(String selectionStr)
    {
        selection.setText(selectionStr);
        try
        {
            //dismiss soft keyboard
            KeyboardUtils.hideKeyboard(idsText);
            if (selectionStr == null || selectionStr.length() == 0)
            {
                inviteUserToAddIDs();
            }
        }
        catch (Exception e)
        {

        }
    }
}
