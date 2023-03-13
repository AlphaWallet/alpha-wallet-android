package com.alphawallet.app.ui;

import static com.alphawallet.app.ui.AddCustomRPCNetworkActivity.CHAIN_ID;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.ui.widget.adapter.MultiSelectNetworkAdapter;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.viewmodel.NetworkToggleViewModel;
import com.alphawallet.ethereum.NetworkInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NetworkToggleActivity extends NetworkBaseActivity
{
    private NetworkToggleViewModel viewModel;
    private MultiSelectNetworkAdapter mainNetAdapter;
    private MultiSelectNetworkAdapter testNetAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this)
                .get(NetworkToggleViewModel.class);
        initTestNetDialog(this);
        setupFilterList();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setupFilterList();
        viewModel.track(Analytics.Navigation.SELECT_NETWORKS);
    }

    private void setupFilterList()
    {
        List<NetworkItem> mainNetList = viewModel.getNetworkList(true);
        List<NetworkItem> testNetList = viewModel.getNetworkList(false);

        MultiSelectNetworkAdapter.Callback callback = new MultiSelectNetworkAdapter.Callback()
        {

            private void showPopup(View view, long chainId)
            {
                LayoutInflater inflater = LayoutInflater.from(NetworkToggleActivity.this);
                View popupView = inflater.inflate(R.layout.popup_view_delete_network, null);

                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
                popupView.findViewById(R.id.popup_view).setOnClickListener(v -> {
                    // view network
                    Intent intent = new Intent(NetworkToggleActivity.this, AddCustomRPCNetworkActivity.class);
                    intent.putExtra(CHAIN_ID, chainId);
                    startActivity(intent);
                    popupWindow.dismiss();
                });

                NetworkInfo network = viewModel.getNetworkByChain(chainId);
                if (network.isCustom)
                {
                    popupView.findViewById(R.id.popup_delete).setOnClickListener(v -> {
                        // delete network
                        viewModel.removeCustomNetwork(chainId);
                        popupWindow.dismiss();
                        setupFilterList();
                    });
                }
                else
                {
                    popupView.findViewById(R.id.popup_delete).setVisibility(View.GONE);
                }

                popupView.measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                popupWindow.setHeight(popupView.getMeasuredHeight());

                popupWindow.setElevation(5);

                popupWindow.showAsDropDown(view);
            }

            @Override
            public void onEditSelected(long chainId, View parent)
            {
                showPopup(parent, chainId);
            }

            @Override
            public void onCheckChanged(long chainId, int count)
            {
                updateTitle();
            }
        };

        mainNetAdapter = new MultiSelectNetworkAdapter(mainNetList, callback);
        mainnetRecyclerView.setAdapter(mainNetAdapter);

        testNetAdapter = new MultiSelectNetworkAdapter(testNetList, callback);
        testnetRecyclerView.setAdapter(testNetAdapter);

        updateTitle();
    }

    @Override
    protected void updateTitle()
    {
        if (mainNetAdapter == null || testNetAdapter == null)
        {
            return;
        }

        int count = mainNetAdapter.getSelectedItemCount();
        if (testnetSwitch.isChecked())
        {
            count += testNetAdapter.getSelectedItemCount();
        }
        setTitle(getString(R.string.title_enabled_networks, String.valueOf(count)));
    }

    @Override
    protected void handleSetNetworks()
    {
        viewModel.setTestnetEnabled(testnetSwitch.isChecked());

        List<Long> filterList = new ArrayList<>(Arrays.asList(mainNetAdapter.getSelectedItems()));
        if (testnetSwitch.isChecked())
        {
            filterList.addAll(Arrays.asList(testNetAdapter.getSelectedItems()));
        }
        boolean hasClicked = mainNetAdapter.hasSelectedItems() || testNetAdapter.hasSelectedItems();
        boolean shouldBlankUserSelection = filterList.size() == 0; //This is only set when we want to automatically discover all tokens. If user sets all networks blank it auto-fills them

        viewModel.setFilterNetworks(filterList, hasClicked, shouldBlankUserSelection);
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
