package com.alphawallet.app.ui.widget.adapter;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.token.entity.ContractAddress;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Small access-ordered cache for the token objects around the visible window.
 */
class TokenWindowCache
{
    private final Map<String, Token> tokens;

    TokenWindowCache(int maximumSize)
    {
        tokens = new LinkedHashMap<>(maximumSize, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Token> eldest)
            {
                return size() > maximumSize;
            }
        };
    }

    Token get(long chainId, String address)
    {
        if (address == null) return null;
        return tokens.get(key(chainId, address));
    }

    boolean contains(String key)
    {
        return tokens.containsKey(normalise(key));
    }

    void putAll(Map<String, Token> loaded)
    {
        for (Map.Entry<String, Token> entry : loaded.entrySet())
        {
            tokens.put(normalise(entry.getKey()), entry.getValue());
        }
    }

    void remove(TokenCardMeta token)
    {
        if (token != null)
        {
            tokens.remove(key(token.getChain(), token.getAddress()));
        }
    }

    void clear()
    {
        tokens.clear();
    }

    int size()
    {
        return tokens.size();
    }

    private static String key(long chainId, String address)
    {
        return normalise(ContractAddress.toAddressKey(chainId, address));
    }

    private static String normalise(String key)
    {
        return key.toLowerCase(Locale.ROOT);
    }
}
