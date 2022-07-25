package com.alphawallet.app.ui.widget.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.lifi.Chain;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ChainFilterTest
{
    private ChainFilter chainFilter;

    @Before
    public void setUp() throws Exception
    {
        List<Chain> list = new ArrayList<>();
        list.add(createChain(1285, "Moonriver"));
        list.add(createChain(1, "Ethereum"));
        list.add(createChain(137, "Matic"));
        chainFilter = new ChainFilter(list);
    }

    @Test
    public void should_not_contain_unsupported_chain()
    {
        List<Chain> result = chainFilter.getSupportedChains();
        assertThat(result.get(0).name, equalTo("Ethereum"));
        assertThat(result.get(1).name, equalTo("Matic"));
    }

    @NonNull
    private Chain createChain(long id, String name)
    {
        Chain c = new Chain();
        c.id = id;
        c.name = name;
        return c;
    }
}