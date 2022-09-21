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

    public Set<String> getPreferredExchanges(Context context)
    {
        Set<String> exchanges = preferenceRepository.getSelectedExchanges();
        if (exchanges.isEmpty())
        {
            List<ToolDetails> tools = getTools(context);
            if (tools != null)
            {
                for (ToolDetails tool : tools)
                {
                    exchanges.add(tool.key);
                }
                preferenceRepository.setSelectedExchanges(exchanges);
            }
        }
        return exchanges;
    }

    public List<ToolDetails> getTools(Context context)
    {
        List<ToolDetails> tools = new Gson().fromJson(Utils.loadJSONFromAsset(context, "swap_providers_list.json"),
                new TypeToken<List<ToolDetails>>()
                {
                }.getType());

        if (tools != null)
        {
            Set<String> preferredProviders =  preferenceRepository.getSelectedExchanges();
            for (ToolDetails tool : tools)
            {
                if (preferredProviders.contains(tool.key))
                {
                    tool.isChecked = true;
                }
            }
        }

        return tools;
    }

    public boolean savePreferences(List<ToolDetails> selectedExchanges)
    {
        Set<String> stringSet = new HashSet<>();
        for (ToolDetails tool : selectedExchanges)
        {
            if (tool.isChecked)
            {
                stringSet.add(tool.key);
            }
        }
        if (!stringSet.isEmpty())
        {
            preferenceRepository.setSelectedExchanges(stringSet);
        }
        return !stringSet.isEmpty();
    }
}
