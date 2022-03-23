package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.opensea.Trait;

import java.text.DecimalFormat;
import java.util.List;

public class TraitsAdapter extends RecyclerView.Adapter<TraitsAdapter.ViewHolder> {
    private List<Trait> traitList;
    private Context context;

    public TraitsAdapter(Context context, List<Trait> data)
    {
        this.context = context;
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
        Trait trait = traitList.get(i);
        viewHolder.trait.setText(trait.getTraitType());
        viewHolder.value.setText(trait.getValue());

        float rarity = trait.getTraitRarity();
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
