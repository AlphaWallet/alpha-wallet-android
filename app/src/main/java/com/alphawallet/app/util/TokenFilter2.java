package com.alphawallet.app.util;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.tokens.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TokenFilter2
{
    private final List<Token> tokens;

    public TokenFilter2(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    public List<Token> filterBy(String keyword)
    {
        String lowerCaseKeyword = lowerCase(keyword);

        List<Token> result = new ArrayList<>();
        for (Token t : this.tokens)
        {
            String name = lowerCase(t.getFullName());
            String symbol = lowerCase(t.getSymbol());

            if (name.startsWith(lowerCaseKeyword) || symbol.startsWith(lowerCaseKeyword))
            {
                result.add(0, t);
            }
            else if (name.contains(lowerCaseKeyword) || symbol.contains(lowerCaseKeyword))
            {
                if (!result.contains(t))
                {
                    result.add(t);
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
