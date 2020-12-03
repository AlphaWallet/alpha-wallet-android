package com.alphawallet.app.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.util.Blockies;
import com.alphawallet.app.util.Utils;

/**
 * Created by JB on 28/11/2020.
 */
public class AddressDetailView extends LinearLayout
{
    private final TextView textAddressSummary;
    private final TextView textFullAddress;
    private final TextView textEnsName;
    private final ImageView recipientDetails;
    private final ImageView addressBlockie;
    private final LinearLayout layoutDetails;

    public AddressDetailView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_address_detail, this);
        textAddressSummary = findViewById(R.id.text_recipient);
        textFullAddress = findViewById(R.id.text_recipient_address);
        textEnsName = findViewById(R.id.text_ens_name);
        recipientDetails = findViewById(R.id.image_more);
        addressBlockie = findViewById(R.id.blockie);
        layoutDetails = findViewById(R.id.layout_detail);
    }

    public void setupAddress(String address, String ensName)
    {
        String destStr = (!TextUtils.isEmpty(ensName) ? ensName + " | " : "") + Utils.formatAddress(address);
        textAddressSummary.setText(destStr);
        addressBlockie.setImageBitmap(Blockies.createIcon(address.toLowerCase()));
        textFullAddress.setText(address);
        textEnsName.setText(ensName);

        recipientDetails.setOnClickListener(v -> {
            if (layoutDetails.getVisibility() == View.GONE)
            {
                layoutDetails.setVisibility(View.VISIBLE);
                textAddressSummary.setVisibility(View.INVISIBLE);
                recipientDetails.setImageResource(R.drawable.ic_expand_less_black);
            }
            else
            {
                layoutDetails.setVisibility(View.GONE);
                textAddressSummary.setVisibility(View.VISIBLE);
                recipientDetails.setImageResource(R.drawable.ic_expand_more);
            }
        });
    }
}
