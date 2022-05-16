package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;

import java.text.DecimalFormat;
import java.util.List;

public class TraitsAdapter extends RecyclerView.Adapter<TraitsAdapter.ViewHolder> {
    private final List<OpenSeaAsset.Trait> traitList;
    private final Context context;
    private final long totalSupply;

    public TraitsAdapter(Context context, List<OpenSeaAsset.Trait> data, long totalSupply)
    {
        this.context = context;
        this.traitList = data;
        this.totalSupply = totalSupply;
    }

    @NonNull
    @Override
    public TraitsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_attribute, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TraitsAdapter.ViewHolder viewHolder, int i)
    {
        OpenSeaAsset.Trait trait = traitList.get(i);
        viewHolder.trait.setText(trait.traitType);
        viewHolder.value.setText(trait.value);

        if (trait.traitCount > 0)
        {
            viewHolder.rarity.setVisibility(View.VISIBLE);
            float rarity = trait.getTraitRarity(totalSupply);
            if (trait.isUnique())
            {
                viewHolder.rarity.setText(R.string.trait_rarity_unique);
            }
            else
            {
                DecimalFormat df = new DecimalFormat("#0");
                String s = context.getString(R.string.trait_rarity_suppl_text, df.format(rarity));
                viewHolder.rarity.setText(s);
            }
        }
        else
        {
            viewHolder.rarity.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount()
    {
        return traitList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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
