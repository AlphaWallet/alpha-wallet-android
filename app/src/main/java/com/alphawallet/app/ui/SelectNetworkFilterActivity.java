package com.alphawallet.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.ui.widget.adapter.MultiSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.viewmodel.SelectNetworkFilterViewModel;
import com.alphawallet.app.widget.TestNetDialog;
import com.alphawallet.ethereum.NetworkInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;


import static com.alphawallet.app.ui.AddCustomRPCNetworkActivity.CHAIN_ID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectNetworkFilterActivity extends SelectNetworkBaseActivity implements TestNetDialog.TestNetDialogCallback {
    private SelectNetworkFilterViewModel viewModel;

    private MultiSelectNetworkAdapter mainNetAdapter;
    private MultiSelectNetworkAdapter testNetAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.select_active_networks));

        viewModel = new ViewModelProvider(this)
                .get(SelectNetworkFilterViewModel.class);

        setupList();

        initTestNetDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFilterList();
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

        MultiSelectNetworkAdapter.EditNetworkListener editNetworkListener = new MultiSelectNetworkAdapter.EditNetworkListener() {

            private void showPopup(View view, long chainId) {
                LayoutInflater inflater = LayoutInflater.from(SelectNetworkFilterActivity.this);
                View popupView = inflater.inflate(R.layout.popup_view_delete_network, null);


                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
                popupView.findViewById(R.id.popup_view).setOnClickListener(v -> {
                    // view network
                    Intent intent = new Intent(SelectNetworkFilterActivity.this, AddCustomRPCNetworkActivity.class);
                    intent.putExtra(CHAIN_ID, chainId);
                    startActivity(intent);
                    popupWindow.dismiss();
                });

                NetworkInfo network = viewModel.getNetworkByChain(chainId);
                if (network.isCustom) {
                    popupView.findViewById(R.id.popup_delete).setOnClickListener(v -> {
                        // delete network
                        viewModel.removeCustomNetwork(chainId);
                        popupWindow.dismiss();
                        setupFilterList();
                    });
                } else {
                    popupView.findViewById(R.id.popup_delete).setVisibility(View.GONE);
                }

                popupView.measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(popupView.getMeasuredHeight());

                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                popupWindow.setElevation(5);

                popupWindow.showAsDropDown(view);
            }

            @Override
            public void onEditNetwork(long chainId, View parent) {
                showPopup(parent, chainId);
            }
        };

        mainNetAdapter = new MultiSelectNetworkAdapter(mainNetList, editNetworkListener);
        mainnetRecyclerView.setAdapter(mainNetAdapter);

        testNetAdapter = new MultiSelectNetworkAdapter(testNetList, editNetworkListener);
        testnetRecyclerView.setAdapter(testNetAdapter);
    }

    @Override
    protected void handleSetNetworks()
    {
        List<Long> filterList = new ArrayList<>(Arrays.asList(mainNetAdapter.getSelectedItems()));
        filterList.addAll(Arrays.asList(testNetAdapter.getSelectedItems()));
        boolean hasClicked = mainNetAdapter.hasSelectedItems() || testNetAdapter.hasSelectedItems();
        boolean shouldBlankUserSelection = (mainnetSwitch.isChecked() && mainNetAdapter.getSelectedItems().length == 0)
                || (testnetSwitch.isChecked() && testNetAdapter.getSelectedItems().length == 0);

        viewModel.setFilterNetworks(filterList, mainnetSwitch.isChecked(), hasClicked, shouldBlankUserSelection);
        setResult(RESULT_OK, new Intent());
        finish();
    }

    @Override
    public void onTestNetDialogClosed()
    {
        testnetSwitch.setChecked(false);
    }

    @Override
    public void onTestNetDialogConfirmed(long newChainId)
    {
        //Shouldn't we change to testnet here?
    }

    @Override
    public void onTestNetDialogCancelled()
    {
        testnetSwitch.setChecked(false);
    }
}
