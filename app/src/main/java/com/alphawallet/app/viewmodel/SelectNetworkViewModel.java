package com.alphawallet.app.viewmodel;

import java.util.List;

import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.ui.SelectNetworkActivity;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.service.TokensService;

public class SelectNetworkViewModel extends BaseViewModel {
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;

    public SelectNetworkViewModel(EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                                  TokensService tokensService) {
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
    }

    public NetworkInfo[] getNetworkList() {
        return networkRepository.getAvailableNetworkList();
    }

    public String getFilterNetworkList() {
        List<Integer> networkIds = networkRepository.getFilterNetworkList();
        StringBuilder sb = new StringBuilder();
        boolean firstValue = true;
        for (int networkId : networkIds) {
            if (!firstValue) sb.append(",");
            sb.append(networkId);
            firstValue = false;
        }
        return sb.toString();
    }

    public void setFilterNetworks(Integer[] selectedItems) {
        int[] selectedIds = new int[selectedItems.length];
        int index = 0;
        for (Integer selectedId : selectedItems) {
            selectedIds[index++] = selectedId;
        }
        networkRepository.setFilterNetworkList(selectedIds);
        tokensService.setupFilter();
    }

    public List<Integer> getActiveNetworks() {
        return networkRepository.getFilterNetworkList();
    }

    public void openFilterSelect(Activity ctx)
    {
        Intent intent = new Intent(ctx, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }
}
