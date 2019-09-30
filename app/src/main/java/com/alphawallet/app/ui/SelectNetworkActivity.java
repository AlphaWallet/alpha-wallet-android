package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.widget.TextView;

import com.alphawallet.app.ui.widget.adapter.NetworkListAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;

import dagger.android.AndroidInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.viewmodel.SelectNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkViewModelFactory;

import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;

public class SelectNetworkActivity extends BaseActivity {
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    private Button confirmButton;
    private ListView listView;
    private NetworkListAdapter adapter;
    private boolean singleItem;
    private String selectedChainId;
    private LinearLayout filterButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        confirmButton = findViewById(R.id.dialog_button);
        filterButton = findViewById(R.id.filter_button);
        toolbar();
        setTitle(R.string.empty);

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
            TextView title = findViewById(R.id.dialog_main_text);
            title.setText(R.string.select_single_network);
            filterButton.setVisibility(View.VISIBLE);
            filterButton.setOnClickListener(v -> { viewModel.openFilterSelect(this); });
        }

        confirmButton.setOnClickListener(v -> {
            handleSetNetworks();
        });
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
        if (singleItem)
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_CHAIN_ID, adapter.getSelectedItems()[0]);
            setResult(RESULT_OK, intent);
            finish();
        }
        else
        {
            viewModel.setFilterNetworks(adapter.getSelectedItems());
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
