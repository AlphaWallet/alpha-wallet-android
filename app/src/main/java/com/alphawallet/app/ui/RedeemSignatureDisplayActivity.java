package com.alphawallet.app.ui;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.viewmodel.RedeemAssetSelectViewModel;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.entity.TicketRangeParcel;

import timber.log.Timber;

import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.RedeemSignatureDisplayModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;

import javax.inject.Inject;

import static com.alphawallet.app.C.Key.*;
import static com.alphawallet.app.C.PRUNE_ACTIVITY;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by James on 24/01/2018.
 */
@AndroidEntryPoint
public class RedeemSignatureDisplayActivity extends BaseActivity implements View.OnClickListener, SignAuthenticationCallback, PageReadyCallback
{
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    public static final String KEY_ADDRESS = "key_address";

    private RedeemSignatureDisplayModel viewModel;

    private FinishReceiver finishReceiver;

    private Wallet wallet;
    private Token token;
    private TicketRangeParcel ticketRange;
    private Web3TokenView tokenView;
    private LinearLayout webWrapper;
    private boolean signRequest;
    private AWalletAlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rotating_signature);
        toolbar();
        signRequest = false;

        setTitle(getString(R.string.action_redeem));

        viewModel = new ViewModelProvider(this)
                .get(RedeemSignatureDisplayModel.class);

        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        wallet = getIntent().getParcelableExtra(WALLET);
        ticketRange = getIntent().getParcelableExtra(TICKET_RANGE);
        tokenView = findViewById(R.id.web3_tokenview);
        webWrapper = findViewById(R.id.layout_webwrapper);
        findViewById(R.id.advanced_options).setVisibility(View.GONE); //setOnClickListener(this);

        viewModel.signature().observe(this, this::onSignatureChanged);
        viewModel.selection().observe(this, this::onSelected);
        viewModel.burnNotice().observe(this, this::onBurned);
        viewModel.signRequest().observe(this, this::onSignRequest);

        ticketBurnNotice();
        TextView tv = findViewById(R.id.textAddIDs);
        tv.setText(getString(R.string.waiting_for_blockchain));
        tv.setVisibility(View.VISIBLE);

        //given a webview populate with rendered token
        tokenView.displayTicketHolder(token, ticketRange.range, viewModel.getAssetDefinitionService());
        tokenView.setOnReadyCallback(this);
        tokenView.setLayout(token, true);
        finishReceiver = new FinishReceiver(this);
    }

    private void onSignRequest(Boolean aBoolean)
    {
        viewModel.getAuthorisation(this, this);
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
        if (!signRequest) viewModel.prepare(token.tokenInfo.address, token, ticketRange.range);
        signRequest = false;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
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
            Timber.e(e);
        }
    }

    private void onSelected(String selectionStr)
    {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        signRequest = true;
        super.onActivityResult(requestCode,resultCode,intent);

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            gotAuthorisation(resultCode == RESULT_OK);
        }
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (gotAuth)
        {
            viewModel.completeAuthentication(SIGN_DATA);
            viewModel.updateSignature(wallet);
        }
        else
        {
            dialogKeyNotAvailableError();
        }
    }

    @Override
    public void cancelAuthentication()
    {
        if (dialog != null && dialog.isShowing()) dialog.cancel();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setTitle(R.string.authentication_cancelled);
        dialog.setButtonText(R.string.ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    private void dialogKeyNotAvailableError()
    {
        if (dialog != null && dialog.isShowing()) dialog.cancel();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.key_error);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setMessage(getString(R.string.fail_sign));
        dialog.setOnDismissListener(v -> {
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.show();
    }

    @Override
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }


    @Override
    public void onPageRendered(WebView view)
    {
        webWrapper.setVisibility(View.VISIBLE);
    }
}