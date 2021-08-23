package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.adapter.NFTAssetCountAdapter;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.LocaleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import io.realm.Realm;

/**
 * Created by Jenny Jingjing Li on 21/03/2021
 * */

public class AmountDisplayWidget extends LinearLayout {

    private final Locale deviceSettingsLocale = LocaleUtils.getDeviceLocale(getContext());
    private final TextView amount;
    private final RecyclerView tokensList;

    public AmountDisplayWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);

        inflate(context, R.layout.item_amount_display, this);
        amount = findViewById(R.id.text_amount);
        tokensList = findViewById(R.id.tokens_list);
    }

    public void setAmountFromString(String displayStr)
    {
        amount.setText(displayStr);
    }

    public void setAmountFromBigInteger(BigInteger txAmount, String token)
    {
        NumberFormat decimalFormat = NumberFormat.getInstance(deviceSettingsLocale);
        setAmountFromString(decimalFormat.format(txAmount) + ' ' + token);
    }

    public void setAmountFromAssetList(List<NFTAsset> assets)
    {
        amount.setVisibility(View.GONE);
        tokensList.setVisibility(View.VISIBLE);

        //display assets
        NFTAssetCountAdapter adapter = new NFTAssetCountAdapter(assets);
        tokensList.setAdapter(adapter);
    }

    private String getValueString(BigInteger amount, Token token, TokensService tokensService)
    {
        String formattedValue = BalanceUtils.getScaledValueMinimal(amount, token.tokenInfo.decimals);
        //fetch ticker if required
        try (Realm realm = tokensService.getTickerRealmInstance())
        {
            RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                    .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                    .findFirst();

            if (rtt != null)
            {
                //calculate equivalent fiat
                BigDecimal cryptoRate = new BigDecimal(rtt.getPrice());
                BigDecimal cryptoAmount = BalanceUtils.subunitToBase(amount, token.tokenInfo.decimals);
                return getContext().getString(R.string.fiat_format, formattedValue, token.getSymbol(),
                        TickerService.getCurrencyString(cryptoAmount.multiply(cryptoRate).doubleValue()),
                        rtt.getCurrencySymbol());
            }
        }
        catch (Exception e)
        {
            //
        }

        return getContext().getString(R.string.total_cost, formattedValue, token.getSymbol());
    }

    public void setAmountUsingToken(BigInteger amountValue, Token token, TokensService tokensService)
    {
        amount.setText(getValueString(amountValue, token, tokensService));
    }
}