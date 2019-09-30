package com.alphawallet.app.widget;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.LocaleItem;

public class SelectLocaleDialog extends Dialog {
    private static SelectLocaleDialog dialog = null;
    private TextView titleText;
    private Button button;
    private Context context;
    private ListView listView;
    private CustomAdapter adapter;

    public SelectLocaleDialog(@NonNull Activity activity, ArrayList<LocaleItem> list, String selectedItem) {
        super(activity);
        this.context = activity;

        setContentView(R.layout.dialog_awallet_list);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        listView = findViewById(R.id.dialog_list);
        button = findViewById(R.id.dialog_button);
        titleText = findViewById(R.id.dialog_main_text);

        adapter = new CustomAdapter(list, selectedItem);
        listView.setAdapter(adapter);

        setTitle(R.string.dialog_title_select_locale);
    }

    @Override
    public void show() {
        super.show();
    }

    public String getSelectedItem() {
        return adapter.getSelectedItem();
    }

    public String getSelectedItemId() {
        return adapter.getSelectedItemId();
    }

    public void setOnClickListener(View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    public void setTitle(int resId) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(context.getResources().getString(resId));
    }

    @Override
    public void setTitle(CharSequence message) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(message);
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
            LinearLayout itemLayout;
        }

        private CustomAdapter(ArrayList<LocaleItem> data, String selectedItemId) {
            super(context, R.layout.item_dialog_list, data);
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
                    viewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_active);
                } else {
                    viewHolder.checkbox.setImageResource(R.drawable.ic_checkbox);
                }
            }

            return view;
        }
    }
}
