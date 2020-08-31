package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;

public class SelectNetworkActivity extends BaseActivity {
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private boolean singleItem;
    private String selectedChainId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        toolbar();
        setTitle(getString(R.string.select_active_networks));

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SelectNetworkViewModel.class);

        if (getIntent() != null) {
            singleItem = getIntent().getBooleanExtra(C.EXTRA_SINGLE_ITEM, false);
            selectedChainId = getIntent().getStringExtra(C.EXTRA_CHAIN_ID);
        }

        if (selectedChainId == null || selectedChainId.isEmpty()) {
            selectedChainId = viewModel.getFilterNetworkList();
        }

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (singleItem) {
            setTitle(getString(R.string.select_single_network));
            getMenuInflater().inflate(R.menu.menu_filter_network, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setupFilterList() {
        ArrayList<NetworkItem> list = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);
        List<Integer> activeNetworks = viewModel.getActiveNetworks();

        //Ensure that there's always a network selected in single network mode
        if (singleItem && (intList.size() < 1 || !activeNetworks.contains(intList.get(0)))) {
            intList.clear();
            intList.add(MAINNET_ID);
        }

        //if active networks is empty ensure mainnet is displayed
        if (activeNetworks.size() == 0) {
            activeNetworks.add(MAINNET_ID);
            intList.add(MAINNET_ID);
        }

        for (NetworkInfo info : viewModel.getNetworkList()) {
            if (!singleItem || activeNetworks.contains(info.chainId)) {
                list.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
            }
        }

        adapter = new CustomAdapter(list, singleItem);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                handleSetNetworks();
                break;
            }
            case R.id.action_filter: {
                viewModel.openFilterSelect(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleSetNetworks();
    }

    private void handleSetNetworks() {
        Integer[] filterList = adapter.getSelectedItems();
        if (filterList.length == 0)
            filterList = EthereumNetworkRepository.addDefaultNetworks().toArray(new Integer[0]);
        if (singleItem) {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_CHAIN_ID, filterList[0]);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            viewModel.setFilterNetworks(filterList);
            sendBroadcast(new Intent(C.RESET_WALLET));
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFilterList();
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
        private ArrayList<NetworkItem> dataSet;
        private int chainId;
        private final boolean singleItem;

        public Integer[] getSelectedItems() {
            List<Integer> enabledIds = new ArrayList<>();
            for (NetworkItem data : dataSet) {
                if (data.isSelected()) enabledIds.add(data.getChainId());
            }

            return enabledIds.toArray(new Integer[0]);
        }

        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int buttonTypeId = singleItem ? R.layout.item_simple_radio : R.layout.item_simple_check;
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(buttonTypeId, parent, false);

            return new CustomAdapter.CustomViewHolder(itemView);
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

        private CustomAdapter(ArrayList<NetworkItem> data, boolean singleItem) {
            this.dataSet = data;
            this.singleItem = singleItem;

            if (!singleItem) {
                for (NetworkItem item : data) {
                    if (CustomViewSettings.isPrimaryNetwork(item)) {
                        item.setSelected(true);
                        break;
                    }
                }
            }
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position) {
            NetworkItem item = dataSet.get(position);

            //
            if (item != null) {
                holder.name.setText(item.getName());
                holder.itemLayout.setOnClickListener(v -> clickListener(holder, position));
                holder.checkbox.setSelected(item.isSelected());

                if (!singleItem && dataSet.get(position).getName().equals(CustomViewSettings.primaryNetworkName())) {
                    holder.checkbox.setAlpha(0.5f);
                } else {
                    holder.checkbox.setAlpha(1.0f);
                }
            }
        }

        private void clickListener(final CustomAdapter.CustomViewHolder holder, final int position)
        {
            if (singleItem) {
                for (NetworkItem networkItem : dataSet) {
                    networkItem.setSelected(false);
                }
                dataSet.get(position).setSelected(true);
                notifyDataSetChanged();
            } else if (!dataSet.get(position).getName().equals(CustomViewSettings.primaryNetworkName())) {
                dataSet.get(position).setSelected(!dataSet.get(position).isSelected());
            }
            holder.checkbox.setSelected(dataSet.get(position).isSelected());
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }
}
