package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.entity.UrlFilter;

public class AutoCompleteUrlAdapter extends ArrayAdapter<String> {
    private final List<String> history;
    public List<String> filteredUrls = new ArrayList<>();
    private Context context;

    public AutoCompleteUrlAdapter(Context context, List<String> history) {
        super(context, 0, history);
        this.history = history;
        this.context = context;
    }

    @Override
    public int getCount() {
        return filteredUrls.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return filteredUrls.get(position);
    }

    @Override
    public Filter getFilter() {
        return new UrlFilter(this, history);
    }

    @Override
    public void add(@Nullable String object) {
        if (!history.contains(object)) {
            super.insert(object, 0);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = filteredUrls.get(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.item_autocomplete_url, parent, false);
        TextView urlText = convertView.findViewById(R.id.url);
        urlText.setText(url);
        return convertView;
    }
}
