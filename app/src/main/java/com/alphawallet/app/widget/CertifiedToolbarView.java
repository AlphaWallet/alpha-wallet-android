package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.app.R;

public class CertifiedToolbarView extends android.support.v7.widget.Toolbar
{
    private final Context context;
    private Activity activity;
    private AWalletAlertDialog dialog;
    private int lockResource;

    public CertifiedToolbarView(Context ctx)
    {
        super(ctx);
        context = ctx;
    }

    public CertifiedToolbarView(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        context = ctx;
    }

    public void onSigData(final XMLDsigDescriptor sigData, final Activity act)
    {
        activity = act;
        findViewById(R.id.certificate_spinner).setVisibility(View.GONE);
        ImageView lockStatus = findViewById(R.id.image_lock);
        //TextView signatureMessage = findViewById(R.id.text_verified);
        lockStatus.setVisibility(View.VISIBLE);

        lockStatus.setOnClickListener(view -> {
            showCertificateDetails(sigData);
        });

        switch (sigData.type)
        {
            case NO_TOKENSCRIPT:
                lockStatus.setVisibility(View.GONE);
                break;
            case DEBUG_NO_SIGNATURE:
                lockStatus.setImageResource(R.mipmap.ic_unlocked_debug);
                break;
            case DEBUG_SIGNATURE_INVALID:
                lockResource = R.mipmap.ic_unlocked_debug;
                lockStatus.setImageResource(R.mipmap.ic_unlocked_debug);
                //signatureMessage.setText(R.string.certificate_fail);
                break;
            case DEBUG_SIGNATURE_PASS:
                lockResource = R.mipmap.ic_locked_debug;
                lockStatus.setImageResource(R.mipmap.ic_locked_debug);
                break;
            case NO_SIGNATURE:
                lockStatus.setImageResource(R.mipmap.ic_unverified);
                break;
            case SIGNATURE_INVALID:
                lockResource = R.mipmap.ic_unverified;
                lockStatus.setImageResource(R.mipmap.ic_unverified);
                break;
            case SIGNATURE_PASS:
                lockResource = R.mipmap.ic_locked;
                lockStatus.setImageResource(R.mipmap.ic_locked);
                break;
        }
    }

    private void showCertificateDetails(final XMLDsigDescriptor sigData)
    {
        if (dialog != null && dialog.isShowing()) dialog.cancel();
        dialog = new AWalletAlertDialog(activity);
        dialog.setIcon(lockResource);
        dialog.setTitle(R.string.signature_details);
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
}
