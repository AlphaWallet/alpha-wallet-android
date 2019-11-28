package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.adapter.NetworkListAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;

public class SelectNetworkActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    //private Button confirmButton;
    private ListView listView;
    private NetworkListAdapter adapter;
    private boolean singleItem;
    private String selectedChainId;
    private LinearLayout filterButton;
    private FunctionButtonBar functionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        filterButton = findViewById(R.id.filter_button);
        functionBar = findViewById(R.id.layoutButtons);
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

        if (singleItem)
        {
            setTitle(getString(R.string.select_single_network));
            filterButton.setVisibility(View.VISIBLE);
            filterButton.setOnClickListener(v -> { viewModel.openFilterSelect(this); });
        }

        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_confirm));
        functionBar.setupFunctions(this, functions);
    }

    @Override
    public void handleClick(int view)
    {
        handleSetNetworks();
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
        listView.setDividerHeight(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                handleSetNetworks();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
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
