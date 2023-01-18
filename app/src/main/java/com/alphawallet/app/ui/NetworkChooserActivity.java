package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.ui.widget.adapter.SingleSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.viewmodel.NetworkChooserViewModel;
import com.alphawallet.app.widget.TestNetDialog;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NetworkChooserActivity extends NetworkBaseActivity implements TestNetDialog.TestNetDialogCallback
{
    private static final int REQUEST_SELECT_ACTIVE_NETWORKS = 2000;

    boolean localSelectionMode;
    private NetworkChooserViewModel viewModel;
    private SingleSelectNetworkAdapter networkAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(NetworkChooserViewModel.class);

        prepare(getIntent());
    }

    void prepare(Intent intent)
    {
        if (intent == null)
        {
            finish();
            return;
        }

        long selectedChainId = intent.getLongExtra(C.EXTRA_CHAIN_ID, -1);

        // Previous active network was deselected, get the first item in filtered networks
        if (selectedChainId == -1)
        {
            selectedChainId = viewModel.getSelectedNetwork();
        } //try network from settings
        if (selectedChainId == -1
                || !viewModel.getFilterNetworkList().contains(selectedChainId))
        {
            selectedChainId = viewModel.getFilterNetworkList().get(0);
        } //use first network known on list if there's still any kind of issue

        setTitle(getString(R.string.select_dappbrowser_network));

        hideSwitch();
        List<NetworkInfo> filteredNetworks = new ArrayList<>();
        for (Long chainId : viewModel.getFilterNetworkList())
        {
            filteredNetworks.add(viewModel.getNetworkByChain(chainId));
        }

        setupList(selectedChainId, filteredNetworks);
    }

    void setupList(Long selectedNetwork, List<NetworkInfo> availableNetworks)
    {
        initViews();
        setupFilters(selectedNetwork, availableNetworks);
    }

    private void setupFilters(Long selectedNetwork, List<NetworkInfo> availableNetworks)
    {
        ArrayList<NetworkItem> mainNetList = new ArrayList<>();

        for (NetworkInfo info : availableNetworks)
        {
            mainNetList.add(new NetworkItem(info.name, info.chainId, selectedNetwork.equals(info.chainId)));
        }

        networkAdapter = new SingleSelectNetworkAdapter(mainNetList);
        mainnetRecyclerView.setAdapter(networkAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (!localSelectionMode && !CustomViewSettings.showAllNetworks())
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_filter_network, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_filter)
        {
            viewModel.openSelectNetworkFilters(this, REQUEST_SELECT_ACTIVE_NETWORKS);
        }
        else
        {
            super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == REQUEST_SELECT_ACTIVE_NETWORKS)
        {
            if (resultCode == RESULT_OK)
            {
                prepare(data);
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void handleSetNetworks()
    {
        long selectedNetwork = networkAdapter.getSelectedItem();
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_CHAIN_ID, selectedNetwork);
        setResult(RESULT_OK, intent);
        finish();
    }
}

