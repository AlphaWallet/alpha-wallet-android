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
        for (Connection.LToken lToken : this.tokens)
        {
            String name = lowerCase(lToken.name);
            String symbol = lowerCase(lToken.symbol);

            if (name.startsWith(lowerCaseKeyword) || name.contains(lowerCaseKeyword)
                    || symbol.startsWith(lowerCaseKeyword) || symbol.contains(lowerCaseKeyword))
            {
                result.add(lToken);
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
