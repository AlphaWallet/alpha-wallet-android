package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.lifi.SwapProvider;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.SwapRepositoryType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

@HiltViewModel
public class SelectSwapProvidersViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;
    private final SwapRepositoryType swapRepository;

    @Inject
    public SelectSwapProvidersViewModel(
            PreferenceRepositoryType preferenceRepository,
            SwapRepositoryType swapRepository)
    {
        this.preferenceRepository = preferenceRepository;
        this.swapRepository = swapRepository;
    }

    public Set<String> getPreferredExchanges()
    {
        Set<String> exchanges = preferenceRepository.getSelectedSwapProviders();
        if (exchanges.isEmpty())
        {
            List<SwapProvider> swapProviders = getSwapProviders();
            if (swapProviders != null)
            {
                for (SwapProvider provider : swapProviders)
                {
                    exchanges.add(provider.key);
                }
                preferenceRepository.setSelectedSwapProviders(exchanges);
            }
        }
        return exchanges;
    }

    public List<SwapProvider> getSwapProviders()
    {
        List<SwapProvider> swapProviders = swapRepository.getProviders();

        if (swapProviders != null)
        {
            Set<String> preferredProviders = preferenceRepository.getSelectedSwapProviders();
            for (SwapProvider provider : swapProviders)
            {
                if (preferredProviders.contains(provider.key))
                {
                    provider.isChecked = true;
                }
            }
        }
        else
        {
            Timber.w("No Swap Providers found.");
            swapProviders = new ArrayList<>();
        }

        return swapProviders;
    }

    public boolean savePreferences(List<SwapProvider> swapProviders)
    {
        Set<String> stringSet = new HashSet<>();
        for (SwapProvider providerool : swapProviders)
        {
            if (providerool.isChecked)
            {
                stringSet.add(providerool.key);
            }
        }
        if (!stringSet.isEmpty())
        {
            preferenceRepository.setSelectedSwapProviders(stringSet);
        }
        return !stringSet.isEmpty();
    }
}
