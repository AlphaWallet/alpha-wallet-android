package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.ui.widget.entity.PriceAlertCallback;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.alphawallet.app.service.TickerService.getCurrencyWithoutSymbol;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class PriceAlertAdapter extends RecyclerView.Adapter<PriceAlertAdapter.PriceAlertViewHolder>
{
    private List<PriceAlert> items;
    private final Context context;
    private final PriceAlertCallback callback;

    public PriceAlertAdapter(Context context, List<PriceAlert> items, PriceAlertCallback callback)
    {
        this.items = items;
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public PriceAlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_token_price_alert, parent, false);
        return new PriceAlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PriceAlertViewHolder holder, int position)
    {
        PriceAlert alert = items.get(position);

        int indicator;
        if (alert.getAbove())
        {
            holder.icon.setImageResource(R.drawable.ic_system_up);
            indicator = R.string.price_alert_indicator_above;
        } else
        {
            holder.icon.setImageResource(R.drawable.ic_system_down);
            indicator = R.string.price_alert_indicator_below;
        }

        CurrencyItem currencyItem = CurrencyRepository.getCurrencyByISO(alert.getCurrency());
        holder.rule.setText(format("%s %s%s", context.getText(indicator), requireNonNull(currencyItem).getSymbol(), getCurrencyWithoutSymbol(Double.parseDouble(alert.getValue()))));

        holder.alertSwitch.setChecked(alert.isEnabled());

        holder.alertSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            PriceAlert item = items.get(position);
            item.setEnabled(isChecked);
            notifyItemChanged(position);
            callback.onCheckChanged(isChecked, position);
        });
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    public void add(PriceAlert item)
    {
        items.add(item);
        notifyItemChanged(items.size() - 1);
    }

    public void remove(int position)
    {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public List<PriceAlert> getItems()
    {
        return items;
    }

    public void setItems(ArrayList<PriceAlert> items)
    {
        this.items = items;
    }

    public static class PriceAlertViewHolder extends RecyclerView.ViewHolder
    {
        ImageView icon;
        TextView rule;
        SwitchMaterial alertSwitch;

        public PriceAlertViewHolder(@NonNull View itemView)
        {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            rule = itemView.findViewById(R.id.rule);
            alertSwitch = itemView.findViewById(R.id.alert_switch);
        }
    }
}
