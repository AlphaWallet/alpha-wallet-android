package io.stormbird.wallet.widget;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Network;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.ui.widget.entity.NetworkItem;
import io.stormbird.wallet.util.Utils;

public class SelectNetworkDialog extends Dialog {
    public static final int NONE = 0;
    public static final int ERROR = R.drawable.ic_error;
    private TextView titleText;
    private Button button;
    private Context context;
    private ListView listView;
    private CustomAdapter adapter;

    public SelectNetworkDialog(@NonNull Activity activity, NetworkInfo[] networkList, String selectedChainId, boolean singleItem) {
        super(activity);
        this.context = activity;

        setContentView(R.layout.dialog_awallet_list);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        listView = findViewById(R.id.dialog_list);
        button = findViewById(R.id.dialog_button);
        titleText = findViewById(R.id.dialog_main_text);

        ArrayList<NetworkItem> list = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);

        for (NetworkInfo info : networkList)
        {
            list.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
        }

        adapter = new CustomAdapter(list, selectedChainId, singleItem);
        listView.setAdapter(adapter);
    }

    @Override
    public void show() {
        super.show();
    }

    public int getSelectedChainId() {
        return adapter.getSelectedChainId();
    }

    public Integer[] getSelectedItems() {
        return adapter.getSelectedItems();
    }

    public void setOnClickListener(View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    public void setTitle(int resId) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(context.getResources().getString(resId));
    }

    public void setButtonText(int resId) {
        button.setText(context.getResources().getString(resId));
    }

    @Override
    public void setTitle(CharSequence message) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(message);
    }

    public class CustomAdapter extends ArrayAdapter<NetworkItem> {
        private ArrayList<NetworkItem> dataSet;
        private String selectedItem;
        private int chainId;
        private boolean singleItem;

        private void setSelectedItem(String selectedItem, int chainId) {
            this.selectedItem = selectedItem;
            this.chainId = chainId;
        }

        private int getSelectedChainId() {
            return this.chainId;
        }

        Integer[] getSelectedItems()
        {
            List<Integer> enabledIds = new ArrayList<>();
            for (NetworkItem data : dataSet)
            {
                if (data.isSelected()) enabledIds.add(data.getChainId());
            }

            return enabledIds.toArray(new Integer[0]);
        }

        private class ViewHolder {
            ImageView checkbox;
            TextView name;
            LinearLayout itemLayout;
        }

        private CustomAdapter(ArrayList<NetworkItem> data, String selectedItem, boolean singleItem) {
            super(context, R.layout.item_dialog_list, data);
            this.dataSet = data;
            this.selectedItem = selectedItem;
            this.singleItem = singleItem;

            if (!singleItem)
            {
                for (NetworkItem item : data)
                {
                    if (item.getName().equals(C.ETHEREUM_NETWORK_NAME))
                    {
                        item.setSelected(true);
                        break;
                    }
                }
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NetworkItem item = getItem(position);
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
                    if (singleItem)
                    {
                        for (NetworkItem networkItem : dataSet)
                        {
                            networkItem.setSelected(false);
                        }
                        dataSet.get(position).setSelected(true);
                    }
                    else if (!dataSet.get(position).getName().equals(C.ETHEREUM_NETWORK_NAME))
                    {
                        if (dataSet.get(position).isSelected())
                        {
                            dataSet.get(position).setSelected(false);
                        }
                        else
                        {
                            dataSet.get(position).setSelected(true);
                        }
                    }
                    setSelectedItem(dataSet.get(position).getName(), dataSet.get(position).getChainId());
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
