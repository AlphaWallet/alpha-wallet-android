package com.alphawallet.app.ui.widget.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.LifiToken;
import com.alphawallet.app.util.LifiTokenFilter;
import com.alphawallet.app.widget.AddressIcon;
import com.alphawallet.app.widget.SelectLifiTokenDialog;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

public class SelectLifiTokenAdapter extends RecyclerView.Adapter<SelectLifiTokenAdapter.ViewHolder>
{
    private final List<LifiToken> displayData;
    private final SelectLifiTokenDialog.EventListener callback;
    private final LifiTokenFilter lifiTokenFilter;
    private String selectedTokenAddress;

    public SelectLifiTokenAdapter(List<LifiToken> tokens, SelectLifiTokenDialog.EventListener callback)
    {
        lifiTokenFilter = new LifiTokenFilter(tokens);
        this.callback = callback;
        displayData = new ArrayList<>();
        displayData.addAll(tokens);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_token_select;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        LifiToken item = displayData.get(position);
        if (item != null)
        {
            holder.name.setText(item.name);
            holder.name.append(" (");
            holder.name.append(item.symbol);
            holder.name.append(")");

            holder.tokenIcon.bindData(item.logoURI, item.chainId, selectedTokenAddress, item.symbol);

            String balance = item.balance;
            if (!TextUtils.isEmpty(balance))
            {
                holder.balance.setText(balance);
                holder.balance.append(" ");
            }
            else
            {
                holder.balance.setText("0 ");
            }

            holder.radio.setChecked(item.address.equalsIgnoreCase(selectedTokenAddress));
            holder.balance.append(item.symbol);

            holder.itemLayout.setOnClickListener(v -> callback.onChainSelected(item));
        }
    }

    public void filter(String keyword)
    {
        updateList(lifiTokenFilter.filterBy(keyword));
    }

    public void updateList(List<LifiToken> filteredList)
    {
        displayData.clear();
        displayData.addAll(filteredList);
        notifyDataSetChanged();
    }

    public void setSelectedToken(String address)
    {
        selectedTokenAddress = address;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount()
    {
        return displayData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        MaterialRadioButton radio;
        TextView name;
        TextView balance;
        View itemLayout;
        AddressIcon tokenIcon;

        ViewHolder(View view)
        {
            super(view);
            radio = view.findViewById(R.id.radio);
            name = view.findViewById(R.id.name);
            balance = view.findViewById(R.id.balance);
            itemLayout = view.findViewById(R.id.layout_list_item);
            tokenIcon = view.findViewById(R.id.token_icon);
        }
    }
}
