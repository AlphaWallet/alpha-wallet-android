package com.alphawallet.app.ui.widget.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.opensea.AssetTrait;

import java.text.DecimalFormat;
import java.util.List;

public class TraitsAdapter extends RecyclerView.Adapter<TraitsAdapter.ViewHolder> {
    private List<AssetTrait> traitList;

    public TraitsAdapter(List<AssetTrait> data)
    {
        this.traitList = data;
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
        AssetTrait trait = traitList.get(i);
        viewHolder.trait.setText(trait.getTraitType());
        viewHolder.value.setText(trait.getValue());

        float rarity = trait.getTraitRarity();
        if (rarity == 0)
        {
            viewHolder.rarity.setText("Unique");
        }
        else
        {
            DecimalFormat df = new DecimalFormat("#0");
            viewHolder.rarity.setText(df.format(rarity) + "% have this trait");
        }
    }

    @Override
    public int getItemCount()
    {
        return traitList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
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
