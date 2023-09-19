package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;

import java.util.List;

public class EventAdapter extends ArrayAdapter<String>
{
    public EventAdapter(@NonNull Context context, List<String> list)
    {
        super(context, 0, list);
    }

    @Override
    public boolean isEnabled(int position)
    {
        return false;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event, parent, false);
        }

        TextView view = convertView.findViewById(R.id.text);
        view.setText(getItem(position));

        return convertView;
    }
}
