package com.alphawallet.app.ui.widget.adapter;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.lifi.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TokenFilter
{
    private final List<Connection.LToken> tokens;

    public TokenFilter(List<Connection.LToken> tokens)
    {
        this.tokens = tokens;
    }

    public List<Connection.LToken> filterBy(String keyword)
    {
        String lowerCaseKeyword = lowerCase(keyword);

        List<Connection.LToken> result = new ArrayList<>();
        // First filter: Add all entries that start with the keyword on top of the list.
        for (Connection.LToken lToken : this.tokens)
        {
            String name = lowerCase(lToken.name);
            String symbol = lowerCase(lToken.symbol);

            if (name.startsWith(lowerCaseKeyword) || symbol.startsWith(lowerCaseKeyword))
            {
                result.add(lToken);
            }
        }

        // Second filter: Add the rest of the entries that contain the keyword on top of the list.
        for (Connection.LToken lToken : this.tokens)
        {
            String name = lowerCase(lToken.name);
            String symbol = lowerCase(lToken.symbol);

            if (name.contains(lowerCaseKeyword) || symbol.contains(lowerCaseKeyword))
            {
                if (!result.contains(lToken))
                {
                    result.add(lToken);
                }
            }
        }
        return result;
    }

    @NonNull
    private String lowerCase(String name)
    {
        return name.toLowerCase(Locale.ENGLISH);
    }

}
