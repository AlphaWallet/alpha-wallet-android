package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.ui.widget.adapter.NetworkListAdapter;
import io.stormbird.wallet.ui.widget.entity.NetworkItem;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.SelectNetworkViewModel;
import io.stormbird.wallet.viewmodel.SelectNetworkViewModelFactory;

public class SelectNetworkActivity extends BaseActivity {
    @Inject
    SelectNetworkViewModelFactory viewModelFactory;
    private SelectNetworkViewModel viewModel;
    private Button confirmButton;
    private ListView listView;
    private NetworkListAdapter adapter;
    private boolean singleItem;
    private String selectedChainId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_awallet_list);
        listView = findViewById(R.id.dialog_list);
        confirmButton = findViewById(R.id.dialog_button);
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

        ArrayList<NetworkItem> list = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);

        for (NetworkInfo info : viewModel.getNetworkList()) {
            list.add(new NetworkItem(info.name, info.chainId, intList.contains(info.chainId)));
        }

        adapter = new NetworkListAdapter(this, list, selectedChainId, singleItem);
        listView.setAdapter(adapter);
        listView.setDividerHeight(0);

        if (singleItem) {
            confirmButton.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.putExtra(C.EXTRA_CHAIN_ID, adapter.getSelectedItems()[0]);
                setResult(RESULT_OK, intent);
                finish();
            });
        } else {
            confirmButton.setOnClickListener(v -> {
                viewModel.setFilterNetworks(adapter.getSelectedItems());
                sendBroadcast(new Intent(C.RESET_WALLET));
                finish();
            });
        }
    }
}
