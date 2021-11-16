package com.alphawallet.app.widget;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.Signable;

public class SignMessageDialog extends Dialog {
    //    private LinearLayout container;
    private final TextView message;
    private final TextView requester;
    private final TextView address;
    private final TextView value;
    private final TextView valueLabel;
    private final TextView messageLabel;
    private final TextView title;
    private final LinearLayout valueLayout;
    private final TextView valueUSD;
    private final TextView usdLabel;
    private final Button btnReject;
    private final LinearLayout layoutBtnApprove;
    private final Context context;

    public SignMessageDialog(@NonNull Context activity) {
        super(activity);
        this.context = activity;

        setContentView(R.layout.dialog_sign_message);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        title = findViewById(R.id.dialog_main_text);
        message = findViewById(R.id.message);
        requester = findViewById(R.id.requester);
        address = findViewById(R.id.address);
        value = findViewById(R.id.value);
        valueLabel = findViewById(R.id.value_label);
        messageLabel = findViewById(R.id.message_label);
        valueLayout = findViewById(R.id.value_layout);
        valueUSD = findViewById(R.id.value_usd);
        usdLabel = findViewById(R.id.usd_label);
        btnReject = findViewById(R.id.btn_reject);
        layoutBtnApprove = findViewById(R.id.button_container);
        btnReject.setOnClickListener(v -> dismiss());
    }

    public SignMessageDialog(Context activity, Signable message) {
        this(activity);

        if (message instanceof EthereumTypedMessage)
        {
            setMessage(((EthereumTypedMessage)message).getUserMessage());
        }
        else
        {
            setMessage(message.getMessage());
        }
        setRequester(message.getOrigin());
    }

    public void setMessage(CharSequence message) {
        this.message.setText(message);
    }

    public void setRequester(CharSequence requester) {
        this.requester.setText(requester);
    }

    public void setAddress(CharSequence address) {
        this.address.setText(address);
    }

    public void setValue(CharSequence value, CharSequence dollarValue, String networkName)
    {
        title.setText(R.string.dialog_title_sign_transaction);

        this.message.setVisibility(View.GONE);
        this.messageLabel.setVisibility(View.GONE);

        this.valueLayout.setVisibility(View.VISIBLE);
        this.valueLabel.setVisibility(View.VISIBLE);

        this.valueUSD.setText(dollarValue);
        this.value.setText(value);

        if (networkName.length() > 0)
        {
            usdLabel.setVisibility(View.VISIBLE);
            usdLabel.setText(networkName);
        }
    }

    public void setOnApproveListener(View.OnClickListener listener) {
        layoutBtnApprove.setOnClickListener(listener);
    }

    public void setOnRejectListener(View.OnClickListener listener) {
        btnReject.setOnClickListener(listener);

    }
}
