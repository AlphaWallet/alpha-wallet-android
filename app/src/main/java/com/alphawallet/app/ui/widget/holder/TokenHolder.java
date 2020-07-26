package com.alphawallet.app.ui.widget.holder;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

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
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.realm.Realm;
import io.realm.RealmResults;

public class TokenHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, View.OnLongClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    private final TextView balanceEth;
    private final TextView balanceCurrency;
    private final ImageView icon;
    private final TextView textIcon;
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
    private final CustomViewTarget viewTarget;
    private RealmResults<RealmTokenTicker> realmUpdate = null;
    private String tokenName;
    private boolean primaryElement;

    private Handler handler;

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs)
    {
        super(resId, parent);

        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
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
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        tokensService = tSvs;

        icon.setVisibility(View.INVISIBLE);
        textIcon.setVisibility(View.GONE);

        viewTarget = new CustomViewTarget<ImageView, BitmapDrawable>(icon) {
            @Override
            protected void onResourceCleared(@Nullable Drawable placeholder) { }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable)
            {
                setupTextIcon(token);
            }

            @Override
            public void onResourceReady(@NotNull BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition)
            {
                textIcon.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageDrawable(bitmap);
            }
        };
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
            tokenName = token.getFullName(assetDefinition, token.getTicketCount());
            contractSeparator.setVisibility(View.GONE);

            //setup name and value (put these together on a single string to make wrap-around text appear better).
            String nameValue = token.getStringBalance() + " " + tokenName;
            balanceEth.setText(nameValue);

            primaryElement = false;

            displayTokenIcon();

            populateTicker();

            setContractType();

            setPendingAmount();
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
        TokenTicker ticker = tokensService.getTokenTicker(token);
        if (ticker == null)
        {
            balanceCurrency.setVisibility(View.GONE);
            layoutAppreciation.setVisibility(View.GONE);
            if (token.isEthereum()) animateTextWhileWaiting();
            else setIssuerDetails();
            return;
        }

        primaryElement = true;
        hideIssuerViews();
        layoutAppreciation.setVisibility(View.VISIBLE);
        balanceCurrency.setVisibility(View.VISIBLE);
        stopTextAnimation();

        startRealmListener();
    }

    private void displayTokenIcon()
    {
        int chainIcon = EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId);

        // This appears more complex than necessary;
        // This is because we are dealing with: new token holder view, refreshing views and recycled views
        // If the token is a basechain token, immediately show the chain icon - no need to load
        // Otherwise, try to load the icon resource. If there's no icon resource then generate a text token Icon (round circle with first four chars from the name)
        // Only reveal the icon immediately before populating it - this stops the update flicker.
        if (token.isEthereum())
        {
            textIcon.setVisibility(View.GONE);
            icon.setImageResource(chainIcon);
            icon.setVisibility(View.VISIBLE);
        }
        else
        {
            setupTextIcon(token);
            IconItem iconItem = assetDefinition.fetchIconForToken(token);

            Glide.with(getContext().getApplicationContext())
                    .load(iconItem.getUrl())
                    .signature(iconItem.getSignature())
                    .onlyRetrieveFromCache(iconItem.onlyFetchFromCache()) //reduce URL checking, only check once per session
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(chainIcon))
                    .listener(requestListener)
                    .into(viewTarget);
        }
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

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

    private void setupTextIcon(@NotNull Token token) {
        icon.setVisibility(View.GONE);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
        textIcon.setText(Utils.getIconisedText(tokenName));
    }

    private void animateTextWhileWaiting() {
        emptyTicker();
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

    private void hideIssuerViews() {
        issuer.setVisibility(View.GONE);
        issuerPlaceholder.setVisibility(View.GONE);
        contractSeparator.setVisibility(View.GONE);
    }

    private void stopTextAnimation() {
        text24Hours.clearAnimation();
        textAppreciation.clearAnimation();
        balanceCurrency.clearAnimation();
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

    private void startRealmListener()
    {
        Realm realm = tokensService.getTickerRealmInstance();
        realmUpdate = realm.where(RealmTokenTicker.class)
                .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                .findAllAsync();
        realmUpdate.addChangeListener(realmTicker -> {
            //update balance
            if (realmTicker.size() == 0) return;
            RealmTokenTicker rawTicker = realmTicker.first();
            if (rawTicker == null) return;
            //update ticker info
            TokenTicker tt = new TokenTicker(rawTicker.getPrice(), rawTicker.getPercentChange24h(), rawTicker.getCurrencySymbol(),
                    rawTicker.getImage(), rawTicker.getUpdatedTime());
            setTickerInfo(tt);
            if (token.tokenInfo.chainId == 1 && token.isEthereum())
            {
                Toast.makeText(getContext(), "Ticker Updated: $" + rawTicker.getPrice(), Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void setTickerInfo(TokenTicker ticker)
    {
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