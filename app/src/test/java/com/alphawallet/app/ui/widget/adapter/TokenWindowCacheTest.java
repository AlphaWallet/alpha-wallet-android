package com.alphawallet.app.ui.widget.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

import com.alphawallet.app.entity.tokens.Token;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TokenWindowCacheTest
{
    @Test
    public void lookupIsCaseInsensitive()
    {
        TokenWindowCache cache = new TokenWindowCache(2);
        Token token = mock(Token.class);
        cache.putAll(tokens("0xabcdef-1", token));

        assertThat(cache.get(1, "0xABCDEF"), equalTo(token));
    }

    @Test
    public void evictsLeastRecentlyUsedTokenAtCapacity()
    {
        TokenWindowCache cache = new TokenWindowCache(2);
        Token first = mock(Token.class);
        Token second = mock(Token.class);
        Token third = mock(Token.class);

        cache.putAll(tokens("0x1-1", first));
        cache.putAll(tokens("0x2-1", second));
        assertThat(cache.get(1, "0x1"), equalTo(first));
        cache.putAll(tokens("0x3-1", third));

        assertThat(cache.size(), equalTo(2));
        assertThat(cache.get(1, "0x2"), nullValue());
        assertThat(cache.get(1, "0x1"), equalTo(first));
        assertThat(cache.get(1, "0x3"), equalTo(third));
    }

    private Map<String, Token> tokens(String key, Token token)
    {
        Map<String, Token> tokens = new HashMap<>();
        tokens.put(key, token);
        return tokens;
    }
}
