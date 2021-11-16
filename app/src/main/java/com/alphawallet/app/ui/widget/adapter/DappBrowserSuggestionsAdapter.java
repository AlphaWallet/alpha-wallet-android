package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.Utils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.ui.widget.entity.SuggestionsFilter;
import com.bumptech.glide.request.target.Target;

public class DappBrowserSuggestionsAdapter extends ArrayAdapter<DApp> implements Filterable {
    private final List<DApp> suggestions;
    public List<DApp> filteredSuggestions;
    private final ItemClickListener listener;
//    private String text;
//    private TextView name;

    public DappBrowserSuggestionsAdapter(@NonNull Context context,
                                         List<DApp> suggestions,
                                         ItemClickListener listener) {
        super(context, 0, suggestions);
        this.suggestions = suggestions;
        this.listener = listener;
        this.filteredSuggestions = new ArrayList<>();
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

    public void removeSuggestion(DApp dapp) {
        for (DApp d : suggestions) {
            if (d.getName().equals(dapp.getName()) && d.getUrl().equals(dapp.getUrl())) {
                suggestions.remove(d);
                break;
            }
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

        //highlightSearch(text, dapp.getName());

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

    private void highlightSearch(String text, String name) {
        String lowerCaseText;
        String lowerCaseName;
        if (!text.isEmpty()) {
            lowerCaseName = name.toLowerCase();
            lowerCaseText = text.toLowerCase();
            int start = lowerCaseName.indexOf(lowerCaseText);
            int end = lowerCaseText.length() + start;
            SpannableStringBuilder builder = new SpannableStringBuilder(name);
            if (start >= 0) {
                int highlightColor = ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
                builder.setSpan(new ForegroundColorSpan(highlightColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            //this.name.setText(builder);
        } else {
            //this.name.setText(name);
        }
    }

    public void setHighlighted(String text) {
        //this.text = text;
        notifyDataSetChanged();
    }
}
