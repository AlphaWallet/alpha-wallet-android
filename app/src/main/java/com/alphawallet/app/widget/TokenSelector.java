package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.util.Utils;
import com.google.android.material.button.MaterialButton;

public class TokenSelector extends LinearLayout
{
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AddressIcon icon;
    private final TextView label;
    private final TextView address;
    private final TextView symbolText;
    private final MaterialButton btnSelectToken;
    private final LinearLayout tokenLayout;
    private final EditText editText;
    private final TextView balance;
    private final TextView maxBtn;
    private final TextView error;
    private Runnable runnable;
    private TokenSelectorEventListener callback;
    private Token tokenItem;

    public TokenSelector(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.token_selector, this);

        label = findViewById(R.id.label);
        address = findViewById(R.id.address);
        icon = findViewById(R.id.token_icon);
        symbolText = findViewById(R.id.text_token_symbol);
        btnSelectToken = findViewById(R.id.btn_select_token);
        tokenLayout = findViewById(R.id.layout_token);
        editText = findViewById(R.id.amount_entry);
        balance = findViewById(R.id.balance);
        error = findViewById(R.id.error);
        maxBtn = findViewById(R.id.btn_max);
        maxBtn.setOnClickListener(v -> {
            callback.onMaxClicked();
        });

        symbolText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (callback != null)
                {
                    callback.onSelectionChanged(tokenItem);
                }
            }
        });

        setupAttrs(context, attrs);
    }

    private void setupAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TokenSelector,
                0, 0
        );

        try
        {
            boolean showLabel = a.getBoolean(R.styleable.TokenSelector_tsShowLabel, true);
            boolean isEditable = a.getBoolean(R.styleable.TokenSelector_tsEditable, true);
            boolean showMaxBtn = a.getBoolean(R.styleable.TokenSelector_tsShowMaxButton, true);
            int labelRes = a.getResourceId(R.styleable.TokenSelector_tsLabelRes, R.string.empty);

            label.setVisibility(showLabel ? View.VISIBLE : View.GONE);
            label.setText(labelRes);

            if (!isEditable)
            {
                editText.setEnabled(false);
                editText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }

            maxBtn.setVisibility(showMaxBtn ? View.VISIBLE : View.GONE);
        }
        finally
        {
            a.recycle();
        }
    }

    public void clear()
    {
        this.tokenItem = null;

        icon.blankIcon();

        btnSelectToken.setVisibility(View.VISIBLE);

        tokenLayout.setVisibility(View.GONE);

        editText.getText().clear();

        address.setText(R.string.empty);

        balance.setVisibility(View.INVISIBLE);

        error.setVisibility(View.GONE);

        setVisibility(View.INVISIBLE);
    }

    public void reset()
    {
        clear();

        setVisibility(View.VISIBLE);
    }

    public void init(Token tokenItem)
    {
        this.tokenItem = tokenItem;

        icon.bindData(tokenItem.logoURI, tokenItem.chainId, tokenItem.address, tokenItem.symbol);

        btnSelectToken.setVisibility(View.GONE);

        tokenLayout.setVisibility(View.VISIBLE);

        symbolText.setText(tokenItem.symbol);

        address.setText(Utils.formatAddress(tokenItem.address));

        setVisibility(View.VISIBLE);
    }

    public void setEventListener(TokenSelectorEventListener listener)
    {
        this.callback = listener;
        btnSelectToken.setOnClickListener(v -> listener.onSelectorClicked());
        tokenLayout.setOnClickListener(v -> listener.onSelectorClicked());
        editText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                error.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                runnable = () -> listener.onAmountChanged(editable.toString());

                handler.postDelayed(runnable, 1000);
            }
        });
    }

    public Token getToken()
    {
        return this.tokenItem;
    }

    public String getAmount()
    {
        return editText.getText().toString();
    }

    public void setAmount(String amount)
    {
        editText.setText(amount);
    }

    public void clearAmount()
    {
        editText.getText().clear();
    }

    public void setBalance(String amount)
    {
        StringBuilder balanceStr = new StringBuilder(getContext().getString(R.string.label_balance));
        balanceStr.append(" ");
        if (!TextUtils.isEmpty(amount) && !amount.equals("0"))
        {
            balanceStr.append(amount);
            setMaxButtonEnabled(true);
        }
        else
        {
            balanceStr.append(0);
            setMaxButtonEnabled(false);
        }
        balanceStr.append(" ");
        balanceStr.append(tokenItem.symbol);
        balance.setText(balanceStr.toString());
        balance.setVisibility(View.VISIBLE);
    }

    public void setError(String message)
    {
        error.setVisibility(View.VISIBLE);
        error.setText(message);
    }

    public void setMaxButtonEnabled(boolean enabled)
    {
        maxBtn.setEnabled(enabled);
    }

    public interface TokenSelectorEventListener
    {
        /**
         * Triggered when the Token or 'Select Button' is clicked.
         **/
        void onSelectorClicked();

        /**
         * Triggered when user changes the amount.
         **/
        void onAmountChanged(String amount);

        /**
         * Triggered when a new Token is selected.
         **/
        void onSelectionChanged(Token token);

        /**
         * Triggered when the `Max` button is clicked.
         **/
        void onMaxClicked();
    }
}
