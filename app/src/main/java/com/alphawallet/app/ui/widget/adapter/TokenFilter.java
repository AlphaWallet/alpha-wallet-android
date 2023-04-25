package com.alphawallet.app.ui.widget.adapter;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.lifi.LifiToken;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class TokenFilter
{
    private final List<LifiToken> tokens;

    public TokenFilter(List<LifiToken> tokens)
    {
        this.tokens = tokens;
        removeBadTokens();
    }

    private void removeBadTokens()
    {
        ListIterator<LifiToken> iterator = this.tokens.listIterator();
        while (iterator.hasNext())
        {
            LifiToken t = iterator.next();
            if (TextUtils.isEmpty(t.name) || TextUtils.isEmpty(t.symbol))
            {
                iterator.remove();
            }
        }
    }

    public List<LifiToken> filterBy(String keyword)
    {
        String lowerCaseKeyword = lowerCase(keyword);

        List<LifiToken> result = new ArrayList<>();
        // First filter: Add all entries that start with the keyword on top of the list.
        for (LifiToken lToken : this.tokens)
        {
            String name = lowerCase(lToken.name);
            String symbol = lowerCase(lToken.symbol);

            if (name.startsWith(lowerCaseKeyword) || symbol.startsWith(lowerCaseKeyword))
            {
                result.add(lToken);
            }
        }

        // Second filter: Add the rest of the entries that contain the keyword on top of the list.
        for (LifiToken lToken : this.tokens)
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
