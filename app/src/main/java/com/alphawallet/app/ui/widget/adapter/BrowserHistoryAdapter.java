package com.alphawallet.app.ui.widget.adapter;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.Utils;

import java.net.URISyntaxException;
import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.OnHistoryItemRemovedListener;
import com.bumptech.glide.request.target.Target;

public class BrowserHistoryAdapter extends RecyclerView.Adapter<BrowserHistoryAdapter.ViewHolder> {
    private List<DApp> data;
    private OnDappClickListener listener;
    private OnHistoryItemRemovedListener onHistoryItemRemovedListener;

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, url, remove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            remove = itemView.findViewById(R.id.remove);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            url = itemView.findViewById(R.id.url);
        }
    }

    public BrowserHistoryAdapter(List<DApp> data,
                                 OnDappClickListener listener,
                                 OnHistoryItemRemovedListener onHistoryItemRemovedListener) {
        this.data = data;
        this.listener = listener;
        this.onHistoryItemRemovedListener = onHistoryItemRemovedListener;
    }

    public void setDapps(List<DApp> dapps) {
        this.data = dapps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BrowserHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_browser_history, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrowserHistoryAdapter.ViewHolder viewHolder, int i) {
        DApp dApp = data.get(i);
        String visibleUrl = Utils.getDomainName(dApp.getUrl());
        viewHolder.name.setText(dApp.getName());
        viewHolder.url.setText(dApp.getUrl());

        String favicon;
        if (!TextUtils.isEmpty(visibleUrl)) {
            favicon = DappBrowserUtils.getIconUrl(visibleUrl);
            Glide.with(viewHolder.icon.getContext())
                    .load(favicon)
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .listener(requestListener)
                    .into(viewHolder.icon);

            viewHolder.icon.setOnClickListener(v -> listener.onDappClick(dApp));
        }

        viewHolder.remove.setOnClickListener(v ->
                onHistoryItemRemovedListener.onHistoryItemRemoved(dApp));
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    @Override
    public int getItemCount() {
        return data.size();
    }
}
