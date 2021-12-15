package com.alphawallet.app.ui.widget.holder;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.PALM_ID;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.widget.TokenIcon;
import com.alphawallet.token.tools.Convert;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TokenHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, View.OnLongClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";
    private static final long TICKER_PERIOD_VALIDITY = 60 * DateUtils.MINUTE_IN_MILLIS; //Tickers stale after 60 minutes

    private final TokenIcon tokenIcon;
    private final TextView balanceEth;
    private final TextView balanceCurrency;
    private final TextView balanceCoin;
    private final TextView text24Hours;
    private final View     root24Hours;
    private final ImageView image24h;
    private final TextView textAppreciation;
    private final View contractSeparator;
    private final View layoutAppreciation;
    private final LinearLayout extendedInfo;
    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor
    private final TokensService tokensService;
    private final TextView pendingText;
    private final RelativeLayout tokenLayout;

    public Token token;
    private TokensAdapterCallback tokensAdapterCallback;

    public TokenHolder(ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs)
    {
        super(R.layout.item_token, parent);

        tokenIcon = findViewById(R.id.token_icon);
        balanceEth = findViewById(R.id.eth_data);
        balanceCurrency = findViewById(R.id.balance_currency);
        balanceCoin = findViewById(R.id.balance_coin);
        text24Hours = findViewById(R.id.text_24_hrs);
        root24Hours = findViewById(R.id.root_24_hrs);
        image24h = findViewById(R.id.image_24_hrs);
        textAppreciation = findViewById(R.id.text_appreciation);
        contractSeparator = findViewById(R.id.contract_seperator);
        pendingText = findViewById(R.id.balance_eth_pending);
        tokenLayout = findViewById(R.id.token_layout);
        extendedInfo = findViewById(R.id.layout_extended_info);
        layoutAppreciation = findViewById(R.id.layout_appreciation);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        tokensService = tSvs;
    }

    @Override
    public void bind(@Nullable TokenCardMeta data, @NonNull Bundle addition)
    {
        layoutAppreciation.setForeground(null);
        balanceCurrency.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        if (data == null) { fillEmpty(); return; }
        try
        {
            token = tokensService.getToken(data.getChain(), data.getAddress());
            if (token == null)
            {
                fillEmpty();
                return;
            }
            else if (data.nameWeight < 1000 && !token.isEthereum())
            {
                //edge condition - looking at a contract as an account
                Token backupChain = tokensService.getToken(data.getChain(), "eth");
                if (backupChain != null) token = backupChain;
            }

            tokenLayout.setVisibility(View.VISIBLE);
            tokenLayout.setBackgroundResource(R.drawable.background_marketplace_event);
            if (EthereumNetworkRepository.isPriorityToken(token)) extendedInfo.setVisibility(View.GONE);
            contractSeparator.setVisibility(View.GONE);

            balanceEth.setText(shortTitle());

            String coinBalance = token.getStringBalance();
            if (!TextUtils.isEmpty(coinBalance)) {
                balanceCoin.setVisibility(View.VISIBLE);

                String symbol = token.getSymbol().substring(0, Math.min(token.getSymbol().length(), 5))
                        .toUpperCase();

                balanceCoin.setText(getString(R.string.valueSymbol, coinBalance, symbol));
            }

            tokenIcon.bindData(token, assetDefinition);
            if (!token.isEthereum()) tokenIcon.setChainIcon(token.tokenInfo.chainId); //Add in when we upgrade the design
            tokenIcon.setOnTokenClickListener(tokensAdapterCallback);

            populateTicker();

            setPendingAmount();

        } catch (Exception ex) {
            fillEmpty();
        }
    }

    @Override
    public void onDestroyView()
    {

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
        resetTickerViews();
        TokenTicker ticker = tokensService.getTokenTicker(token);
        if (ticker != null || (token.isEthereum() && EthereumNetworkRepository.hasRealValue(token.tokenInfo.chainId)))
        {
            handleTicker(ticker);
        }
        else
        {
            balanceCurrency.setVisibility(View.GONE);
            layoutAppreciation.setVisibility(View.GONE);
        }

        if (!token.isEthereum() && token.tokenInfo.chainId != MAINNET_ID)
        {
            showNetworkLabel();
        }
        else
        {
            hideNetworkLabel();
        }
    }

    private void handleTicker(TokenTicker ticker)
    {
        if (ticker != null)
        {
            hideIssuerViews();
            layoutAppreciation.setVisibility(View.VISIBLE);
            balanceCurrency.setVisibility(View.VISIBLE);
            setTickerInfo(ticker);
            maskStaleTicker(ticker);
        }
        else
        {
            //Ethereum token without a ticker
            balanceCurrency.setVisibility(View.GONE);
            layoutAppreciation.setVisibility(View.GONE);
        }
    }

    private void maskStaleTicker(TokenTicker ticker)
    {
        if ((System.currentTimeMillis() - ticker.updateTime) > TICKER_PERIOD_VALIDITY)
        {
            layoutAppreciation.setForeground(AppCompatResources.getDrawable(getContext(), R.color.translucentWhiteSolid));
            balanceCurrency.setTextColor(ContextCompat.getColor(getContext(), R.color.dove_hint));
        }
        else
        {
            layoutAppreciation.setForeground(null);
            balanceCurrency.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        }
    }

    private void showNetworkLabel() {

    }

    private void hideNetworkLabel() {

    }

    private void fillEmpty() {
        balanceEth.setText(R.string.empty);
        balanceCurrency.setText(EMPTY_BALANCE);
    }

    @Override
    public void onClick(View v) {
        if (tokensAdapterCallback != null && token != null) {
            tokensAdapterCallback.onTokenClick(v, token, null, true);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (tokensAdapterCallback != null) {
            tokensAdapterCallback.onLongTokenClick(v, token, null);
        }

        return true;
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback) {
        this.tokensAdapterCallback = tokensAdapterCallback;
    }

    private void hideIssuerViews() {
        contractSeparator.setVisibility(View.GONE);
    }

    private void setTickerInfo(TokenTicker ticker)
    {
        //Set the fiat equivalent (leftmost value)
        BigDecimal correctedBalance = token.getCorrectedBalance(Convert.Unit.ETHER.getFactor());
        BigDecimal fiatBalance = correctedBalance.multiply(new BigDecimal(ticker.price)).setScale(Convert.Unit.ETHER.getFactor(), RoundingMode.DOWN);
        String converted = TickerService.getCurrencyString(fiatBalance.doubleValue());
        String formattedPercents = "";
        int color = Color.RED;

        String lbl = getString(R.string.token_balance, "", converted);
        Spannable spannable;
        if (correctedBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                    converted.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.balanceCurrency.setText(lbl);
        }
        else
        {
            this.balanceCurrency.setText(EMPTY_BALANCE);
        }

        //This sets the 24hr percentage change (rightmost value)
        double percentage = 0;
        try {
            percentage = Double.parseDouble(ticker.percentChange24h);
            color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = ticker.percentChange24h.replace("-", "") + "%";
            root24Hours.setBackgroundResource(percentage < 0 ? R.drawable.background_24h_change_red : R.drawable.background_24h_change_green);
            text24Hours.setText(formattedPercents);
            text24Hours.setTextColor(color);
            image24h.setImageResource(percentage < 0 ? R.drawable.ic_price_down : R.drawable.ic_price_up);
        } catch (Exception ex) { /* Quietly */ }

        //This sets the crypto price value (middle amount)
        BigDecimal currencyChange = new BigDecimal(fiatBalance.doubleValue()).multiply((
                new BigDecimal(ticker.percentChange24h)).divide(new BigDecimal(100)));
        String formattedValue =  TickerService.getCurrencyString(currencyChange.doubleValue());
        
        this.textAppreciation.setTextColor(color);
        this.textAppreciation.setText(formattedValue);
    }

    private String shortTitle() {
        String localizedNameFromAssetDefinition = token.getTSName(assetDefinition, token.getTokenCount());
        // 1 Use TokenScript name if available.
        if (!TextUtils.isEmpty(localizedNameFromAssetDefinition)) {
            return localizedNameFromAssetDefinition;
        } else {
            return token.getName();
        }
    }

    private void resetTickerViews()
    {
        extendedInfo.setForeground(null);
        layoutAppreciation.setForeground(null);
    }
}