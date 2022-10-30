package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.widget.SwapSettingsDialog;
import com.alphawallet.app.widget.TokenIcon;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.List;

public class SelectChainAdapter extends RecyclerView.Adapter<SelectChainAdapter.ViewHolder>
{
    private List<Chain> chains;
    private Context context;
    private SwapSettingsDialog.SwapSettingsInterface callback;
    private long selectedChainId;

    public SelectChainAdapter(Context context, List<Chain> chains, SwapSettingsDialog.SwapSettingsInterface callback)
    {
        this.context = context;
        this.chains = chains;
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_chain_select;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        holder.setIsRecyclable(false);
        Chain item = chains.get(position);
        if (item != null)
        {
            holder.name.setText(item.metamask.chainName);
            holder.chainId.setText(context.getString(R.string.chain_id, item.id));
            holder.chainIcon.bindData(item.id);

            if (item.id == selectedChainId)
            {
                holder.radio.setChecked(true);
            }

            holder.itemLayout.setOnClickListener(v -> callback.onChainSelected(item));
        }
    }

    @Override
    public int getItemCount()
    {
        return chains.size();
    }

    public void setChains(List<Chain> chains)
    {
        this.chains = chains;
        notifyItemRangeChanged(0, getItemCount());
    }

    public long getSelectedChain()
    {
        return this.selectedChainId;
    }

    public void setSelectedChain(long selectedChainId)
    {
        this.selectedChainId = selectedChainId;
        notifyItemRangeChanged(0, getItemCount());
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        MaterialRadioButton radio;
        TextView name;
        TextView chainId;
        View itemLayout;
        TokenIcon chainIcon;

        ViewHolder(View view)
        {
            super(view);
            radio = view.findViewById(R.id.radio);
            name = view.findViewById(R.id.name);
            chainId = view.findViewById(R.id.chain_id);
            itemLayout = view.findViewById(R.id.layout_list_item);
            chainIcon = view.findViewById(R.id.chain_icon);
        }
    }
}
