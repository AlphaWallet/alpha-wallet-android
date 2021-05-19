package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.ui.widget.entity.PriceAlertCallback;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class PriceAlertAdapter extends RecyclerView.Adapter<PriceAlertAdapter.PriceAlertViewHolder> {
    private List<PriceAlert> items;
    private Context context;
    private PriceAlertCallback callback;

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

        // TODO: Format currency value
        holder.value.setText(alert.getValue());

        holder.alertSwitch.setChecked(alert.isEnabled());

        holder.alertSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
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

    public void add(PriceAlert item) {
        items.add(item);
        notifyItemChanged(items.size()-1);
    }

    public void remove(int position) {
        items.remove(position);
        notifyDataSetChanged();
    }

    public List<PriceAlert> getItems()
    {
        return items;
    }

    public void setItems(ArrayList<PriceAlert> items)
    {
        this.items = items;
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
