package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.ui.widget.entity.ItemClickListener;
import io.stormbird.wallet.ui.widget.entity.SuggestionsFilter;

public class DappBrowserSuggestionsAdapter extends ArrayAdapter<DApp> implements Filterable {
    private final List<DApp> suggestions;
    public List<DApp> filteredSuggestions;
    private ItemClickListener listener;
    private String text;
    private TextView name;

    public DappBrowserSuggestionsAdapter(@NonNull Context context,
                                         List<DApp> suggestions,
                                         ItemClickListener listener) {
        super(context, 0, suggestions);
        this.suggestions = suggestions;
        this.listener = listener;
        this.filteredSuggestions = new ArrayList<>();
        this.text = "";
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
        layout.setOnClickListener(v-> listener.onItemClick(dapp.getUrl()));

        name = convertView.findViewById(R.id.name);
        TextView description = convertView.findViewById(R.id.description);
        TextView url = convertView.findViewById(R.id.url);

        name.setText(dapp.getName());
        description.setText(dapp.getDescription());
        url.setText(dapp.getUrl());

        highlightSearch(text, dapp.getName());

        return convertView;
    }

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
            this.name.setText(builder);
        } else {
            this.name.setText(name);
        }
    }

    public void setHighlighted(String text) {
        this.text = text;
        notifyDataSetChanged();
    }
}
