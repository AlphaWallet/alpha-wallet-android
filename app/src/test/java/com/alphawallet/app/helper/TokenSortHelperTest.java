package com.alphawallet.app.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.lifi.Connection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TokenSortHelperTest
{

    @Test
    public void tokens_with_balance_are_showed_first_sorted_in_descending_order()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Ethereum", "ETH", "0x0", "0"));
        list.add(createToken("Binance Smart Chain", "BNB", "0x1", "1"));
        list.add(createToken("Solana", "SOL", "0x2", "2"));
        TokenSortHelper.sort(list);
        assertThat(list.get(0).symbol, equalTo("SOL"));
        assertThat(list.get(1).symbol, equalTo("BNB"));
        assertThat(list.get(2).symbol, equalTo("ETH"));
    }

    @Test
    public void tokens_with_zero_balance_are_sorted_alphabetically()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("WETH", "WETH", "0x3", "0"));
        list.add(createToken("Ethereum", "ETH", "0x0", "0"));
        list.add(createToken("Binance Smart Chain", "BNB", "0x1", "1"));
        list.add(createToken("Solana", "SOL", "0x2", "2"));

        TokenSortHelper.sort(list);

        assertThat(list.get(0).symbol, equalTo("SOL"));
        assertThat(list.get(1).symbol, equalTo("BNB"));
        assertThat(list.get(2).symbol, equalTo("ETH"));
        assertThat(list.get(3).symbol, equalTo("WETH"));
    }

    @Test
    public void should_be_case_insensitive()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Stox", "STX", "0x0", "0"));
        list.add(createToken("stETH", "stETH", "0x3", "0"));

        TokenSortHelper.sort(list);

        assertThat(list.get(0).symbol, equalTo("stETH"));
        assertThat(list.get(1).symbol, equalTo("STX"));
    }

    private Connection.LToken createToken(String name, String symbol, String address, String balance)
    {
        Connection.LToken lToken = new Connection.LToken();
        lToken.name = name;
        lToken.symbol = symbol;
        lToken.address = address;
        lToken.balance = balance;
        return lToken;
    }
}