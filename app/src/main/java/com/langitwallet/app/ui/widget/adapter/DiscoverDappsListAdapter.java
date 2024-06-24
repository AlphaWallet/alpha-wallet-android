package com.langitwallet.app.ui.widget.adapter;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
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

import java.util.List;

import com.langitwallet.app.R;
import com.bumptech.glide.request.target.Target;
import com.langitwallet.app.entity.DApp;
import com.langitwallet.app.ui.widget.OnDappAddedListener;
import com.langitwallet.app.ui.widget.OnDappClickListener;
import com.langitwallet.app.ui.widget.OnDappRemovedListener;
import com.langitwallet.app.util.DappBrowserUtils;
import com.langitwallet.app.util.Utils;

public class DiscoverDappsListAdapter extends RecyclerView.Adapter<DiscoverDappsListAdapter.ViewHolder> {
    private List<DApp> data;
    private final OnDappClickListener listener;
    private final OnDappAddedListener onDappAddedListener;
    private final OnDappRemovedListener onDappRemovedListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, description, remove, add;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            remove = itemView.findViewById(R.id.remove);
            add = itemView.findViewById(R.id.add);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            description = itemView.findViewById(R.id.description);
        }
    }

    public DiscoverDappsListAdapter(List<DApp> data,
                                    OnDappClickListener listener,
                                    OnDappAddedListener onDappAddedListener,
                                    OnDappRemovedListener onDappRemovedListener) {
        this.data = data;
        this.listener = listener;
        this.onDappAddedListener = onDappAddedListener;
        this.onDappRemovedListener = onDappRemovedListener;
    }

    public void setDapps(List<DApp> dapps) {
        this.data = dapps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_discover_dapps, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DApp dApp = data.get(i);
        String visibleUrl = Utils.getDomainName(dApp.getUrl());
        viewHolder.name.setText(dApp.getName());
        viewHolder.description.setText(dApp.getDescription());

        String favicon;
        if (!TextUtils.isEmpty(visibleUrl)) {
            favicon = DappBrowserUtils.getIconUrl(visibleUrl);
            Glide.with(viewHolder.icon.getContext())
                    .load(favicon)
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .listener(requestListener)
                    .into(viewHolder.icon);

            viewHolder.icon.setOnClickListener(v -> {
                listener.onDappClick(dApp);
            });
        }


        if (dApp.isAdded()) {
            viewHolder.add.setVisibility(View.GONE);
            viewHolder.remove.setVisibility(View.VISIBLE);
        } else {
            viewHolder.add.setVisibility(View.VISIBLE);
            viewHolder.remove.setVisibility(View.GONE);
        }

        viewHolder.add.setOnClickListener(v -> {
            onDappAddedListener.onDappAdded(dApp);
            viewHolder.add.setVisibility(View.GONE);
            viewHolder.remove.setVisibility(View.VISIBLE);
        });

        viewHolder.remove.setOnClickListener(v -> {
            onDappRemovedListener.onDappRemoved(dApp);
            viewHolder.add.setVisibility(View.VISIBLE);
            viewHolder.remove.setVisibility(View.GONE);
        });
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
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
