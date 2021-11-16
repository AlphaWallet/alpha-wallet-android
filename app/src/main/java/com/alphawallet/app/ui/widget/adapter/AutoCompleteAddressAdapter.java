package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.ui.widget.entity.UrlFilter;

public class AutoCompleteAddressAdapter extends ArrayAdapter<String>
{
    private final List<String> history;
    public List<String> filteredUrls = new ArrayList<>();
    private final Context context;
    private ItemClickListener listener;
    private final String preferenceTag;

    public AutoCompleteAddressAdapter(Context context, String tag) {
        super(context, 0);
        preferenceTag = tag;
        this.context = context;
        this.history = getENSHistoryFromPrefs();
        super.addAll(history);
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
    public void remove(@Nullable String object)
    {
        if (history.contains(object)) {
            super.remove(object);
            filteredUrls.remove(object);
        }
    }

    @Override
    public void add(@Nullable String object) {
        if (!history.contains(object)) {
            super.insert(object, 0);
            storeItem(object);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = filteredUrls.get(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.item_autocomplete_url, parent, false);
        TextView urlText = convertView.findViewById(R.id.url);
        urlText.setText(url);
        urlText.setOnLongClickListener(v -> onViewLongClick(url));
        urlText.setOnClickListener(v -> onClickListener(url));
        return convertView;
    }

    private void onClickListener(String url)
    {
        listener.onItemClick(url);
    }

    private boolean onViewLongClick(String url)
    {
        remove(url);
        notifyDataSetChanged();
        listener.onItemLongClick(url);
        return true;
    }

    public void setListener(ItemClickListener listener)
    {
        this.listener = listener;
    }

    private ArrayList<String> getENSHistoryFromPrefs()
    {
        ArrayList<String> history;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(preferenceTag, "");
        if (!historyJson.isEmpty()) {
            history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>(){}.getType());
        } else {
            history = new ArrayList<>();
        }
        return history;
    }

    private void storeItem(String address)
    {
        ArrayList<String> history = getENSHistoryFromPrefs();
        boolean foundValue = false;
        for (String item : history)
        {
            if (item.contains(address))
            {
                foundValue = true;
                break;
            }
        }

        if (!foundValue)
        {
            this.history.add(address);
            storeHistory();
        }
    }

    private void storeHistory()
    {
        String historyJson = new Gson().toJson(history);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(preferenceTag, historyJson).apply();
    }

    public void addDAppURL(String url)
    {
        // don't record the homepage

        String checkVal = url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
        for (String item : history)
        {
            if (item.contains(checkVal))
            {
                //replace with this new one
                history.remove(item);
                if (!history.contains(item))
                {
                    history.add(0, url);
                }
                storeHistory();
                return;
            }
        }

        history.add(0, url);
        storeHistory();
    }

    public boolean hasContext()
    {
        return context != null;
    }
}
