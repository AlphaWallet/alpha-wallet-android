package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.alphawallet.app.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MethodAdapter extends ArrayAdapter<String>
{
    public MethodAdapter(@NonNull Context context, List<String> methods)
    {
        super(context, 0, methods);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_method, parent, false);
        }

        TextView methodName = convertView.findViewById(R.id.tv_method_name);

        methodName.setText(getItem(position));
        return convertView;
    }
}
