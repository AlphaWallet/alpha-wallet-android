package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.widget.TokenIcon;

import timber.log.Timber;

public class TestNetHorizontalListAdapter extends RecyclerView.Adapter<TestNetHorizontalListAdapter.ViewHolder>
{
    private final Token[] tokens;
    private final Context context;

    public  TestNetHorizontalListAdapter(Token[] tokens, Context context)
    {
        this.tokens = tokens;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horizontal_testnet_list, parent, false);
        return new TestNetHorizontalListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.tokenIcon.clearLoad();
        try
        {
            String coinBalance = tokens[position].getStringBalanceForUI(4);
            if (!TextUtils.isEmpty(coinBalance))
            {
                holder.tokenPrice.setText(context.getString(R.string.valueSymbol, coinBalance, tokens[position].getTokenSymbol(tokens[position])));
            }
            holder.tokenIcon.bindData(tokens[position].tokenInfo.chainId);
            if (!tokens[position].isEthereum())
            {
                holder.tokenIcon.setChainIcon(tokens[position].tokenInfo.chainId); //Add in when we upgrade the design
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    @Override
    public int getItemCount()
    {
        return tokens.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TokenIcon tokenIcon;
        TextView tokenPrice;
        ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            tokenIcon = itemView.findViewById(R.id.token_icon);
            tokenPrice = itemView.findViewById(R.id.title_set_price);
        }
    }
}

