package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.adapter.NetworkListAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;

public class SelectNetworkActivity extends BaseActivity
{
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    private ListView listView;
    private NetworkListAdapter adapter;
    private boolean singleItem;
    private String selectedChainId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        toolbar();
        setTitle(getString(R.string.select_network_filters));

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SelectNetworkViewModel.class);

        if (getIntent() != null) {
            singleItem = getIntent().getBooleanExtra(C.EXTRA_SINGLE_ITEM, false);
            selectedChainId = getIntent().getStringExtra(C.EXTRA_CHAIN_ID);
        }

        if (selectedChainId == null || selectedChainId.isEmpty()) {
            selectedChainId = viewModel.getFilterNetworkList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (singleItem)
        {
            setTitle(getString(R.string.select_single_network));
            getMenuInflater().inflate(R.menu.menu_filter_network, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setupFilterList()
    {
        ArrayList<NetworkItem> list = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);
        List<Integer> activeNetworks = viewModel.getActiveNetworks();

        //Ensure that there's always a network selected in single network mode
        if (singleItem && (intList.size() < 1 || !activeNetworks.contains(intList.get(0))))
        {
            intList.clear();
            intList.add(MAINNET_ID);
        }

        //if active networks is empty ensure mainnet is displayed
        if (activeNetworks.size() == 0)
        {
            activeNetworks.add(MAINNET_ID);
            intList.add(MAINNET_ID);
        }

        for (NetworkInfo info : viewModel.getNetworkList()) {
            if (!singleItem || activeNetworks.contains(info.chainId))
            {
                list.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
            }
        }
        
        adapter = new NetworkListAdapter(this, list, selectedChainId, singleItem);
        listView.setAdapter(adapter);
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

    private void handleSetNetworks()
    {
        Integer[] filterList = adapter.getSelectedItems();
        if (filterList.length == 0) filterList = EthereumNetworkRepository.addDefaultNetworks().toArray(new Integer[0]);
        if (singleItem)
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_CHAIN_ID, filterList[0]);
            setResult(RESULT_OK, intent);
            finish();
        }
        else
        {
            viewModel.setFilterNetworks(filterList);
            sendBroadcast(new Intent(C.RESET_WALLET));
            finish();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setupFilterList();
    }
}
