package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.NetworkItem;

import java.util.ArrayList;
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

    public void setFilterNetworks(List<Integer> selectedItems, boolean mainnetActive, boolean hasSelected) {
        preferenceRepository.setActiveMainnet(mainnetActive);
        if (hasSelected) preferenceRepository.setHasSetNetworkFilters();

        NetworkInfo activeNetwork = networkRepository.getActiveBrowserNetwork();
        int activeNetworkId = -99;
        if (activeNetwork != null) {
            activeNetworkId = networkRepository.getActiveBrowserNetwork().chainId;
        }

        //Mark dappbrowser network as deselected if appropriate
        boolean deselected = true;
        Integer[] selectedIds = new Integer[selectedItems.size()];
        int index = 0;
        for (Integer selectedId : selectedItems) {
            if (EthereumNetworkRepository.hasRealValue(selectedId) == mainnetActive && activeNetworkId == selectedId) {
                deselected = false;
            }
            selectedIds[index++] = selectedId;
        }

        if (deselected) networkRepository.setActiveBrowserNetwork(null);
        networkRepository.setFilterNetworkList(selectedIds);
        tokensService.setupFilter();

        preferenceRepository.commit();
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

    public boolean mainNetActive()
    {
        return preferenceRepository.isActiveMainnet();
    }

    public List<NetworkItem> getNetworkList(boolean isMainNet)
    {
        List<NetworkItem> networkList = new ArrayList<>();
        List<Integer> filterIds = networkRepository.getSelectedFilters(isMainNet);

        for (NetworkInfo info : getNetworkList())
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId) == isMainNet)
            {
                networkList.add(new NetworkItem(info.name, info.chainId, filterIds.contains(info.chainId)));
            }
        }

        return networkList;
    }
}
