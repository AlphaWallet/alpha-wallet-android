package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.adapter.SingleSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectBrowserNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectBrowserNetworkViewModelFactory;
import com.alphawallet.app.widget.TestNetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SelectBrowserNetworkActivity extends SelectNetworkBaseActivity implements TestNetDialog.TestNetDialogCallback {
    private static final int REQUEST_SELECT_ACTIVE_NETWORKS = 2000;

    @Inject
    SelectBrowserNetworkViewModelFactory viewModelFactory;
    boolean localSelectionMode;
    private SelectBrowserNetworkViewModel viewModel;
    private SingleSelectNetworkAdapter mainNetAdapter;
    private SingleSelectNetworkAdapter testNetAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.select_dappbrowser_network));

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(SelectBrowserNetworkViewModel.class);

        prepare();
    }

    void prepare()
    {
        Intent intent = getIntent();
        if (intent != null)
        {
            localSelectionMode = intent.getBooleanExtra(C.EXTRA_LOCAL_NETWORK_SELECT_FLAG, false);
            int selectedChainId = intent.getIntExtra(C.EXTRA_CHAIN_ID, -1);

            if (selectedChainId == -1)
            {
                selectedChainId = Utils.intListToArray(viewModel.getFilterNetworkList()).get(0);
            }

            List<NetworkInfo> availableNetworks;

            if (localSelectionMode || CustomViewSettings.allowAllNetworks())
            {
                setTitle(getString(R.string.choose_network_preference));
                availableNetworks = Arrays.asList(viewModel.getNetworkList());
            } else
            {
                hideSwitches();
                availableNetworks = new ArrayList<>();
                for (Integer chainId : Utils.intListToArray(viewModel.getFilterNetworkList()))
                {
                    availableNetworks.add(viewModel.getNetworkByChain(chainId));
                }
            }

            setupList(selectedChainId, availableNetworks);

            initTestNetDialog(this);
        }
    }

    void setupList(Integer selectedNetwork, List<NetworkInfo> availableNetworks)
    {
        mainnetSwitch.setOnCheckedChangeListener(null);
        testnetSwitch.setOnCheckedChangeListener(null);

        boolean isMainNetActive = EthereumNetworkBase.hasRealValue(selectedNetwork);

        mainnetSwitch.setChecked(isMainNetActive);
        testnetSwitch.setChecked(!isMainNetActive);

        CompoundButton.OnCheckedChangeListener mainnetListener = (compoundButton, checked) -> {
            testnetSwitch.setChecked(!checked);
        };

        CompoundButton.OnCheckedChangeListener testnetListener = (compoundButton, checked) ->
        {
            mainnetSwitch.setOnCheckedChangeListener(null);
            mainnetSwitch.setChecked(!checked);
            mainnetSwitch.setOnCheckedChangeListener(mainnetListener);

            toggleListVisibility(!checked);

            if (!checked)
            {
                mainNetAdapter.selectDefault();
            } else
            {
                testnetDialog.show();
            }
        };

        mainnetSwitch.setOnCheckedChangeListener(mainnetListener);
        testnetSwitch.setOnCheckedChangeListener(testnetListener);

        toggleListVisibility(isMainNetActive);

        setupFilters(selectedNetwork, availableNetworks);
    }

    private void setupFilters(Integer selectedNetwork, List<NetworkInfo> availableNetworks)
    {
        ArrayList<NetworkItem> mainNetList = new ArrayList<>();
        ArrayList<NetworkItem> testNetList = new ArrayList<>();

        for (NetworkInfo info : availableNetworks)
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId))
            {
                mainNetList.add(new NetworkItem(info.name, info.chainId, selectedNetwork.equals(info.chainId)));
            } else
            {
                testNetList.add(new NetworkItem(info.name, info.chainId, selectedNetwork.equals(info.chainId)));
            }
        }

        mainNetAdapter = new SingleSelectNetworkAdapter(mainNetList);
        mainnetRecyclerView.setAdapter(mainNetAdapter);

        testNetAdapter = new SingleSelectNetworkAdapter(testNetList);
        testnetRecyclerView.setAdapter(testNetAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (!localSelectionMode)
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
        } else
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
                prepare();
            }
        } else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void handleSetNetworks()
    {
        int selectedNetwork = mainnetSwitch.isChecked() ? mainNetAdapter.getSelectedItem() : testNetAdapter.getSelectedItem();
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_CHAIN_ID, selectedNetwork);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onTestNetDialogClosed()
    {
        testnetSwitch.setChecked(false);
        testnetDialog.dismiss();
    }

    @Override
    public void onTestNetDialogConfirmed()
    {
        testnetDialog.dismiss();
        testNetAdapter.selectDefault();
    }

    @Override
    public void onTestNetDialogCancelled()
    {
        testnetSwitch.setChecked(false);
    }
}
