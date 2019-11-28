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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.LocaleItem;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectLocaleActivity extends BaseActivity implements StandardFunctionInterface
{
    private ListView listView;
    private CustomAdapter adapter;
    private FunctionButtonBar functionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentLocale = getIntent().getStringExtra(C.EXTRA_LOCALE);
        ArrayList<LocaleItem> localeItems = getIntent().getParcelableArrayListExtra(C.EXTRA_STATE);

        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        functionBar = findViewById(R.id.layoutButtons);
        toolbar();
        setTitle(getString(R.string.dialog_title_select_locale));

        adapter = new CustomAdapter(this, localeItems, currentLocale);
        listView.setAdapter(adapter);

        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_confirm));
        functionBar.setupFunctions(this, functions);
    }

    @Override
    public void handleClick(int view)
    {
        Intent intent = new Intent();
        String item = adapter.getSelectedItemId();
        intent.putExtra(C.EXTRA_LOCALE, item);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                handleClick(0);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends ArrayAdapter<LocaleItem>
    {
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
            LinearLayout itemLayout;
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
        public View getView(int position, View convertView, ViewGroup parent)
        {
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
                    viewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_active);
                } else {
                    viewHolder.checkbox.setImageResource(R.drawable.ic_checkbox);
                }
            }

            return view;
        }
    }
}
