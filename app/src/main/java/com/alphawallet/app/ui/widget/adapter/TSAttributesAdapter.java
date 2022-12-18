package com.alphawallet.app.ui.widget.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.token.entity.TokenScriptResult;

import java.util.ArrayList;
import java.util.List;

public class TSAttributesAdapter extends RecyclerView.Adapter<TSAttributesAdapter.ViewHolder>
{
    private final List<TokenScriptResult.Attribute> attrList;

    public TSAttributesAdapter(List<TokenScriptResult.Attribute> attrs)
    {
        this.attrList = new ArrayList<>();
        for (TokenScriptResult.Attribute attr : attrs)
        {
            if (!TextUtils.isEmpty(attr.name))
            {
                this.attrList.add(attr);
            }
        }
    }

    @NonNull
    @Override
    public TSAttributesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_attribute, viewGroup, false);
        return new TSAttributesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TSAttributesAdapter.ViewHolder viewHolder, int i)
    {
        TokenScriptResult.Attribute attr = attrList.get(i);
        viewHolder.trait.setText(attr.name);
        viewHolder.value.setText(attr.attrValue());
        viewHolder.rarity.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount()
    {
        return attrList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView trait;
        TextView value;
        TextView rarity;

        ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            trait = itemView.findViewById(R.id.trait);
            value = itemView.findViewById(R.id.value);
            rarity = itemView.findViewById(R.id.rarity);
        }
    }
}
