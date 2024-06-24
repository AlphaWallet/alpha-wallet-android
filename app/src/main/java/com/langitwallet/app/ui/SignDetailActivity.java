package com.langitwallet.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.langitwallet.app.R;
import com.langitwallet.app.C;
import com.langitwallet.app.repository.SignRecord;
import com.langitwallet.app.util.Utils;
import com.langitwallet.app.widget.StandardHeader;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by JB on 9/09/2020.
 */
@AndroidEntryPoint
public class SignDetailActivity extends BaseActivity
{
    private RecyclerView recyclerView;
    private ArrayList<SignRecord> signRecords;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basic_list_activity);
        toolbar();
        setTitle(getString(R.string.signed_transactions));
        signRecords = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        if (signRecords != null)
        {
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomAdapter();
            recyclerView.setAdapter(adapter);
        }
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sign_instance, parent, false);

            return new CustomViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position)
        {
            SignRecord record = signRecords.get(position);
            holder.date.setText(record.type +
                    " (" +
                    Utils.localiseUnixTime(getApplicationContext(), record.date / 1000) +
                    ")");
            holder.detail.setText(record.message);
        }

        @Override
        public int getItemCount()
        {
            return signRecords.size();
        }

        class CustomViewHolder extends RecyclerView.ViewHolder
        {
            StandardHeader date;
            TextView detail;

            CustomViewHolder(View view)
            {
                super(view);
                date = view.findViewById(R.id.date);
                detail = view.findViewById(R.id.details);
            }
        }
    }
}
