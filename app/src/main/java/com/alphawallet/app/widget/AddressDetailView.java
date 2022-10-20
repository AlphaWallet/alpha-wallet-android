package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
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
    private final UserAvatar userAvatar;
    private final LinearLayout layoutDetails;
    private final LinearLayout layoutHolder;

    public AddressDetailView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_address_detail, this);
        textAddressSummary = findViewById(R.id.text_recipient);
        textFullAddress = findViewById(R.id.text_recipient_address);
        textEnsName = findViewById(R.id.text_ens_name);
        recipientDetails = findViewById(R.id.image_more);
        userAvatar = findViewById(R.id.blockie);
        layoutDetails = findViewById(R.id.layout_detail);
        layoutHolder = findViewById(R.id.layout_holder);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0);

        TextView recipientText = findViewById(R.id.text_recipient_title);
        recipientText.setText(a.getResourceId(R.styleable.InputView_label, R.string.recipient));
    }

    public void setupAddress(String address, String ensName, Token destToken)
    {
        boolean hasEns = !TextUtils.isEmpty(ensName);
        String destStr = (hasEns ? ensName + " | " : "") + (hasEns ? Utils.formatAddress(address) : address);
        textAddressSummary.setText(destStr);
        userAvatar.bind(new Wallet(address), wallet -> { /*NOP, here to enable lookup of ENS avatar*/ });
        textFullAddress.setText(address);
        textEnsName.setText(ensName);

        if (TextUtils.isEmpty(ensName) && destToken != null && !destToken.isEthereum())
        {
            ((TextView)findViewById(R.id.label_ens)).setText(R.string.token_text);
            textEnsName.setText(destToken.getFullName());
        }

        layoutHolder.setOnClickListener(v -> {
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

    public void setupRequester(String requesterUrl)
    {
        setVisibility(View.VISIBLE);
        recipientDetails.setVisibility(View.GONE);
        //shorten requesterURL if required
        requesterUrl = abbreviateURL(requesterUrl);
        textAddressSummary.setText(requesterUrl);
        ViewGroup.LayoutParams param = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 3.4f);
        textAddressSummary.setLayoutParams(param);
    }

    private String abbreviateURL(String inputURL)
    {
        if (inputURL.length() > 32)
        {
            int index = inputURL.indexOf("/", 20);
            return index >= 0 ? inputURL.substring(0,index) : inputURL;
        }
        else
        {
            return inputURL;
        }
    }
}
