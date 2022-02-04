package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.ui.widget.divider.ListDivider;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectCurrencyActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private String currentCurrency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basic_list_activity);
        toolbar();
        setTitle(getString(R.string.dialog_title_select_currency));
        currentCurrency = getIntent().getStringExtra(C.EXTRA_CURRENCY);

        ArrayList<CurrencyItem> currencyItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);
        if (currencyItems != null) {
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomAdapter(currencyItems, currentCurrency);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new ListDivider(this));
        }
    }

    @Override
    public void onBackPressed() {
        if (!currentCurrency.equals(adapter.getSelectedItemId())) {
            setCurrency();
        } else {
            super.onBackPressed();
        }
    }

    private void setCurrency() {
        Intent intent = new Intent();
        String item = adapter.getSelectedItemId();
        intent.putExtra(C.EXTRA_CURRENCY, item);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
        private final ArrayList<CurrencyItem> dataSet;
        private String selectedItemId;

        private void setSelectedItemId(String selectedItemId) {
            this.selectedItemId = selectedItemId;
        }

        private String getSelectedItemId() {
            return this.selectedItemId;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_currency, parent, false);

            return new CustomViewHolder(itemView);
        }

        class CustomViewHolder extends RecyclerView.ViewHolder {
            ImageView checkbox;
            ImageView flag;
            TextView code;
            TextView name;
            View itemLayout;

            CustomViewHolder(View view) {
                super(view);
                checkbox = view.findViewById(R.id.checkbox);
                flag = view.findViewById(R.id.flag);
                code = view.findViewById(R.id.code);
                name = view.findViewById(R.id.name);
                itemLayout = view.findViewById(R.id.layout_list_item);
            }
        }

        private CustomAdapter(ArrayList<CurrencyItem> data, String selectedItemId) {
            this.dataSet = data;
            this.selectedItemId = selectedItemId;

            for (CurrencyItem l : data) {
                if (l.getCode().equals(selectedItemId)) {
                    l.setSelected(true);
                }
            }
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
            CurrencyItem currencyItem = dataSet.get(position);
            holder.name.setText(currencyItem.getName());
            holder.code.setText(currencyItem.getCode());
            holder.flag.setImageResource(currencyItem.getFlag());
            holder.itemLayout.setOnClickListener(v -> {
                for (int i = 0; i < dataSet.size(); i++) {
                    dataSet.get(i).setSelected(false);
                }
                dataSet.get(position).setSelected(true);
                setSelectedItemId(dataSet.get(position).getCode());
                notifyDataSetChanged();
            });

            holder.checkbox.setSelected(currencyItem.isSelected());
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }
}
