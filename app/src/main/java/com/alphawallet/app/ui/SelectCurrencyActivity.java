package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectCurrencyActivity extends BaseActivity implements StandardFunctionInterface
{
    private RecyclerView listView;
    private CustomAdapter adapter;
    private FunctionButtonBar functionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentCurrency = getIntent().getStringExtra(C.EXTRA_CURRENCY);
        ArrayList<CurrencyItem> currencyItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        setContentView(R.layout.dialog_awallet_currency_list);
        listView = findViewById(R.id.dialog_list);
        toolbar();
        setTitle(getString(R.string.dialog_title_select_currency));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(linearLayoutManager);

        adapter = new CustomAdapter(currencyItems, currentCurrency);
        listView.setAdapter(adapter);
        listView.addItemDecoration(new ListDivider(this));
        functionBar = findViewById(R.id.layoutButtons); //use standard bottom function button bar to make it easy to customise or update UI
        functionBar.setVisibility(View.VISIBLE);
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.button_ok));
        functionBar.setupFunctions(this, functions);
    }

    @Override
    public void onBackPressed() {
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

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        private ArrayList<CurrencyItem> dataSet;
        private String selectedItem;
        private String selectedItemId;

        private void setSelectedItem(String selectedItem) {
            this.selectedItem = selectedItem;
        }

        private void setSelectedItemId(String selectedItemId) {
            this.selectedItemId = selectedItemId;
        }

        private String getSelectedItemId() {
            return this.selectedItemId;
        }

        private String getSelectedItem() {
            return this.selectedItem;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dialog_currency_list, parent, false);

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
                setSelectedItem(dataSet.get(position).getName());
                setSelectedItemId(dataSet.get(position).getCode());
                notifyDataSetChanged();
            });

            if (currencyItem.isSelected()) {
                holder.checkbox.setImageResource(R.drawable.ic_radio_on);
            } else {
                holder.checkbox.setImageResource(R.drawable.ic_radio_off);
            }
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }

    @Override
    public void handleClick(int view)
    {
        if (view == R.string.button_ok) //handle OK button
        {
            onBackPressed();
        }
    }
}
