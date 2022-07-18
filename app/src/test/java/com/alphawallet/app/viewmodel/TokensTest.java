package com.alphawallet.app.viewmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.lifi.Connection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TokensTest
{
    @Test
    public void sort_token_by_fiat_value_in_DESC()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Ethereum", "ETH", "0x0", 0));
        list.add(createToken("Binance Smart Chain", "BNB", "0x1", 1));
        list.add(createToken("Solana", "SOL", "0x2", 2));

        Tokens.sortValue(list);

        assertThat(list.get(0).symbol, equalTo("SOL"));
        assertThat(list.get(1).symbol, equalTo("BNB"));
        assertThat(list.get(2).symbol, equalTo("ETH"));
    }

    @Test
    public void sort_tokens_by_name_alphabetically()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Ethereum", "ETH", "0x0", 0));
        list.add(createToken("Binance Smart Chain", "BNB", "0x1", 0));
        list.add(createToken("Solana", "SOL", "0x2", 0));

        Tokens.sortName(list);

        assertThat(list.get(0).symbol, equalTo("BNB"));
        assertThat(list.get(1).symbol, equalTo("ETH"));
        assertThat(list.get(2).symbol, equalTo("SOL"));
    }

    @Test
    public void sort_name_should_return_native_token_first()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Solana", "SOL", "0x0", 0));
        list.add(createToken("Stox", "STX", "0x0000000000000000000000000000000000000000", 0));
        list.add(createToken("stETH", "stETH", "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", 0));

        Tokens.sortName(list);

        assertThat(list.get(0).symbol, equalTo("stETH"));
        assertThat(list.get(1).symbol, equalTo("STX"));
        assertThat(list.get(2).symbol, equalTo("SOL"));
    }

    @Test
    public void sort_name_should_be_case_insensitive()
    {
        List<Connection.LToken> list = new ArrayList<>();
        list.add(createToken("Stox", "STX", "0x0", 0));
        list.add(createToken("stETH", "stETH", "0x3", 0));

        Tokens.sortName(list);

        assertThat(list.get(0).symbol, equalTo("stETH"));
        assertThat(list.get(1).symbol, equalTo("STX"));
    }

    private Connection.LToken createToken(String name, String symbol, String address, double fiatEquivalent)
    {
        Connection.LToken lToken = new Connection.LToken();
        lToken.name = name;
        lToken.symbol = symbol;
        lToken.address = address;
        lToken.fiatEquivalent = fiatEquivalent;
        return lToken;
    }
}