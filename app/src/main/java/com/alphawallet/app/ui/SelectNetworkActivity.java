package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SelectNetworkViewModel;
import com.alphawallet.app.viewmodel.SelectNetworkViewModelFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;

public class SelectNetworkActivity extends BaseActivity {
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    private RecyclerView mainnetRecyclerView;
    private RecyclerView testnetRecyclerView;
    private CustomAdapter mainnetAdapter;
    private CustomAdapter testnetAdapter;
    private SwitchMaterial mainnetSwitch;
    private SwitchMaterial testnetSwitch;
    private boolean singleItem;
    private boolean switchedNetworks = false;
    private String selectedChainId = null;
    private BottomSheetDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_network);
        toolbar();
        setTitle(getString(R.string.select_active_networks));

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(SelectNetworkViewModel.class);

        if (getIntent() != null)
        {
            singleItem = getIntent().getBooleanExtra(C.EXTRA_SINGLE_ITEM, false);
            selectedChainId = getIntent().getStringExtra(C.EXTRA_CHAIN_ID);
        }

        if (selectedChainId == null || selectedChainId.isEmpty())
        {
            selectedChainId = viewModel.getFilterNetworkList();
        }

        initViews();
    }

    private void initViews()
    {
        mainnetSwitch = findViewById(R.id.mainnet_switch);
        testnetSwitch = findViewById(R.id.testnet_switch);

        mainnetSwitch.setChecked(viewModel.isActiveMainnet());
        testnetSwitch.setChecked(!viewModel.isActiveMainnet());

        mainnetRecyclerView = findViewById(R.id.main_list);
        mainnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainnetRecyclerView.addItemDecoration(new ListDivider(this));

        testnetRecyclerView = findViewById(R.id.test_list);
        testnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        testnetRecyclerView.addItemDecoration(new ListDivider(this));

        dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.layout_dialog_testnet_confirmation);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.findViewById(R.id.enable_testnet_action).setOnClickListener(v -> {
            dialog.dismiss();
        });
        dialog.findViewById(R.id.close_action).setOnClickListener(v -> {
            testnetSwitch.setChecked(false);
            dialog.dismiss();
        });

        CompoundButton.OnCheckedChangeListener mainnetListener = (compoundButton, b) -> {
            testnetSwitch.setChecked(!b);
        };

        CompoundButton.OnCheckedChangeListener testnetListener = (compoundButton, b) ->
        {
            mainnetSwitch.setOnCheckedChangeListener(null);
            mainnetSwitch.setChecked(!b);
            mainnetSwitch.setOnCheckedChangeListener(mainnetListener);

            viewModel.setActiveMainnet(!b);
            setupListVisibility();
            //ask user to confirm new dappbrowser network
            pickNewBrowserNetwork();
            switchedNetworks = true;
        };

        setupListVisibility();

        mainnetSwitch.setOnCheckedChangeListener(mainnetListener);
        testnetSwitch.setOnCheckedChangeListener(testnetListener);
    }

    private void setupListVisibility()
    {
        if (viewModel.isActiveMainnet())
        {
            testnetRecyclerView.setVisibility(View.GONE);
            mainnetRecyclerView.setVisibility(View.VISIBLE);
        }
        else
        {
            testnetRecyclerView.setVisibility(View.VISIBLE);
            mainnetRecyclerView.setVisibility(View.GONE);
        }
    }

    private void setupFilterList() {
        ArrayList<NetworkItem> mainnetlist = new ArrayList<>();
        ArrayList<NetworkItem> testnetlist = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);
        List<Integer> activeNetworks = viewModel.getActiveNetworks();

        //if active networks is empty ensure mainnet is displayed
        if (activeNetworks.size() == 0) {
            activeNetworks.add(MAINNET_ID);
            intList.add(MAINNET_ID);
        }

        //If we're in 'single selection' mode (eg dappbrowser network select) this sets up the currently selected network
        //If we're in 'group selection' mode (eg select active networks) it sets any currently selected networks
        for (NetworkInfo info : viewModel.getNetworkList())
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId))
            {
                mainnetlist.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
            }
            else
            {
                testnetlist.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
            }
        }

        mainnetAdapter = new CustomAdapter(mainnetlist, singleItem);
        mainnetRecyclerView.setAdapter(mainnetAdapter);

        testnetAdapter = new CustomAdapter(testnetlist, singleItem);
        testnetRecyclerView.setAdapter(testnetAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            handleSetNetworks();
        }
        else if (item.getItemId() == R.id.action_filter)
        {
            viewModel.openFilterSelect(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleSetNetworks();
    }

    private void handleSetNetworks() {
        Integer[] filterList = mainnetSwitch.isChecked() ? mainnetAdapter.getSelectedItems() : testnetAdapter.getSelectedItems();
        if (filterList.length == 0)
        {
            filterList = EthereumNetworkRepository.addDefaultNetworks().toArray(new Integer[0]);
        }

        if (singleItem) {
            //ensure any single selection is made into an active network
            // - eg if user is using xDai in a dapp, received xdai token they won't appear unless they also activated xDai network
            addNetworkToActiveList(filterList[0]);
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_CHAIN_ID, filterList[0]);
            setResult(RESULT_OK, intent);
        } else {
            viewModel.setFilterNetworks(filterList);
            sendBroadcast(new Intent(C.RESET_WALLET));
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFilterList();
    }

    /**
     * User just switched between testnet / mainnet, prompt them to pick a new dappbrowser network if in network selection
     *  - or just pick the first network if we're already picking a single item
     */
    private void pickNewBrowserNetwork()
    {
        if (!singleItem)
        {
            NetworkInfo currentNetwork = viewModel.getDefaultNetwork();
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(SelectNetworkActivity.this);
                AlertDialog dialog = builder.setTitle(R.string.change_network_title)
                        .setMessage(getString(R.string.change_network_message, currentNetwork.getShortName()))
                        .setPositiveButton(R.string.ok, (d, w) -> {
                            //remove network from filter
                            setSelectedNetworks();
                            //open network select
                            selectNetwork(getFirstSelectionItem().getChainId());
                        })
                        .create();
                dialog.show();
            });
        }
        else
        {
            acceptPreviousOrSetDefault();
        }
    }

    //single selection: is an item already set? If not, select first item in list to be active
    private void acceptPreviousOrSetDefault()
    {
        CustomAdapter activeAdapter = viewModel.isActiveMainnet() ? mainnetAdapter : testnetAdapter;
        if (!activeAdapter.hasSelection())
        {
            getFirstSelectionItem().setSelected(true);
            activeAdapter.notifyItemChanged(0);
        }
    }

    private NetworkItem getFirstSelectionItem()
    {
        CustomAdapter useAdapter = viewModel.isActiveMainnet() ? mainnetAdapter : testnetAdapter;
        if (useAdapter.dataSet.size() > 0)
        {
            return useAdapter.dataSet.get(0);
        }
        else
        {
            return new NetworkItem(C.ETHEREUM_NETWORK_NAME, MAINNET_ID, false);
        }
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
        private ArrayList<NetworkItem> dataSet;
        private final boolean singleItem;

        public Integer[] getSelectedItems() {
            List<Integer> enabledIds = new ArrayList<>();
            for (NetworkItem data : dataSet) {
                if (data.isSelected()) enabledIds.add(data.getChainId());
            }

            return enabledIds.toArray(new Integer[0]);
        }

        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int buttonTypeId = singleItem ? R.layout.item_simple_radio : R.layout.item_simple_check;
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(buttonTypeId, parent, false);

            return new CustomAdapter.CustomViewHolder(itemView);
        }

        // Determine if the adapter already has an item selected
        public boolean hasSelection()
        {
            for (NetworkItem data : dataSet) {
                if (data.isSelected()) return true;
            }

            return false;
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
                checkDappNetwork(position);
            }
            holder.checkbox.setSelected(dataSet.get(position).isSelected());
        }

        private void checkDappNetwork(int position)
        {
            NetworkInfo currentNetwork = viewModel.getDefaultNetwork();
            NetworkItem selectedItem = dataSet.get(position);
            if (!singleItem && !selectedItem.isSelected() && currentNetwork != null && selectedItem.getChainId() == currentNetwork.chainId)
            {
                //deselected active network - need to revert or select a new active network
                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SelectNetworkActivity.this);
                    AlertDialog dialog = builder.setTitle(getString(R.string.disconnect_title, currentNetwork.getShortName()))
                            .setMessage(getString(R.string.disconnect_body, currentNetwork.getShortName()))
                            .setPositiveButton(R.string.disconnect, (d, w) -> {
                                //remove network from filters
                                setSelectedNetworks();
                                //open network select
                                selectNetwork(dataSet.get(0).getChainId());
                            })
                            .setNegativeButton(R.string.keep_connect, (d, w) -> {
                                selectedItem.setSelected(true);
                                notifyItemChanged(position);
                            })
                            .create();
                    dialog.show();
                });
            }
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }

    private void setSelectedNetworks()
    {
        Integer[] filterList = mainnetSwitch.isChecked() ? mainnetAdapter.getSelectedItems() : testnetAdapter.getSelectedItems();
        viewModel.setFilterNetworks(filterList);
    }

    private void selectNetwork(int chainId)
    {
        Intent intent = new Intent(this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, String.valueOf(chainId));
        startActivityForResult(intent, C.REQUEST_SELECT_NETWORK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == C.REQUEST_SELECT_NETWORK && resultCode == RESULT_OK)
        {
            int networkId = data.getIntExtra(C.EXTRA_CHAIN_ID, 1);
            viewModel.setActiveNetwork(networkId);
            //ensure this network is set active
            List<Integer> filterList = new ArrayList<>(Arrays.asList(EthereumNetworkRepository.hasRealValue(networkId) ?
                    mainnetAdapter.getSelectedItems() : testnetAdapter.getSelectedItems()));
            if (!filterList.contains(networkId)) { filterList.add(networkId); }
            selectedChainId = Utils.integerListToString(filterList, false);

            //reset dappbrowser
            Intent intent = new Intent(C.CHANGED_NETWORK);
            intent.putExtra(C.EXTRA_CHAIN_ID, networkId);
            sendBroadcast(intent);
        }
    }

    // Add network filter to prefs
    private void addNetworkToActiveList(int chainId)
    {
        List<Integer> activeNetworks = switchedNetworks ? new ArrayList<>() : viewModel.getActiveNetworks(); //if we switched networks then use a blank list
        if (!activeNetworks.contains(chainId)) { activeNetworks.add(chainId); }
        viewModel.setFilterNetworks(activeNetworks.toArray(new Integer[0]));
    }
}
