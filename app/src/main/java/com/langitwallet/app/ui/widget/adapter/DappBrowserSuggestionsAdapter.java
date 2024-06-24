package com.langitwallet.app.ui.widget.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.langitwallet.app.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.langitwallet.app.entity.DApp;
import com.langitwallet.app.ui.widget.entity.ItemClickListener;
import com.langitwallet.app.ui.widget.entity.SuggestionsFilter;
import com.langitwallet.app.util.DappBrowserUtils;
import com.langitwallet.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class DappBrowserSuggestionsAdapter extends ArrayAdapter<DApp> implements Filterable {
    private final List<DApp> suggestions;
    public List<DApp> filteredSuggestions;
    private final ItemClickListener listener;
    private final Vibrator vibrate;

    public DappBrowserSuggestionsAdapter(@NonNull Context context,
                                         List<DApp> suggestions,
                                         ItemClickListener listener) {
        super(context, 0, suggestions);
        this.suggestions = suggestions;
        this.listener = listener;
        this.filteredSuggestions = new ArrayList<>();
        this.vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        //this.text = "";

        // Append browser history to known DApps list during initialisation
        addSuggestions(DappBrowserUtils.getBrowserHistory(context));
    }

    public void addSuggestion(DApp dapp) {
        if (!suggestions.contains(dapp)) {
            suggestions.add(dapp);
            notifyDataSetChanged();
        }
    }

    public void addSuggestions(List<DApp> dapps) {
        for (DApp d : dapps) {
            if (!suggestions.contains(d)) {
                suggestions.add(d);
            }
        }
        notifyDataSetChanged();
    }

    public void removeSuggestion(String dappUrl)
    {
        filterList(suggestions, dappUrl);
        filterList(filteredSuggestions, dappUrl);
    }

    private void filterList(List<DApp> dappList, String urlToRemove)
    {
        List<Integer> removeList = new ArrayList<>();

        for (int i = 0; i < dappList.size(); i++)
        {
            if (dappList.get(i).getUrl().equals(urlToRemove))
            {
                removeList.add(i);
            }
        }

        removeList.sort((d1, d2) -> Integer.compare(d2, d1));

        //remove in reverse order
        for (Integer i : removeList)
        {
            dappList.remove((int)i);
        }
    }

    @Override
    public int getCount() {
        return filteredSuggestions.size();
    }

    @Nullable
    @Override
    public DApp getItem(int position) {
        return filteredSuggestions.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new SuggestionsFilter(this, suggestions);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        DApp dapp = filteredSuggestions.get(position);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_browser_suggestion, parent, false);
        }

        RelativeLayout layout = convertView.findViewById(R.id.layout);
        layout.setOnClickListener(v -> listener.onItemClick(dapp.getUrl()));
        layout.setOnLongClickListener(v -> {
            vibrate.vibrate(100);
            listener.onItemLongClick(dapp.getUrl());
            return true;
        });

        ImageView icon = convertView.findViewById(R.id.icon);
        String visibleUrl = Utils.getDomainName(dapp.getUrl());

        String favicon;
        if (!TextUtils.isEmpty(visibleUrl)) {
            favicon = DappBrowserUtils.getIconUrl(visibleUrl);
            Glide.with(icon.getContext())
                    .load(favicon)//.load(favicon)
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .listener(requestListener)
                    .into(icon);
        }

        TextView name = convertView.findViewById(R.id.name);
        TextView description = convertView.findViewById(R.id.description);

        name.setText(dapp.getName());
        if (!TextUtils.isEmpty(dapp.getDescription()))
        {
            description.setText(dapp.getDescription());
        }
        else
        {
            description.setText(dapp.getUrl());
        }

        return convertView;
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
}
