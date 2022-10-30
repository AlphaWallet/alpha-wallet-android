package com.alphawallet.app.ui.widget.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.widget.AddressIcon;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

public class SelectTokenAdapter extends RecyclerView.Adapter<SelectTokenAdapter.ViewHolder>
{
    private final List<Token> displayData;
    private final SelectTokenDialog.SelectTokenDialogEventListener callback;
    private final TokenFilter tokenFilter;
    private String selectedTokenAddress;

    public SelectTokenAdapter(List<Token> tokens, SelectTokenDialog.SelectTokenDialogEventListener callback)
    {
        tokenFilter = new TokenFilter(tokens);
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
        Token item = displayData.get(position);
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
        updateList(tokenFilter.filterBy(keyword));
    }

    public void updateList(List<Token> filteredList)
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
