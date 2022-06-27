package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectCurrencyActivity extends BaseActivity
{
    private RecyclerView recyclerView;
    private SelectCurrencyAdapter adapter;
    private String currentCurrency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basic_list_activity);
        toolbar();
        setTitle(getString(R.string.dialog_title_select_currency));
        currentCurrency = getIntent().getStringExtra(C.EXTRA_CURRENCY);

        ArrayList<CurrencyItem> currencyItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);
        if (currencyItems != null)
        {
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SelectCurrencyAdapter(currencyItems, currentCurrency);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (!currentCurrency.equals(adapter.getSelectedItemId()))
        {
            setCurrency();
        }
        else
        {
            super.onBackPressed();
        }
    }

    private void setCurrency()
    {
        Intent intent = new Intent();
        String item = adapter.getSelectedItemId();
        intent.putExtra(C.EXTRA_CURRENCY, item);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public class SelectCurrencyAdapter extends RecyclerView.Adapter<SelectCurrencyAdapter.SelectCurrencyViewHolder>
    {
        private final ArrayList<CurrencyItem> dataSet;
        private String selectedItemId;

        private SelectCurrencyAdapter(ArrayList<CurrencyItem> data, String selectedItemId)
        {
            this.dataSet = data;
            this.selectedItemId = selectedItemId;

            for (CurrencyItem l : data)
            {
                if (l.getCode().equals(selectedItemId))
                {
                    l.setSelected(true);
                }
            }
        }

        private String getSelectedItemId()
        {
            return this.selectedItemId;
        }

        private void setSelectedItemId(String selectedItemId)
        {
            this.selectedItemId = selectedItemId;
        }

        @Override
        public SelectCurrencyViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_currency, parent, false);

            return new SelectCurrencyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SelectCurrencyViewHolder holder, int position)
        {
            CurrencyItem currencyItem = dataSet.get(position);
            holder.name.setText(currencyItem.getName());
            holder.code.setText(currencyItem.getCode());
            holder.flag.setImageResource(currencyItem.getFlag());
            holder.itemLayout.setOnClickListener(v -> {
                for (int i = 0; i < dataSet.size(); i++)
                {
                    dataSet.get(i).setSelected(false);
                }
                dataSet.get(position).setSelected(true);
                setSelectedItemId(dataSet.get(position).getCode());
                notifyDataSetChanged();
            });

            holder.radioButton.setChecked(currencyItem.isSelected());
        }

        @Override
        public int getItemCount()
        {
            return dataSet.size();
        }

        class SelectCurrencyViewHolder extends RecyclerView.ViewHolder
        {
            MaterialRadioButton radioButton;
            ImageView flag;
            TextView code;
            TextView name;
            View itemLayout;

            SelectCurrencyViewHolder(View view)
            {
                super(view);
                radioButton = view.findViewById(R.id.radio_button);
                flag = view.findViewById(R.id.flag);
                code = view.findViewById(R.id.code);
                name = view.findViewById(R.id.name);
                itemLayout = view.findViewById(R.id.layout_list_item);
            }
        }
    }
}
