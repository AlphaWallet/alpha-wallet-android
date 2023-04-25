package com.alphawallet.app.ui.widget.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.lifi.LifiToken;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LifiTokenFilterTest
{
    private TokenFilter tokenFilter;

    @Before
    public void setUp() throws Exception
    {
        List<LifiToken> list = new ArrayList<>();
        list.add(createToken("Ethereum", "ETH", "1"));
        list.add(createToken("Solana", "SOL", "2"));
        list.add(createToken("Binance", "BNB", "3"));
        list.add(createToken("", "", "4"));
        tokenFilter = new TokenFilter(list);
    }

    @Test
    public void nameContains()
    {
        List<LifiToken> result = tokenFilter.filterBy("an");
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0).name, equalTo("Solana"));
        assertThat(result.get(1).name, equalTo("Binance"));
    }

    @Test
    public void nameStartsWith()
    {
        List<LifiToken> result = tokenFilter.filterBy("So");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).name, equalTo("Solana"));
    }

    @Test
    public void symbolContains()
    {
        List<LifiToken> result = tokenFilter.filterBy("B");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).name, equalTo("Binance"));
    }

    @Test
    public void symbolStartsWith()
    {
        List<LifiToken> result = tokenFilter.filterBy("S");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).name, equalTo("Solana"));
    }

    @Test
    public void should_be_case_insensitive()
    {
        List<LifiToken> result = tokenFilter.filterBy("s");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).name, equalTo("Solana"));

        result = tokenFilter.filterBy("b");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).name, equalTo("Binance"));
    }

    @Test
    public void should_sort_starts_with_in_front_of_contains()
    {
        List<LifiToken> list = new ArrayList<>();
        list.add(createToken("Solana", "SOL", "2"));
        list.add(createToken("WETH", "WETH", "2"));
        list.add(createToken("Ethereum", "ETH", "1"));
        tokenFilter = new TokenFilter(list);

        List<LifiToken> result = tokenFilter.filterBy("eth");
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0).name, equalTo("Ethereum"));
        assertThat(result.get(1).name, equalTo("WETH"));
    }

    @NonNull
    private LifiToken createToken(String name, String symbol, String address)
    {
        LifiToken e = new LifiToken();
        e.name = name;
        e.symbol = symbol;
        e.address = address;
        return e;
    }
}