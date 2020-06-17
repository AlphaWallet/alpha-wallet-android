package com.alphawallet.app.ui.widget.holder;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.util.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TokenHolder extends BinderViewHolder<Token> implements View.OnClickListener, View.OnLongClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    private final TextView balanceEth;
    private final TextView balanceCurrency;
    private final ImageView icon;
    private final TextView text24Hours;
    private final TextView textAppreciation;
    private final TextView issuer;
    private final TextView issuerPlaceholder;
    private final TextView textAppreciationSub;
    private final TextView contractType;
    private final TextView currencyLabel;
    private final TextView chainName;
    private final View contractSeparator;
    private final LinearLayout layoutValueDetails;
    private final LinearLayout extendedInfo;
    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor
    private final TextView pendingText;
    private final RelativeLayout tokenLayout;

    private Handler handler;

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent, AssetDefinitionService assetService)
    {
        super(resId, parent);

        icon = findViewById(R.id.icon);
        balanceEth = findViewById(R.id.eth_data);
        balanceCurrency = findViewById(R.id.balance_currency);
        currencyLabel = findViewById(R.id.currency_label);
        text24Hours = findViewById(R.id.text_24_hrs);
        textAppreciation = findViewById(R.id.text_appreciation);
        issuer = findViewById(R.id.issuer);
        issuerPlaceholder = findViewById(R.id.issuerPlaceholder);
        textAppreciationSub = findViewById(R.id.text_appreciation_sub);
        contractType = findViewById(R.id.contract_type);
        contractSeparator = findViewById(R.id.contract_seperator);
        layoutValueDetails = findViewById(R.id.layout_value_details);
        chainName = findViewById(R.id.text_chain_name);
        pendingText = findViewById(R.id.balance_eth_pending);
        tokenLayout = findViewById(R.id.token_layout);
        extendedInfo = findViewById(R.id.layout_extended_info);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition) {
        this.token = data;

        try
        {
            icon.setVisibility(View.GONE); // TODO: code for icon display
            tokenLayout.setBackgroundResource(R.drawable.background_marketplace_event);
            if (EthereumNetworkRepository.isPriorityToken(token)) extendedInfo.setVisibility(View.GONE);

            //setup name and value (put these together on a single string to make wrap-around text appear better).
            String nameValue = token.getStringBalance() + " " + token.getFullName(assetDefinition, token.getTicketCount());
            balanceEth.setText(nameValue);

            setContractType();

            populateTicker();

            setIssuerDetails();

            setPendingAmount();

            setChainColour();
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    private void setPendingAmount()
    {
        String pendingDiff = token.getPendingDiff();
        if (pendingDiff != null)
        {
            pendingText.setText(pendingDiff);
            pendingText.setTextColor(ContextCompat.getColor(getContext(), (pendingDiff.startsWith("-")) ? R.color.red : R.color.green));
        }
        else
        {
            pendingText.setText("");
        }
    }

    private void populateTicker()
    {
        if (token.ticker == null)
        {
            if (token.isEthereum()) animateTextWhileWaiting();
            else layoutValueDetails.setVisibility(View.GONE);
            return;
        }

        layoutValueDetails.setVisibility(View.VISIBLE);
        stopTextAnimation();
        BigDecimal correctedBalance = token.getCorrectedBalance(18);
        BigDecimal fiatBalance = correctedBalance.multiply(new BigDecimal(token.ticker.price)).setScale(18, RoundingMode.DOWN);
        String converted = TickerService.getCurrencyString(fiatBalance.doubleValue());
        String formattedPercents = "";
        currencyLabel.setText(token.ticker.priceSymbol);
        int color = Color.RED;
        double percentage = 0;
        try {
            percentage = Double.valueOf(token.ticker.percentChange24h);
            color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = (percentage < 0 ? "" : "+") + token.ticker.percentChange24h + "%";
            text24Hours.setText(formattedPercents);
            text24Hours.setTextColor(color);
        } catch (Exception ex) { /* Quietly */ }
        String lbl = getString(R.string.token_balance, "",
                               converted);

        Spannable spannable;
        if (correctedBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                              converted.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.balanceCurrency.setText(spannable);
        }
        else
        {
            this.balanceCurrency.setText(EMPTY_BALANCE);
        }

        //calculate the appreciation value
        double dBalance = correctedBalance.multiply(new BigDecimal(token.ticker.price)).doubleValue();
        double nPercentage = (100.0 + percentage)/100.0;
        double dAppreciation = dBalance - (dBalance/nPercentage);
        BigDecimal appreciation = BigDecimal.valueOf(dAppreciation);

        int valColor;
        if (appreciation.compareTo(BigDecimal.ZERO) >= 0)
        {
            valColor = ContextCompat.getColor(getContext(), R.color.light_gray);
            textAppreciationSub.setText(R.string.appreciation);
            textAppreciation.setTextColor(valColor);
        }
        else
        {
            valColor = ContextCompat.getColor(getContext(), R.color.red);
            textAppreciationSub.setText(R.string.depreciation);
            textAppreciation.setTextColor(valColor);
            appreciation = appreciation.multiply(BigDecimal.valueOf(-1));
        }

        String convertedAppreciation = TickerService.getCurrencyString(appreciation.doubleValue());

        lbl = getString(R.string.token_balance, "",
                        convertedAppreciation);

        if (correctedBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                              convertedAppreciation.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.textAppreciation.setText(spannable);
        }
        else
        {
            this.textAppreciation.setText(EMPTY_BALANCE);
        }
    }

    private void fillEmpty() {
        balanceEth.setText(R.string.NA);
        balanceCurrency.setText(EMPTY_BALANCE);
    }

    private Runnable clearElevation = new Runnable()
    {
        @Override
        public void run()
        {
            tokenLayout.setElevation(0.0f);
            handler = null;
        }
    };

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            tokenLayout.setElevation(-10.0f);
            onTokenClickListener.onTokenClick(v, token, null, true);
            handler = new Handler();
            handler.postDelayed(clearElevation, 800);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (onTokenClickListener != null) {
            onTokenClickListener.onLongTokenClick(v, token, null);
        }

        return true;
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    public void setOnLongClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    private void animateTextWhileWaiting() {
        emptyTicker();
        layoutValueDetails.setVisibility(View.VISIBLE);
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(450);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        text24Hours.startAnimation(anim);
        textAppreciation.startAnimation(anim);
        balanceCurrency.startAnimation(anim);
    }

    private void setIssuerDetails()
    {
        String issuerName = assetDefinition.getIssuerName(token);
        if(issuerName != null && !issuerName.equalsIgnoreCase(getString(R.string.app_name))) //don't display issuer if it's alphawallet
        {
            issuer.setVisibility(View.VISIBLE);
            issuerPlaceholder.setVisibility(View.VISIBLE);
            if (contractType.getVisibility() == View.VISIBLE)
            {
                contractSeparator.setVisibility(View.VISIBLE);
            }
            else
            {
                contractSeparator.setVisibility(View.GONE);
            }
            issuer.setText(issuerName);
        }
        else
        {
            issuer.setVisibility(View.GONE);
            issuerPlaceholder.setVisibility(View.GONE);
            contractSeparator.setVisibility(View.GONE);
        }
    }

    private void stopTextAnimation() {
        text24Hours.clearAnimation();
        textAppreciation.clearAnimation();
        balanceCurrency.clearAnimation();
    }

    private void setContractType()
    {
        //Contract type
        int contractStringId = token.getContractType();
        if (contractStringId > 0)
        {
            contractType.setText(contractStringId);
            contractType.setVisibility(View.VISIBLE);
        }
        else
        {
            contractType.setVisibility(View.GONE);
        }
    }

    private void emptyTicker()
    {
        text24Hours.setText(R.string.unknown_balance_without_symbol);
        textAppreciation.setText(R.string.unknown_balance_without_symbol);
        balanceCurrency.setText(R.string.unknown_balance_without_symbol);
    }

    private void setChainColour()
    {
        chainName.setVisibility(View.VISIBLE);
        chainName.setText(token.getNetworkName());
        Utils.setChainColour(chainName, token.tokenInfo.chainId);
    }
}