package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.google.android.material.appbar.MaterialToolbar;

public class CertifiedToolbarView extends MaterialToolbar
{
    private Activity activity;
    private AWalletAlertDialog dialog;
    private final ProgressBar downloadSpinner;
    private final ProgressBar syncSpinner;
    private int lockResource = 0;

    public CertifiedToolbarView(Context ctx, @Nullable AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.layout_certified_toolbar, this);
        downloadSpinner = findViewById(R.id.cert_progress_spinner);
        syncSpinner = findViewById(R.id.nft_scan_spinner);
    }

    public void onSigData(final XMLDsigDescriptor sigData, final Activity act)
    {
        activity = act;
        ImageView lockStatus = findViewById(R.id.image_lock);
        lockStatus.setVisibility(View.VISIBLE);

        lockStatus.setOnClickListener(view -> {
            showCertificateDetails(sigData);
        });

        SigReturnType type = sigData.type != null ? sigData.type : SigReturnType.NO_TOKENSCRIPT;

        lockResource = 0;

        switch (type)
        {
            case NO_TOKENSCRIPT:
                lockStatus.setVisibility(View.GONE);
                break;
            case DEBUG_NO_SIGNATURE:
                lockResource = R.mipmap.ic_unlocked_debug;
                break;
            case DEBUG_SIGNATURE_INVALID:
                lockResource = R.mipmap.ic_unlocked_debug;
                break;
            case DEBUG_SIGNATURE_PASS:
                lockResource = R.mipmap.ic_locked_debug;
                break;
            case NO_SIGNATURE:
                lockResource = R.mipmap.ic_unverified;
                break;
            case SIGNATURE_INVALID:
                lockResource = R.mipmap.ic_unverified;
                break;
            case SIGNATURE_PASS:
                lockResource = R.mipmap.ic_locked;
                break;
        }

        if (lockResource != 0) lockStatus.setImageResource(lockResource);
    }

    public void hideCertificateResource()
    {
        ImageView lockStatus = findViewById(R.id.image_lock);
        lockStatus.setVisibility(View.GONE);
        stopDownload();
    }

    private void showCertificateDetails(final XMLDsigDescriptor sigData)
    {
        if (dialog != null && dialog.isShowing()) dialog.cancel();
        dialog = new AWalletAlertDialog(activity);
        dialog.setIcon(lockResource);
        dialog.setTitle(R.string.signature_details);
        downloadSpinner.setVisibility(View.GONE);
        String sb;
        if (sigData.issuer == null)
        {
            sb = "Tokenscript is not signed";
        }
        else
        {
            sb = "Issuer: " +
                    sigData.issuer +
                    "\n\n" +
                    "Certifier: " +
                    sigData.certificateName +
                    "\n\n" +
                    "Key Type: " +
                    sigData.keyType +
                    "\n\n" +
                    "Key Owner: " +
                    sigData.keyName;
        }
        dialog.setTextStyle(AWalletAlertDialog.TEXT_STYLE.LEFT);
        dialog.setMessage(sb);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    public void startDownload()
    {
        if (lockResource == 0)
        {
            downloadSpinner.setVisibility(View.VISIBLE);
        }
    }

    public void stopDownload()
    {
        downloadSpinner.setVisibility(View.GONE);
    }

    public void showNFTSync()
    {
        syncSpinner.setVisibility(View.VISIBLE);
    }

    public void nftSyncComplete()
    {
        syncSpinner.setVisibility(View.GONE);
    }
}
