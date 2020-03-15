package com.alphawallet.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.LocaleItem;

import java.util.ArrayList;

public class SelectLocaleActivity extends BaseActivity {
    private ListView listView;
    private CustomAdapter adapter;
    private String currentLocale;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLocale = getIntent().getStringExtra(C.EXTRA_LOCALE);
        ArrayList<LocaleItem> localeItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        toolbar();
        setTitle(getString(R.string.settings_locale_lang));

        adapter = new CustomAdapter(this, localeItems, currentLocale);
        listView.setAdapter(adapter);
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
        if (id != null && id != currentLocale) {
            setLocale(id);
        } else {
            super.onBackPressed();
        }
    }

    public class CustomAdapter extends ArrayAdapter<LocaleItem> {
        private ArrayList<LocaleItem> dataSet;
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

        private class ViewHolder {
            ImageView checkbox;
            TextView name;
            RelativeLayout itemLayout;
        }

        private CustomAdapter(Context ctx, ArrayList<LocaleItem> data, String selectedItemId) {
            super(ctx, R.layout.item_dialog_list, data);
            this.dataSet = data;
            this.selectedItemId = selectedItemId;

            for (LocaleItem l : data) {
                if (l.getCode().equals(selectedItemId)) {
                    l.setSelected(true);
                }
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LocaleItem item = getItem(position);
            final ViewHolder viewHolder;
            View view = convertView;

            if (view == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.item_dialog_list, null);
                view.setTag(viewHolder);
                viewHolder.name = view.findViewById(R.id.name);
                viewHolder.checkbox = view.findViewById(R.id.checkbox);
                viewHolder.itemLayout = view.findViewById(R.id.layout_list_item);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            if (item != null) {
                viewHolder.name.setText(item.getName());
                viewHolder.itemLayout.setOnClickListener(v -> {
                    for (int i = 0; i < dataSet.size(); i++) {
                        dataSet.get(i).setSelected(false);
                    }
                    dataSet.get(position).setSelected(true);
                    setSelectedItem(dataSet.get(position).getName());
                    setSelectedItemId(dataSet.get(position).getCode());
                    notifyDataSetChanged();
                });

                if (item.isSelected()) {
                    viewHolder.checkbox.setImageResource(R.drawable.ic_radio_on);
                } else {
                    viewHolder.checkbox.setImageResource(R.drawable.ic_radio_off);
                }
            }

            return view;
        }
    }
}
