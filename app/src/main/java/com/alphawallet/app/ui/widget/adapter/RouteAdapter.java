package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Route;
import com.alphawallet.app.ui.widget.entity.OnRouteSelectedListener;
import com.alphawallet.app.util.SwapUtils;
import com.alphawallet.app.widget.AddressIcon;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder>
{
    private final Context context;
    private final List<Route> data;
    private final OnRouteSelectedListener listener;

    public RouteAdapter(Context context, List<Route> data, OnRouteSelectedListener listener)
    {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_route;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Route item = data.get(position);
        if (item != null)
        {
            Route.Step step = item.steps.get(0);

            holder.provider.setText(context.getString(R.string.label_swap_via, step.swapProvider.name));

            for (String tag : item.tags)
            {
                if (tag.equalsIgnoreCase("RECOMMENDED"))
                {
                    holder.tag.setVisibility(View.VISIBLE);
                    holder.tag.setText(tag);
                }
            }

            holder.value.setText(SwapUtils.getFormattedMinAmount(step.estimate, step.action));
            holder.icon.bindData(step.action.toToken.logoURI, step.action.toToken.chainId, step.action.toToken.address, step.action.toToken.symbol);
//            holder.symbol.setText(step.action.toToken.symbol);
            holder.gas.setText(context.getString(R.string.info_gas_fee, SwapUtils.getTotalGasFees(step.estimate.gasCosts)));
            if (step.estimate.feeCosts != null && step.estimate.feeCosts.isEmpty())
            {
                holder.fees.setVisibility(View.VISIBLE);
                holder.fees.setText(SwapUtils.getOtherFees(step.estimate.feeCosts));
            }
            else
            {
                holder.fees.setVisibility(View.GONE);
            }
            holder.layout.setOnClickListener(v -> listener.onRouteSelected(step.swapProvider.key));
        }
    }

    @Override
    public int getItemCount()
    {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        MaterialCardView layout;
        TextView tag;
        TextView provider;
        TextView value;
        TextView symbol;
        TextView gas;
        TextView fees;
        TextView price;
        AddressIcon icon;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.layout);
            tag = view.findViewById(R.id.tag);
            provider = view.findViewById(R.id.provider);
            value = view.findViewById(R.id.value);
            symbol = view.findViewById(R.id.symbol);
            gas = view.findViewById(R.id.gas);
            fees = view.findViewById(R.id.fees);
            price = view.findViewById(R.id.price);
            icon = view.findViewById(R.id.token_icon);
        }
    }
}
