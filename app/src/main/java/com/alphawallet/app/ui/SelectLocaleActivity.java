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
import com.alphawallet.app.entity.LocaleItem;
import com.alphawallet.app.ui.widget.divider.ListDivider;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectLocaleActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private String currentLocale;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basic_list_activity);
        toolbar();
        setTitle(getString(R.string.settings_locale_lang));

        currentLocale = getIntent().getStringExtra(C.EXTRA_LOCALE);

        ArrayList<LocaleItem> localeItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        if (localeItems != null) {
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomAdapter(localeItems, currentLocale);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new ListDivider(this));
        }
    }

    private void setLocale(String id) {
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_LOCALE, id);
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

    @Override
    public void onBackPressed() {
        String id = adapter.getSelectedItemId();
        if (id != null && !id.equals(currentLocale)) {
            setLocale(id);
        } else {
            super.onBackPressed();
        }
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
        private final ArrayList<LocaleItem> dataSet;
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
                    .inflate(R.layout.item_simple_radio, parent, false);

            return new CustomViewHolder(itemView);
        }

        class CustomViewHolder extends RecyclerView.ViewHolder {
            ImageView checkbox;
            TextView name;
            View itemLayout;

            CustomViewHolder(View view) {
                super(view);
                checkbox = view.findViewById(R.id.checkbox);
                name = view.findViewById(R.id.name);
                itemLayout = view.findViewById(R.id.layout_list_item);
            }
        }

        private CustomAdapter(ArrayList<LocaleItem> data, String selectedItemId) {
            this.dataSet = data;
            this.selectedItemId = selectedItemId;

            for (LocaleItem l : data) {
                if (l.getCode().equals(selectedItemId)) {
                    l.setSelected(true);
                }
            }
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
            LocaleItem item = dataSet.get(position);
            holder.name.setText(item.getName());
            holder.itemLayout.setOnClickListener(v -> {
                for (int i = 0; i < dataSet.size(); i++) {
                    dataSet.get(i).setSelected(false);
                }
                dataSet.get(position).setSelected(true);
                setSelectedItemId(dataSet.get(position).getCode());
                notifyDataSetChanged();
            });

            holder.checkbox.setSelected(item.isSelected());
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }
}
