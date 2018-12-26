package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;

import java.math.BigInteger;

public class ChangeTokenHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1005;
    private final TextView symbol;
    private final Switch enableControl;

    private Token token;
    private OnTokenClickListener onTokenClickListener;

    public ChangeTokenHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        symbol = findViewById(R.id.symbol);
        enableControl = findViewById(R.id.is_enable);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition) {
        if (data == null) {
            return;
        }
        token = data;
        if (TextUtils.isEmpty(token.tokenInfo.name)) {
            symbol.setText(token.tokenInfo.symbol);
        } else {
            symbol.setText(token.tokenInfo.name + " (" + token.tokenInfo.symbol + ")");
        }
        enableControl.setChecked(data.tokenInfo.isEnabled);
    }

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            enableControl.setChecked(!token.tokenInfo.isEnabled);
            onTokenClickListener.onTokenClick(v, token, BigInteger.ZERO);
        }
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }
}
