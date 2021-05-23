package com.alphawallet.app.ui.widget.holder;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.TokenIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class TokenHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, View.OnLongClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    private final TokenIcon tokenIcon;
    private final TextView balanceEth;
    private final TextView balanceCurrency;
    private final TextView text24Hours;
    private final TextView textAppreciation;
    private final TextView issuer;
    private final TextView issuerPlaceholder;
    private final TextView contractType;
    private final View contractSeparator;
    private final View layoutAppreciation;
    private final LinearLayout extendedInfo;
    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor
    private final TokensService tokensService;
    private final TextView pendingText;
    private final RelativeLayout tokenLayout;
    private final ChainName testnet;
    private RealmResults<RealmTokenTicker> realmUpdate = null;
    private boolean primaryElement;
    private final Realm realm;

    private final Handler handler = new Handler();

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs, Realm r)
    {
        super(R.layout.item_token, parent);

        tokenIcon = findViewById(R.id.token_icon);
        balanceEth = findViewById(R.id.eth_data);
        balanceCurrency = findViewById(R.id.balance_currency);
        text24Hours = findViewById(R.id.text_24_hrs);
        textAppreciation = findViewById(R.id.text_appreciation);
        issuer = findViewById(R.id.issuer);
        issuerPlaceholder = findViewById(R.id.issuerPlaceholder);
        contractType = findViewById(R.id.contract_type);
        contractSeparator = findViewById(R.id.contract_seperator);
        pendingText = findViewById(R.id.balance_eth_pending);
        tokenLayout = findViewById(R.id.token_layout);
        extendedInfo = findViewById(R.id.layout_extended_info);
        layoutAppreciation = findViewById(R.id.layout_appreciation);
        testnet = findViewById(R.id.chain_name);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        tokensService = tSvs;
        realm = r;
    }

    @Override
    public void bind(@Nullable TokenCardMeta data, @NonNull Bundle addition) {

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

            if (realmUpdate != null)
            {
                realmUpdate.removeAllChangeListeners();
                realmUpdate = null;
            }

            tokenLayout.setBackgroundResource(R.drawable.background_marketplace_event);
            if (EthereumNetworkRepository.isPriorityToken(token)) extendedInfo.setVisibility(View.GONE);
            contractSeparator.setVisibility(View.GONE);

            //setup name and value (put these together on a single string to make wrap-around text appear better).
            String nameValue = token.getStringBalance() + " " + token.getFullName(assetDefinition, token.getTicketCount());
            balanceEth.setText(nameValue);

            primaryElement = false;

            tokenIcon.bindData(token, assetDefinition);
            tokenIcon.setOnTokenClickListener(onTokenClickListener);

            populateTicker();

            setContractType();

            setPendingAmount();

        } catch (Exception ex) {
            fillEmpty();
        }
    }

    @Override
    public void onDestroyView()
    {
        if (realmUpdate != null)
        {
            realmUpdate.removeAllChangeListeners();
            realmUpdate = null;
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
        TokenTicker ticker = tokensService.getTokenTicker(token);
        if (ticker != null || (token.isEthereum() && EthereumNetworkRepository.hasRealValue(token.tokenInfo.chainId)))
        {
            handleTicker();
        }
        else
        {
            balanceCurrency.setVisibility(View.GONE);
            layoutAppreciation.setVisibility(View.GONE);
            setIssuerDetails();
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

    private void handleTicker()
    {
        primaryElement = true;
        hideIssuerViews();
        layoutAppreciation.setVisibility(View.VISIBLE);
        balanceCurrency.setVisibility(View.VISIBLE);
        startTickerRealmListener();
    }

    private void showNetworkLabel() {
        testnet.setVisibility(View.VISIBLE);
        testnet.setChainID(token.tokenInfo.chainId);
    }

    private void hideNetworkLabel() {
        testnet.setVisibility(View.GONE);
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
        }
    };

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            tokenLayout.setElevation(-10.0f);
            onTokenClickListener.onTokenClick(v, token, null, true);
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

    private void setIssuerDetails()
    {
        if (token.isEthereum())     // If token is eth and we get here, it's a testnet chain, show testnet
        {
            issuer.setVisibility(View.VISIBLE);
            issuer.setText(R.string.testnet);
            issuerPlaceholder.setVisibility(View.GONE);
            primaryElement = true;
        }
        else
        {
            String issuerName = assetDefinition.getIssuerName(token);
            if (issuerName != null && !issuerName.equalsIgnoreCase(getString(R.string.app_name))) //don't display issuer if it's alphawallet
            {
                issuer.setVisibility(View.VISIBLE);
                issuerPlaceholder.setVisibility(View.VISIBLE);
                primaryElement = true;
                issuer.setText(issuerName);
            }
            else
            {
                hideIssuerViews();
            }
        }
    }

    private void hideIssuerViews() {
        issuer.setVisibility(View.GONE);
        issuerPlaceholder.setVisibility(View.GONE);
        contractSeparator.setVisibility(View.GONE);
    }

    private void setContractType()
    {
        //Display contract type if required
        int contractStringId = token.getContractType();
        if (contractStringId > 0)
        {
            contractType.setText(contractStringId);
            contractType.setVisibility(View.VISIBLE);
            if (primaryElement) contractSeparator.setVisibility(View.VISIBLE);
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

    private void startTickerRealmListener()
    {
        realmUpdate = realm.where(RealmTokenTicker.class)
                .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                .findAllAsync();
        realmUpdate.addChangeListener(realmTicker -> {
            //update balance
            if (realmTicker.size() == 0) return;
            RealmTokenTicker rawTicker = realmTicker.first();
            if (rawTicker == null) return;
            //update ticker info
            final TokenTicker tt = new TokenTicker(rawTicker.getPrice(), rawTicker.getPercentChange24h(), rawTicker.getCurrencySymbol(),
                    rawTicker.getImage(), rawTicker.getUpdatedTime());
            handler.post(() -> {
                setTickerInfo(tt);
            });
        });
    }

    private void setTickerInfo(TokenTicker ticker)
    {
        if (((Activity)getContext()).isFinishing() || ((Activity) getContext()).isDestroyed()) { return; }

        //Set the fiat equivalent (leftmost value)
        BigDecimal correctedBalance = token.getCorrectedBalance(18);
        BigDecimal fiatBalance = correctedBalance.multiply(new BigDecimal(ticker.price)).setScale(18, RoundingMode.DOWN);
        String converted = TickerService.getCurrencyString(fiatBalance.doubleValue());
        String formattedPercents = "";
        int color = Color.RED;

        String lbl = getString(R.string.token_balance, "", converted);
        lbl += " " + ticker.priceSymbol;
        Spannable spannable;
        if (correctedBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                    converted.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.balanceCurrency.setText(lbl);
            this.issuer.setVisibility(View.GONE);
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
            formattedPercents = (percentage < 0 ? "(" : "(+") + ticker.percentChange24h + "%)";
            text24Hours.setText(formattedPercents);
            text24Hours.setTextColor(color);
        } catch (Exception ex) { /* Quietly */ }

        //This sets the crypto price value (middle amount)
        String formattedValue = TickerService.getCurrencyWithoutSymbol(new BigDecimal(ticker.price).doubleValue());

        lbl = getString(R.string.token_balance, "", formattedValue);
        lbl += " " + ticker.priceSymbol;
        spannable = new SpannableString(lbl);
        spannable.setSpan(new ForegroundColorSpan(color),
                lbl.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.textAppreciation.setText(lbl);

        tokensService.addTokenValue(token.tokenInfo.chainId, token.getAddress(), fiatBalance.floatValue());
    }
}