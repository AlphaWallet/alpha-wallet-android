package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
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
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.viewmodel.RedeemSignatureDisplayModel;
import com.wallet.crypto.alphawallet.viewmodel.RedeemSignatureDisplayModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.TICKET_RANGE;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 24/01/2018.
 */

public class RedeemSignatureDisplayActivity extends BaseActivity implements View.OnClickListener {
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected RedeemSignatureDisplayModelFactory redeemSignatureDisplayModelFactory;
    private RedeemSignatureDisplayModel viewModel;
    private SystemView systemView;

    private Wallet wallet;
    private Ticket ticket;
    private TicketRange ticketRange;

    TicketAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotating_signature);
        toolbar();

        setTitle(getString(R.string.empty));

        ticket = getIntent().getParcelableExtra(TICKET);
        wallet = getIntent().getParcelableExtra(WALLET);
        ticketRange = getIntent().getParcelableExtra(TICKET_RANGE);
        findViewById(R.id.advanced_options).setVisibility(View.GONE); //setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, redeemSignatureDisplayModelFactory)
                .get(RedeemSignatureDisplayModel.class);
        viewModel.signature().observe(this, this::onSignatureChanged);
        viewModel.ticket().observe(this, this::onTicket);
        viewModel.selection().observe(this, this::onSelected);

        ticketBurnNotice();
        TextView tv = findViewById(R.id.textAddIDs);
        tv.setText(getString(R.string.waiting_for_blockchain));
        tv.setVisibility(View.VISIBLE);

        setupTicket();
    }

    private void setupTicket() {
        TextView textAmount = findViewById(R.id.amount);
        TextView textTicketName = findViewById(R.id.name);
        TextView textVenue = findViewById(R.id.venue);
        TextView textDate = findViewById(R.id.date);
        TextView textRange = findViewById(R.id.tickettext);
        TextView textCat = findViewById(R.id.cattext);

        int numberOfTickets = ticketRange.tokenIds.size();
        if (numberOfTickets > 0) {
            Integer firstTicket = ticketRange.tokenIds.get(0);
            Integer lastTicket = ticketRange.tokenIds.get(numberOfTickets-1);

            String ticketTitle = ticket.getFullName();
            String venue = TicketDecode.getVenue(firstTicket);
            String date = TicketDecode.getDate(firstTicket);
            int rangeFirst = TicketDecode.getSeatIdInt(firstTicket);
            int rangeLast = TicketDecode.getSeatIdInt(lastTicket);
            String cat = TicketDecode.getZone(firstTicket);
            String seatCount = String.format(Locale.getDefault(), "x%d", numberOfTickets);

            textAmount.setText(seatCount);
            textTicketName.setText(ticketTitle);
            textVenue.setText(venue);
            textDate.setText(date);
            textRange.setText(rangeFirst + "-" + rangeLast);
            textCat.setText(cat);
        }
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
        viewModel.prepare(ticket.tokenInfo.address, ticket, ticketRange);
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

    private void ticketBurnNotice()
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
            if (sigPair == null || !sigPair.isValid())
            {
                ticketBurnNotice();
            }
            else
            {
                String qrMessage = sigPair.formQRMessage();
                final Bitmap qrCode = createQRImage(qrMessage);
                ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qrCode);
                findViewById(R.id.qr_image).setAlpha(1.0f);
                findViewById(R.id.textAddIDs).setVisibility(View.GONE);
            }
        }
        catch (Exception e)
        {

        }
    }

    private void onTicket(Ticket ticket)
    {
        String idStr = ticket.populateIDs(ticket.getValidIndicies(), false);
//        ids.setText(idStr);
    }

    private void onSelected(String selectionStr)
    {
//        selection.setText(selectionStr);
//        try
//        {
//            //dismiss soft keyboard
//            KeyboardUtils.hideKeyboard(idsText);
//        }
//        catch (Exception e)
//        {
//
//        }
    }
}