package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.util.LocaleUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by JB on 9/09/2020.
 */
public class SignDetailActivity extends BaseActivity
{
    private RecyclerView recyclerView;
    private ArrayList<SignRecord> signRecords;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        toolbar();
        setTitle(getString(R.string.signed_transactions));
        signRecords = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        if (signRecords != null) {
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomAdapter();
            recyclerView.setAdapter(adapter);
        }
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sign_instance, parent, false);

            return new CustomAdapter.CustomViewHolder(itemView);
        }

        class CustomViewHolder extends RecyclerView.ViewHolder
        {
            TextView date;
            TextView type;
            TextView detail;

            CustomViewHolder(View view)
            {
                super(view);
                date = view.findViewById(R.id.date);
                type = view.findViewById(R.id.sign_type);
                detail = view.findViewById(R.id.details);
            }
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position)
        {
            SignRecord record = signRecords.get(position);

            holder.date.setText(localiseUnixTime(record.date));
            holder.type.setText(record.type);
            holder.detail.setText(record.message);
        }

        @Override
        public int getItemCount()
        {
            return signRecords.size();
        }

        private String localiseUnixTime(long timeStamp)
        {
            Date date = new java.util.Date(timeStamp);
            DateFormat timeFormat = java.text.DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(getApplication()));
            DateFormat dateFormat = java.text.DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtils.getDeviceLocale(getApplication()));
            return timeFormat.format(date) + " | " + dateFormat.format(date);
        }
    }
}
