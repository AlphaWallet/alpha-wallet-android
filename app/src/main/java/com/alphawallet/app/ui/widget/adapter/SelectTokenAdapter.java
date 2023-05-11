package com.alphawallet.app.ui.widget.adapter;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.TokenFilter;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.alphawallet.app.widget.TokenIcon;

import java.util.ArrayList;
import java.util.List;

public class SelectTokenAdapter extends RecyclerView.Adapter<SelectTokenAdapter.ViewHolder>
{
    private final List<Token> displayData;
    private final TokenFilter tokenFilter;
    private final SelectTokenDialog.OnTokenClickListener listener;

    public SelectTokenAdapter(List<Token> tokens, SelectTokenDialog.OnTokenClickListener listener)
    {
        this.listener = listener;
        tokenFilter = new TokenFilter(tokens);
        displayData = new ArrayList<>();
        displayData.addAll(tokens);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_select_token, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.setIsRecyclable(false);
        Token token = displayData.get(position);
        if (token != null)
        {
            holder.name.setText(token.tokenInfo.name);
            holder.tokenIcon.bindData(token);

            String balance = token.getStringBalanceForUI(token.tokenInfo.decimals);
            if (!TextUtils.isEmpty(balance))
            {
                holder.balance.setText(balance);
                holder.balance.append(" ");
            }
            else
            {
                holder.balance.setText("0 ");
            }

            holder.balance.append(token.getSymbol());

            holder.itemLayout.setOnClickListener(v -> listener.onTokenClicked(token));
        }
    }

    public void filter(String keyword)
    {
        updateList(tokenFilter.filterBy(keyword));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Token> filteredList)
    {
        displayData.clear();
        displayData.addAll(filteredList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount()
    {
        return displayData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView name;
        TextView balance;
        View itemLayout;
        TokenIcon tokenIcon;

        ViewHolder(View view)
        {
            super(view);
            name = view.findViewById(R.id.name);
            balance = view.findViewById(R.id.balance);
            itemLayout = view.findViewById(R.id.layout_list_item);
            tokenIcon = view.findViewById(R.id.token_icon);
        }
    }
}
