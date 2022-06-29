package com.alphawallet.app.entity.walletconnect;

import com.walletconnect.walletconnectv2.client.Sign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NamespaceParser
{
    private List<String> chains;
    private List<String> methods;
    private List<String> events;

    public NamespaceParser(Map<String, Sign.Model.Namespace.Proposal> requiredNamespaces)
    {
        chains = new ArrayList<>();
        methods = new ArrayList<>();
        events = new ArrayList<>();
        for (Map.Entry<String, Sign.Model.Namespace.Proposal> entry : requiredNamespaces.entrySet())
        {
            chains.addAll(entry.getValue().getChains());
            methods.addAll(entry.getValue().getMethods());
            events.addAll(entry.getValue().getEvents());
        }
    }

    public List<String> getChains()
    {
        return chains;
    }

    public List<String> getMethods()
    {
        return methods;
    }

    public List<String> getEvents()
    {
        return events;
    }
}
