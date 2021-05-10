package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.adapter.MultiSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectNetworkFilterViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkFilterViewModelFactory;
import com.alphawallet.app.widget.TestNetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SelectNetworkFilterActivity extends SelectNetworkBaseActivity implements TestNetDialog.TestNetDialogCallback {
    @Inject
    SelectNetworkFilterViewModelFactory viewModelFactory;
    private SelectNetworkFilterViewModel viewModel;

    private MultiSelectNetworkAdapter mainNetAdapter;
    private MultiSelectNetworkAdapter testNetAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.select_active_networks));

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(SelectNetworkFilterViewModel.class);

        List<Integer> selectedNetworks = Utils.intListToArray(viewModel.getFilterNetworkList());

        setupList(selectedNetworks, Arrays.asList(viewModel.getNetworkList()));

        initTestNetDialog(this);
    }

    void setupList(List<Integer> selectedNetworks, List<NetworkInfo> availableNetworks)
    {
        boolean isMainNetActive = EthereumNetworkBase.hasRealValue(selectedNetworks.get(0));

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
            }
            else
            {
                testnetDialog.show();
            }
        };

        mainnetSwitch.setOnCheckedChangeListener(mainnetListener);
        testnetSwitch.setOnCheckedChangeListener(testnetListener);

        toggleListVisibility(isMainNetActive);

        setupFilterList(selectedNetworks, availableNetworks);
    }

    private void setupFilterList(List<Integer> selectedNetworks, List<NetworkInfo> availableNetworks)
    {
        ArrayList<NetworkItem> mainNetList = new ArrayList<>();
        ArrayList<NetworkItem> testNetList = new ArrayList<>();

        for (NetworkInfo info : availableNetworks)
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId))
            {
                mainNetList.add(new NetworkItem(info.name, info.chainId, selectedNetworks.contains(info.chainId)));
            }
            else
            {
                testNetList.add(new NetworkItem(info.name, info.chainId, selectedNetworks.contains(info.chainId)));
            }
        }

        mainNetAdapter = new MultiSelectNetworkAdapter(mainNetList);
        mainnetRecyclerView.setAdapter(mainNetAdapter);

        testNetAdapter = new MultiSelectNetworkAdapter(testNetList);
        testnetRecyclerView.setAdapter(testNetAdapter);
    }

    @Override
    protected void handleSetNetworks()
    {
        Integer[] filterList = mainnetSwitch.isChecked() ? mainNetAdapter.getSelectedItems() : testNetAdapter.getSelectedItems();

        if (filterList.length <= 0)
        {
            Toast.makeText(this, "Please select at least one network.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            viewModel.setFilterNetworks(filterList);
            sendBroadcast(new Intent(C.RESET_WALLET));
            setResult(RESULT_OK);
            finish();
        }
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
