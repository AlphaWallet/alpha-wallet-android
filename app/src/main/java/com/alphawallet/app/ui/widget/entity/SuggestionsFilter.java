package com.alphawallet.app.ui.widget.entity;

import android.widget.Filter;

import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;

import java.util.ArrayList;
import java.util.List;

import com.alphawallet.app.entity.DApp;

public class SuggestionsFilter extends Filter {
    private final DappBrowserSuggestionsAdapter adapter;
    private final List<DApp> originalList;
    private final List<DApp> filteredList;

    public SuggestionsFilter(DappBrowserSuggestionsAdapter adapter, List<DApp> originalList) {
        super();
        this.adapter = adapter;
        this.originalList = originalList;
        this.filteredList = new ArrayList<>();
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        filteredList.clear();
        final FilterResults results = new FilterResults();

        if (constraint == null || constraint.length() == 0) {
            filteredList.addAll(originalList);
        } else {
            final String filterPattern = constraint.toString().toLowerCase().trim();

            for (final DApp dapp : originalList) {
                if (dapp.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(dapp);
                }
            }
        }
        results.values = filteredList;
        results.count = filteredList.size();
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.filteredSuggestions.clear();
        if (results != null && results.values != null) adapter.filteredSuggestions.addAll((List) results.values);
        adapter.notifyDataSetChanged();
    }
}
