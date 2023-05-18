package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowKeyService.class})
public class TokenFilterTest
{
    private TokenFilter tokenFilter;

    @Before
    public void setUp() throws Exception
    {
        List<Token> list = new ArrayList<>();
        list.add(createToken("Ethereum", "ETH", "1"));
        list.add(createToken("Solana", "SOL", "2"));
        list.add(createToken("Binance", "BNB", "3"));
        list.add(createToken("", "", "4"));
        tokenFilter = new TokenFilter(list);
    }

    @Test
    public void nameContains()
    {
        List<Token> result = tokenFilter.filterBy("an");
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0).tokenInfo.name, equalTo("Solana"));
        assertThat(result.get(1).tokenInfo.name, equalTo("Binance"));
    }

    @Test
    public void nameStartsWith()
    {
        List<Token> result = tokenFilter.filterBy("So");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).tokenInfo.name, equalTo("Solana"));
    }

    @Test
    public void symbolContains()
    {
        List<Token> result = tokenFilter.filterBy("B");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).tokenInfo.name, equalTo("Binance"));
    }

    @Test
    public void symbolStartsWith()
    {
        List<Token> result = tokenFilter.filterBy("S");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).tokenInfo.name, equalTo("Solana"));
    }

    @Test
    public void should_be_case_insensitive()
    {
        List<Token> result = tokenFilter.filterBy("s");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).tokenInfo.name, equalTo("Solana"));

        result = tokenFilter.filterBy("b");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).tokenInfo.name, equalTo("Binance"));
    }

    @Test
    public void should_sort_starts_with_in_front_of_contains()
    {
        List<Token> list = new ArrayList<>();
        list.add(createToken("Solana", "SOL", "2"));
        list.add(createToken("WETH", "WETH", "2"));
        list.add(createToken("Ethereum", "ETH", "1"));
        tokenFilter = new TokenFilter(list);

        List<Token> result = tokenFilter.filterBy("eth");
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0).tokenInfo.name, equalTo("Ethereum"));
        assertThat(result.get(1).tokenInfo.name, equalTo("WETH"));
    }

    @NonNull
    private Token createToken(String name, String symbol, String address)
    {
        TokenInfo tokenInfo =
            new TokenInfo(
                address,
                name,
                symbol,
                18,
                true,
                1
            );
        return new Token(tokenInfo, BigDecimal.ZERO, System.currentTimeMillis() / 1000, "Ethereum", ContractType.ERC20);
    }
}