package com.alphawallet.app.entity.walletconnect;

import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NamespaceParser
{
    private final List<String> chains = new ArrayList<>();
    private final List<String> methods = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final List<String> wallets = new ArrayList<>();

    public void parseProposal(Map<String, Sign.Model.Namespace.Proposal> requiredNamespaces)
    {
        for (Map.Entry<String, Sign.Model.Namespace.Proposal> entry : requiredNamespaces.entrySet())
        {
            chains.addAll(entry.getValue().getChains());
            methods.addAll(entry.getValue().getMethods());
            events.addAll(entry.getValue().getEvents());
        }
    }

    public void parseSession(Map<String, Sign.Model.Namespace.Session> namespaces)
    {
        for (Map.Entry<String, Sign.Model.Namespace.Session> entry : namespaces.entrySet())
        {
            chains.addAll(parseChains(entry.getValue().getAccounts()));
            methods.addAll(entry.getValue().getMethods());
            events.addAll(entry.getValue().getEvents());
            wallets.addAll(parseWallets(entry.getValue().getAccounts()));
        }
    }

    private List<String> parseWallets(List<String> accounts)
    {
        return accounts.stream()
                .map((account) -> account.substring(account.lastIndexOf(":") + 1))
                .collect(Collectors.toList());
    }

    private List<String> parseChains(List<String> accounts)
    {
        return accounts.stream()
                .map((account) -> account.substring(0, account.lastIndexOf(":")))
                .collect(Collectors.toList());
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

    public List<String> getWallets()
    {
        return new ArrayList<>(new HashSet<>(wallets));
    }
}
