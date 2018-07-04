package io.stormbird.wallet.ui.widget.holder;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTicker;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TokenHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    public final TextView symbol;
    public final TextView balanceEth;
    public final TextView balanceCurrency;
    public final ImageView icon;
    public final TextView arrayBalance;
    public final TextView text24Hours;
    public final TextView textAppreciation;
    public final TextView issuer;
    public final TextView text24HoursSub;
    public final TextView textAppreciationSub;
    public final TextView contractType;
    public final View contractSeparator;
    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent, AssetDefinitionService assetService)
    {
        super(resId, parent);

        icon = findViewById(R.id.icon);
        symbol = findViewById(R.id.symbol);
        balanceEth = findViewById(R.id.balance_eth);
        balanceCurrency = findViewById(R.id.balance_currency);
        arrayBalance = findViewById(R.id.balanceArray);
        text24Hours = findViewById(R.id.text_24_hrs);
        textAppreciation = findViewById(R.id.text_appreciation);
        issuer = findViewById(R.id.issuer);
        text24HoursSub = findViewById(R.id.text_24_hrs_sub);
        textAppreciationSub = findViewById(R.id.text_appreciation_sub);
        contractType = findViewById(R.id.contract_type);
        contractSeparator = findViewById(R.id.contract_seperator);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition) {
        this.token = data;
        if (! data.isERC20)
        {
            // TODO: apply styles for none ERC20 contracts
            contractType.setVisibility(View.GONE);
            contractSeparator.setVisibility(View.GONE);
        }
        try
        {
            String displayTxt = assetDefinition.getIssuerName(token.getAddress());
            issuer.setText(displayTxt);
            // We handled NPE. Exception handling is expensive, but not impotent here -james brown
            symbol.setText(TextUtils.isEmpty(token.tokenInfo.name)
                        ? token.tokenInfo.symbol.toUpperCase()
                        : token.getFullName());// getString(R.string.token_name, token.tokenInfo.name, token.tokenInfo.symbol.toUpperCase()));

            token.setupContent(this, assetDefinition);
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    public void fillIcon(String imageUrl, int defaultResId) {
        if (TextUtils.isEmpty(imageUrl)) {
            icon.setImageResource(defaultResId);
        } else {
            Picasso.with(getContext())
                    .load(imageUrl)
                    .fit()
                    .centerInside()
                    .placeholder(defaultResId)
                    .error(defaultResId)
                    .into(icon);
        }
    }

    public void fillCurrency(BigDecimal ethBalance, TokenTicker ticker) {
        String converted = ethBalance.compareTo(BigDecimal.ZERO) == 0
                ? EMPTY_BALANCE
                : ethBalance.multiply(new BigDecimal(ticker.price))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        String formattedPercents = "";
        int color = Color.RED;
        double percentage = 0;
        try {
            percentage = Double.valueOf(ticker.percentChange24h);
            color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = (percentage < 0 ? "" : "+") + ticker.percentChange24h + "%";
            text24Hours.setText(formattedPercents);
            text24Hours.setTextColor(color);
        } catch (Exception ex) { /* Quietly */ }
        String lbl = getString(R.string.token_balance,
                ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                converted);

        Spannable spannable;
        if (ethBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                              converted.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.balanceCurrency.setText(spannable);
        }
        else
        {
            this.balanceCurrency.setText(EMPTY_BALANCE);
        }

        //calculate the appreciation value
        double dBalance = ethBalance.multiply(new BigDecimal(ticker.price)).doubleValue();
        double nPercentage = (100.0 + percentage)/100.0;
        double dAppreciation = dBalance - (dBalance/nPercentage);
        BigDecimal appreciation = BigDecimal.valueOf(dAppreciation);

        int valColor;
        if (appreciation.compareTo(BigDecimal.ZERO) >= 0)
        {
            valColor = ContextCompat.getColor(getContext(), R.color.black);
            textAppreciationSub.setText(R.string.appreciation);
            textAppreciationSub.setTextColor(valColor);
        }
        else
        {
            valColor = ContextCompat.getColor(getContext(), R.color.red);
            textAppreciationSub.setText(R.string.depreciation);
            textAppreciationSub.setTextColor(valColor);
            appreciation = appreciation.multiply(BigDecimal.valueOf(-1));
        }

        //BigDecimal appreciation = balance.subtract(balance.divide((BigDecimal.valueOf(percentage).add(BigDecimal.ONE))) );
        String convertedAppreciation =
                appreciation
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();

        lbl = getString(R.string.token_balance,
                ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                convertedAppreciation);

        if (ethBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                              convertedAppreciation.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.textAppreciation.setText(spannable);
        }
        else
        {
            this.textAppreciation.setText(EMPTY_BALANCE);
        }
    }

    public boolean needsUpdate()
    {
        return (token != null && token.needsUpdate());
    }

    public void updateHeading()
    {
        if (token != null)
        {
            token.checkUpdateTimeValid(getContext(), this);
        }
    }

    protected void fillEmpty() {
        balanceEth.setText(R.string.NA);
        balanceCurrency.setText(EMPTY_BALANCE);
    }

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            onTokenClickListener.onTokenClick(v, token);
        }
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }
}
