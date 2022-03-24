package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.entity.InputFiatCallback;
import com.alphawallet.app.ui.widget.entity.NumericInput;


public class InputFiatView extends LinearLayout {
    private final Context context;
    private final NumericInput amountInput;
    private final LinearLayout moreLayout;
    private final ImageView icon;
    private final ImageView expandMore;
    private final TextView symbolText;
    private final TextView subTextLabel;
    private final TextView subTextValue;
    private final StandardHeader header;
    private InputFiatCallback callback;

    public InputFiatView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.item_input_fiat, this);

        header = findViewById(R.id.header);
        moreLayout = findViewById(R.id.layout_more_click);
        expandMore = findViewById(R.id.expand_more);
        icon = findViewById(R.id.icon);
        symbolText = findViewById(R.id.symbol);
        amountInput = findViewById(R.id.amount_entry);
        subTextLabel = findViewById(R.id.subtext_label);
        subTextValue = findViewById(R.id.subtext_value);

        setupAttrs(context, attrs);

        setupViewListeners();

        initValues();
    }

    private void initValues()
    {
        String currencySymbol = TickerService.getCurrencySymbolTxt();
        symbolText.setText(currencySymbol);
        icon.setImageResource(CurrencyRepository.getFlagByISO(currencySymbol));

    }

    private void setupAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try
        {
            boolean showHeader = a.getBoolean(R.styleable.InputView_show_header, true);
            int headerTextId = a.getResourceId(R.styleable.InputView_label, R.string.enter_target_price);
            header.setText(headerTextId);
            header.setVisibility(showHeader ? View.VISIBLE : View.GONE);

            boolean canChangeCurrency = a.getBoolean(R.styleable.InputView_can_change_currency, true);
            expandMore.setVisibility(canChangeCurrency ? VISIBLE : GONE);
        } finally
        {
            a.recycle();
        }
    }

    private void setupViewListeners()
    {
        moreLayout.setOnClickListener(v -> {
            callback.onMoreClicked();
        });

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                callback.onInputChanged(s.toString());
            }
        });
    }

    public void setCallback(InputFiatCallback callback)
    {
        this.callback = callback;
    }

    public void setCurrency(String symbol)
    {
        icon.setImageResource(CurrencyRepository.getFlagByISO(symbol));
        symbolText.setText(symbol);
    }

    public void showKeyboard()
    {
        amountInput.requestFocus();
    }

    public void setSubTextValue(String text)
    {
        subTextValue.setText(text);
    }
}
