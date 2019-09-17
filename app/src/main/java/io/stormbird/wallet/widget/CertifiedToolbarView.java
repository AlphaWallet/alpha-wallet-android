package io.stormbird.wallet.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;

import io.stormbird.token.entity.TSAction;
import io.stormbird.token.entity.XMLDsigDescriptor;
import io.stormbird.wallet.R;

public class CertifiedToolbarView extends android.support.v7.widget.Toolbar
{
    private final Context context;

    public CertifiedToolbarView(Context ctx)
    {
        super(ctx);
        context = ctx;
    }

    public CertifiedToolbarView(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        context = ctx;
    }

    public void onSigData(XMLDsigDescriptor sigData)
    {
        findViewById(R.id.certificate_spinner).setVisibility(View.GONE);
        ImageView lockStatus = findViewById(R.id.image_lock);
        TextView signatureMessage = findViewById(R.id.text_verified);
        lockStatus.setVisibility(View.VISIBLE);
        String certifier = sigData.certificateName;
        if (certifier == null) certifier = "aw.app";

        switch (sigData.type)
        {
            case NO_TOKENSCRIPT:
                lockStatus.setVisibility(View.GONE);
                break;
            case DEBUG_NO_SIGNATURE:
                lockStatus.setImageResource(R.mipmap.ic_unlocked_debug);
                signatureMessage.setText(R.string.no_certificate);
                break;
            case DEBUG_SIGNATURE_INVALID:
                lockStatus.setImageResource(R.mipmap.ic_unlocked_debug);
                signatureMessage.setText(R.string.certificate_fail);
                break;
            case DEBUG_SIGNATURE_PASS:
                lockStatus.setImageResource(R.mipmap.ic_locked_debug);
                signatureMessage.setText(context.getString(R.string.verified, certifier));
                break;
            case NO_SIGNATURE:
                lockStatus.setImageResource(R.mipmap.ic_unverified);
                signatureMessage.setText(R.string.no_certificate);
                break;
            case SIGNATURE_INVALID:
                lockStatus.setImageResource(R.mipmap.ic_unverified);
                signatureMessage.setText(R.string.certificate_fail);
                break;
            case SIGNATURE_PASS:
                lockStatus.setImageResource(R.mipmap.ic_locked);
                signatureMessage.setText(context.getString(R.string.verified, certifier));
                break;
        }
    }
}
