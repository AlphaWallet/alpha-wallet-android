package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.PriceAlertItem;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class PriceAlertAdapter extends RecyclerView.Adapter<PriceAlertAdapter.PriceAlertViewHolder> {
    private ArrayList<PriceAlertItem> items;
    private Context context;

    public PriceAlertAdapter(Context context, ArrayList<PriceAlertItem> items)
    {
        this.items = items;
        this.context = context;
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
        PriceAlertItem alert = items.get(position);

        if (alert.getIndicator())
        {
            holder.icon.setImageResource(R.drawable.ic_system_up);
            holder.indicator.setText("Above ");
        }
        else
        {
            holder.icon.setImageResource(R.drawable.ic_system_down);
            holder.indicator.setText("Below ");
        }

        holder.value.setText(alert.getValue());
        holder.alertSwitch.setChecked(alert.isEnabled());
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }

    public static class PriceAlertViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView indicator;
        TextView value;
        SwitchMaterial alertSwitch;

        public PriceAlertViewHolder(@NonNull View itemView)
        {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            indicator = itemView.findViewById(R.id.indicator);
            value = itemView.findViewById(R.id.value);
            alertSwitch = itemView.findViewById(R.id.alert_switch);
        }
    }
}
