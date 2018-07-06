package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

import java.text.DateFormat;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.SignaturePair;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.TicketAdapter;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.wallet.viewmodel.RedeemSignatureDisplayModel;
import io.stormbird.wallet.viewmodel.RedeemSignatureDisplayModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.TICKET_RANGE;
import static io.stormbird.wallet.C.Key.WALLET;
import static io.stormbird.wallet.C.PRUNE_ACTIVITY;

/**
 * Created by James on 24/01/2018.
 */

public class RedeemSignatureDisplayActivity extends BaseActivity implements View.OnClickListener
{
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected RedeemSignatureDisplayModelFactory redeemSignatureDisplayModelFactory;
    private RedeemSignatureDisplayModel viewModel;
    private SystemView systemView;

    private FinishReceiver finishReceiver;

    private Wallet wallet;
    private Ticket ticket;
    private TicketRangeParcel ticketRange;

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
        viewModel.burnNotice().observe(this, this::onBurned);

        ticketBurnNotice();
        TextView tv = findViewById(R.id.textAddIDs);
        tv.setText(getString(R.string.waiting_for_blockchain));
        tv.setVisibility(View.VISIBLE);

        View baseView = findViewById(android.R.id.content);

        ticket.displayTicketHolder(ticketRange.range, baseView, viewModel.getAssetDefinitionService(), getBaseContext());
        finishReceiver = new FinishReceiver(this);
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
        viewModel.prepare(ticket.tokenInfo.address, ticket, ticketRange.range);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
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
    }

    private void onBurned(Boolean burn)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.ticket_redeemed);
        dialog.setIcon(AWalletAlertDialog.SUCCESS);
        dialog.setOnDismissListener(v -> {
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.show();
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
            e.printStackTrace();
        }
    }

    private void onTicket(Ticket ticket)
    {

    }

    private void onSelected(String selectionStr)
    {

    }
}