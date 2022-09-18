package com.alphawallet.app.viewmodel;

import android.content.Context;

import com.alphawallet.app.entity.lifi.ToolDetails;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SelectExchangesViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;

    @Inject
    public SelectExchangesViewModel(
            PreferenceRepositoryType preferenceRepository)
    {
        this.preferenceRepository = preferenceRepository;
    }

    public List<ToolDetails> getTools(Context context)
    {
        List<ToolDetails> tools = new Gson().fromJson(Utils.loadJSONFromAsset(context, "swap_providers_list.json"),
                new TypeToken<List<ToolDetails>>()
                {
                }.getType());

        if (tools != null)
        {
            Set<String> preferredProviders = getPreferredProviders();
            if (preferredProviders.isEmpty())
            {
                for (ToolDetails tool : tools)
                {
                    tool.isChecked = true;
                }
            }
            else
            {
                for (ToolDetails tool : tools)
                {
                    if (preferredProviders.contains(tool.key))
                    {
                        tool.isChecked = true;
                    }
                }
            }
        }

        return tools;
    }

    public Set<String> getPreferredProviders()
    {
        return preferenceRepository.getSwapProviders();
    }

    public void savePreferences(List<String> preferredProviders)
    {
        Set<String> stringSet = new HashSet<>(preferredProviders);
        preferenceRepository.setSwapProviders(stringSet);
    }
}
