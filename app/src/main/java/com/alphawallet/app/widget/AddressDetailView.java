package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
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
    private final TextView labelEnsName;
    private final TextView textEnsName;
    private final TextView textMessage;
    private final ImageView recipientDetails;
    private final UserAvatar userAvatar;
    private final LinearLayout layoutDetails;
    private final LinearLayout layoutEnsName;
    private final LinearLayout layoutHolder;

    public AddressDetailView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_address_detail, this);
        textAddressSummary = findViewById(R.id.text_recipient);
        textFullAddress = findViewById(R.id.text_recipient_address);
        labelEnsName = findViewById(R.id.label_ens_name);
        textEnsName = findViewById(R.id.text_ens_name);
        textMessage = findViewById(R.id.message);
        recipientDetails = findViewById(R.id.image_more);
        userAvatar = findViewById(R.id.blockie);
        layoutDetails = findViewById(R.id.layout_detail);
        layoutEnsName = findViewById(R.id.layout_ens_name);
        layoutHolder = findViewById(R.id.layout_holder);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0);

        TextView titleTextView = findViewById(R.id.text_address_title);
        titleTextView.setText(a.getResourceId(R.styleable.InputView_label, R.string.recipient));
    }

    public void addMessage(String message, int drawableRes)
    {
        textMessage.setText(message);
        textMessage.setVisibility(View.VISIBLE);
        if (drawableRes > 0)
        {
            textAddressSummary.setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0);
            textMessage.setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0);
        }
    }

    public void setupAddress(String address, String ensName, Token destToken)
    {
        boolean hasEns = !TextUtils.isEmpty(ensName);
        String destStr = (hasEns ? ensName + " | " : "") + Utils.formatAddress(address);
        textAddressSummary.setText(destStr);
        userAvatar.bind(new Wallet(address), wallet -> { /*NOP, here to enable lookup of ENS avatar*/ });
        textFullAddress.setText(address);

        if (TextUtils.isEmpty(ensName))
        {
            if (destToken != null && !destToken.isEthereum())
            {
                labelEnsName.setVisibility(View.VISIBLE);
                layoutEnsName.setVisibility(View.VISIBLE);
                labelEnsName.setText(R.string.token_text);
                textEnsName.setText(destToken.getFullName());
            }
            else
            {
                labelEnsName.setVisibility(View.GONE);
                layoutEnsName.setVisibility(View.GONE);
            }
        }
        else
        {
            labelEnsName.setVisibility(View.VISIBLE);
            layoutEnsName.setVisibility(View.VISIBLE);
            textEnsName.setText(ensName);
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
        recipientDetails.setVisibility(View.INVISIBLE);
        //shorten requesterURL if required
        requesterUrl = abbreviateURL(requesterUrl);
        textAddressSummary.setText(requesterUrl);
    }

    private String abbreviateURL(String inputURL)
    {
        if (inputURL.length() > 32)
        {
            int index = inputURL.indexOf("/", 20);
            return index >= 0 ? inputURL.substring(0, index) : inputURL;
        }
        else
        {
            return inputURL;
        }
    }
}
