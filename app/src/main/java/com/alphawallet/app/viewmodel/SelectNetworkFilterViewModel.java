package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;

import java.util.List;

public class SelectNetworkFilterViewModel extends BaseViewModel {
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final PreferenceRepositoryType preferenceRepository;

    public SelectNetworkFilterViewModel(EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                                        TokensService tokensService,
                                        PreferenceRepositoryType preferenceRepository) {
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.preferenceRepository = preferenceRepository;
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
        NetworkInfo activeNetwork = networkRepository.getActiveBrowserNetwork();
        int activeNetworkId = -99;
        if (activeNetwork != null) {
            activeNetworkId = networkRepository.getActiveBrowserNetwork().chainId;
        }
        boolean deselected = true;

        Integer[] selectedIds = new Integer[selectedItems.length];
        int index = 0;
        for (Integer selectedId : selectedItems) {
            if (activeNetworkId == selectedId) {
                deselected = false;
            }
            selectedIds[index++] = selectedId;
        }

        if (deselected) networkRepository.setActiveBrowserNetwork(null);
        networkRepository.setFilterNetworkList(selectedIds);
        tokensService.setupFilter();

        //set all tokens as having visibility changed
        preferenceRepository.setHasSetNetworkFilters();
    }

    public boolean hasShownTestNetWarning()
    {
        return preferenceRepository.hasShownTestNetWarning();
    }

    public void setShownTestNetWarning()
    {
        preferenceRepository.setShownTestNetWarning();
    }

    public NetworkInfo getNetworkByChain(int chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }
}
