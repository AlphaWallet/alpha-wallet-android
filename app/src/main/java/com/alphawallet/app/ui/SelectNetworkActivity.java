package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private boolean singleItemMode;
    private BottomSheetDialog testnetDialog;
    private FrameLayout mainnetFrame;
    private FrameLayout testnetFrame;
    private List<Integer> selectedNetworks;
    private boolean isMainNetActive;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_network);
        toolbar();
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(SelectNetworkViewModel.class);

        initViews();

        Intent intent = getIntent();
        String selectedChainId = null;

        if (intent != null)
        {
            singleItemMode = intent.getBooleanExtra(C.EXTRA_SINGLE_ITEM, false);
            selectedChainId = intent.getStringExtra(C.EXTRA_CHAIN_ID);

            if (singleItemMode)
            {
                setTitle(getString(R.string.select_dappbrowser_network));
                if (selectedChainId == null || selectedChainId.isEmpty()) {
                    selectedChainId = String.valueOf(MAINNET_ID);
                }
            }
            else
            {
                setTitle(getString(R.string.select_active_networks));
                if (selectedChainId == null || selectedChainId.isEmpty()) {
                    selectedChainId = viewModel.getFilterNetworkList();
                }
            }

            selectedNetworks = Utils.intListToArray(selectedChainId);

            isMainNetActive = viewModel.isMainNet(selectedNetworks.get(0));

            prepareUi();

            setupFilterList();
        }
    }

    private void hideSwitches()
    {
        mainnetFrame.setVisibility(View.GONE);
        testnetFrame.setVisibility(View.GONE);
        mainnetSwitch.setVisibility(View.GONE);
        testnetSwitch.setVisibility(View.GONE);
    }

    private void initViews()
    {
        mainnetFrame = findViewById(R.id.mainnet_frame);
        testnetFrame = findViewById(R.id.testnet_frame);

        mainnetSwitch = findViewById(R.id.mainnet_switch);
        testnetSwitch = findViewById(R.id.testnet_switch);

        mainnetRecyclerView = findViewById(R.id.main_list);
        mainnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainnetRecyclerView.addItemDecoration(new ListDivider(this));

        testnetRecyclerView = findViewById(R.id.test_list);
        testnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        testnetRecyclerView.addItemDecoration(new ListDivider(this));

        testnetDialog = new BottomSheetDialog(this);
        testnetDialog.setContentView(R.layout.layout_dialog_testnet_confirmation);
        testnetDialog.setCancelable(true);
        testnetDialog.setCanceledOnTouchOutside(true);
        testnetDialog.findViewById(R.id.enable_testnet_action).setOnClickListener(v -> {
            testnetDialog.dismiss();
            setDefaultSelection();
        });
        testnetDialog.findViewById(R.id.close_action).setOnClickListener(v -> {
            testnetSwitch.setChecked(false);
            testnetDialog.dismiss();
        });
    }

    private void prepareUi()
    {
        mainnetSwitch.setChecked(isMainNetActive);
        testnetSwitch.setChecked(!isMainNetActive);

        CompoundButton.OnCheckedChangeListener mainnetListener = (compoundButton, b) -> {
            testnetSwitch.setChecked(!b);
        };

        CompoundButton.OnCheckedChangeListener testnetListener = (compoundButton, b) ->
        {
            mainnetSwitch.setOnCheckedChangeListener(null);
            mainnetSwitch.setChecked(!b);
            mainnetSwitch.setOnCheckedChangeListener(mainnetListener);

            isMainNetActive = !b;

            setupListVisibility();

            if (isMainNetActive)
            {
                setDefaultSelection();
            }
            else
            {
                testnetDialog.show();
                viewModel.setShownTestNetWarning();
                //enable if we want to be able to do a 'don't show me again'
//                if (!viewModel.hasShownTestNetWarning())
//                {
//                    testnetDialog.show();
//                    viewModel.setShownTestNetWarning();
//                }
//                else
//                {
//                    setDefaultSelection();
//                }
            }
        };

        mainnetSwitch.setOnCheckedChangeListener(mainnetListener);
        testnetSwitch.setOnCheckedChangeListener(testnetListener);

        setupListVisibility();
    }

    private void setupListVisibility()
    {
        if (isMainNetActive)
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
        ArrayList<NetworkItem> mainNetList = new ArrayList<>();
        ArrayList<NetworkItem> testNetList = new ArrayList<>();

        //If we're in 'single selection' mode (eg dappbrowser network select) this sets up the currently selected network
        //If we're in 'group selection' mode (eg select active networks) it sets any currently selected networks
        for (NetworkInfo info : viewModel.getNetworkList())
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

        mainnetAdapter = new CustomAdapter(mainNetList, singleItemMode);
        mainnetRecyclerView.setAdapter(mainnetAdapter);

        testnetAdapter = new CustomAdapter(testNetList, singleItemMode);
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
        return false;
    }

    @Override
    public void onBackPressed() {
        handleSetNetworks();
    }

    private void handleSetNetworks() {
        Integer[] filterList = mainnetSwitch.isChecked() ? mainnetAdapter.getSelectedItems() : testnetAdapter.getSelectedItems();

        if (filterList.length <= 0)
        {
            // Display toast until user selects at least one active network.
            Toast.makeText(this, "Please select at least one network.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            if (singleItemMode)
            {
                Intent intent = new Intent();
                intent.putExtra(C.EXTRA_CHAIN_ID, filterList[0]);
                setResult(RESULT_OK, intent);
            }
            else
            {
                viewModel.setFilterNetworks(filterList);
            }

            finish();
        }
    }

    private void setDefaultSelection()
    {
        CustomAdapter activeAdapter = isMainNetActive ? mainnetAdapter : testnetAdapter;

        //single selection: is an item already set? If not, select first item in list to be active
        if (!activeAdapter.hasSelection())
        {
            getFirstSelectionItem().setSelected(true);
            activeAdapter.notifyItemChanged(0);
        }
    }

    private NetworkItem getFirstSelectionItem()
    {
        CustomAdapter useAdapter = isMainNetActive ? mainnetAdapter : testnetAdapter;
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
            }
            holder.checkbox.setSelected(dataSet.get(position).isSelected());
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }
    }
}
