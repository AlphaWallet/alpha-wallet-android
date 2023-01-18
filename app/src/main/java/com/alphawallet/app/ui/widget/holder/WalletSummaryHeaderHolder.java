package com.alphawallet.app.ui.widget.holder;

import static com.alphawallet.app.ui.widget.holder.WalletHolder.FIAT_CHANGE;
import static com.alphawallet.app.ui.widget.holder.WalletHolder.FIAT_VALUE;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.realm.Realm;
import timber.log.Timber;

public class WalletSummaryHeaderHolder extends BinderViewHolder<Wallet> implements View.OnClickListener
{

    public static final int VIEW_TYPE = 1045;

    private TextView summaryBalance;
    private TextView summaryChange;

    public WalletSummaryHeaderHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);

        this.summaryBalance = findViewById(R.id.summary_balance);
        this.summaryChange = findViewById(R.id.summary_change);
    }

    @Override
    public void bind(@Nullable Wallet data, @NonNull Bundle addition)
    {
        //update using addition
        double fiatValue = addition.getDouble(FIAT_VALUE, 0.00);
        double oldFiatValue = addition.getDouble(FIAT_CHANGE, 0.00);

        String balanceTxt = TickerService.getCurrencyString(fiatValue);

        summaryBalance.setText(balanceTxt);
        setWalletChange(fiatValue, oldFiatValue);
    }

    private void setWalletChange(double fiatValue, double oldFiatValue)
    {
        try
        {
            double change24h = fiatValue - oldFiatValue;
            double percentChange24h = fiatValue != 0 ? (change24h / oldFiatValue) * 100.0 : 0.0;
            summaryChange.setVisibility(View.VISIBLE);
            int color = ContextCompat.getColor(getContext(), percentChange24h < 0 ? R.color.negative : R.color.positive);
            BigDecimal percentChangeBI = BigDecimal.valueOf(percentChange24h).setScale(3, RoundingMode.DOWN);
            String changeTxt = TickerService.getCurrencyString(change24h);
            String formattedPercents = (percentChange24h < 0 ? "" : "+") + percentChangeBI + "%";
            //wallet24hChange.setBackgroundResource(percentage < 0 ? R.drawable.background_24h_change_red : R.drawable.background_24h_change_green);
            summaryChange.setText(getString(R.string.summary_change_24h, changeTxt, formattedPercents));
            summaryChange.setTextColor(color);
            //image24h.setImageResource(percentage < 0 ? R.drawable.ic_price_down : R.drawable.ic_price_up);
        }
        catch (Exception ex)
        {
            Timber.w(ex);
        }
    }

    @Override
    public void onClick(View view)
    {

    }
}
