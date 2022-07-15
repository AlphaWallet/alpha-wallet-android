package com.alphawallet.app.ui.widget.adapter;


import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.widget.AddressIcon;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectTokenAdapter extends RecyclerView.Adapter<SelectTokenAdapter.ViewHolder>
{
    private final List<Connection.LToken> tokens;
    private final List<Connection.LToken> displayData;
    private final SelectTokenDialog.SelectTokenDialogEventListener callback;
    private String selectedTokenAddress;

    public SelectTokenAdapter(List<Connection.LToken> tokens, SelectTokenDialog.SelectTokenDialogEventListener callback)
    {
        this.tokens = tokens;
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
        Connection.LToken item = displayData.get(position);
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

    public void filter(String searchFilter)
    {
        List<Connection.LToken> filteredList = new ArrayList<>();
        for (Connection.LToken data : tokens)
        {
            if (data.name.toLowerCase(Locale.ENGLISH).startsWith(searchFilter.toLowerCase(Locale.ENGLISH)))
            {
                filteredList.add(data);
            }
            else if (data.symbol.toLowerCase(Locale.ENGLISH).startsWith(searchFilter.toLowerCase(Locale.ENGLISH)))
            {
                filteredList.add(data);
            }
        }

        for (Connection.LToken data : tokens)
        {
            if (data.name.toLowerCase(Locale.ENGLISH).contains(searchFilter.toLowerCase(Locale.ENGLISH)))
            {
                if (!filteredList.contains(data)) filteredList.add(data);
            }
            else if (data.symbol.toLowerCase(Locale.ENGLISH).contains(searchFilter.toLowerCase(Locale.ENGLISH)))
            {
                if (!filteredList.contains(data)) filteredList.add(data);
            }
        }

        updateList(filteredList);
    }

    public void updateList(List<Connection.LToken> filteredList)
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
