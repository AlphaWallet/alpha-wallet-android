package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.adapter.MultiSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
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

        setupList();

        initTestNetDialog(this);
    }

    void setupList()
    {
        boolean isMainNetActive = viewModel.mainNetActive();

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

            if (checked)
            {
                testnetDialog.show();
            }
        };

        mainnetSwitch.setOnCheckedChangeListener(mainnetListener);
        testnetSwitch.setOnCheckedChangeListener(testnetListener);

        toggleListVisibility(isMainNetActive);

        setupFilterList();
    }

    private void setupFilterList()
    {
        List<NetworkItem> mainNetList = viewModel.getNetworkList(true);
        List<NetworkItem> testNetList = viewModel.getNetworkList(false);

        mainNetAdapter = new MultiSelectNetworkAdapter(mainNetList);
        mainnetRecyclerView.setAdapter(mainNetAdapter);

        testNetAdapter = new MultiSelectNetworkAdapter(testNetList);
        testnetRecyclerView.setAdapter(testNetAdapter);
    }

    @Override
    protected void handleSetNetworks()
    {
        List<Integer> filterList = new ArrayList<>(Arrays.asList(mainNetAdapter.getSelectedItems()));
        filterList.addAll(Arrays.asList(testNetAdapter.getSelectedItems()));
        boolean hasClicked = mainNetAdapter.hasSelectedItems() || testNetAdapter.hasSelectedItems();

        viewModel.setFilterNetworks(filterList, mainnetSwitch.isChecked(), hasClicked);
        sendBroadcast(new Intent(C.RESET_WALLET));
        setResult(RESULT_OK, new Intent());
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
    }

    @Override
    public void onTestNetDialogCancelled()
    {
        testnetSwitch.setChecked(false);
    }
}
