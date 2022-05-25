package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TokensMappingTest {
    private String groupString;
    private TokenGroup groupEnum;

    public TokensMappingTest(String groupString, TokenGroup groupEnum) {
        this.groupString = groupString;
        this.groupEnum = groupEnum;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Assets", TokenGroup.ASSET},
                {"DeFi", TokenGroup.DEFI},
                {"Governance", TokenGroup.GOVERNANCE},
        });
    }

    @Test
    public void serializeAndDeserialize() {
        String rawJson = "{\"contracts\":[{\"address\":\"abcdefg\",\"chainId\":456789}],\"group\":\"" + groupString + "\"}";
        TokensMapping tokensMapping = new Gson().fromJson(rawJson, TokensMapping.class);

        ContractAddress contract = tokensMapping.getContracts().get(0);
        assertThat(contract.address, equalTo("abcdefg"));
        assertThat(contract.chainId, equalTo(456789L));

        assertThat(tokensMapping.getGroup(), equalTo(groupEnum));

        //Can we go back to JSON, then back again and get the same result?
        String json = new Gson().toJson(tokensMapping);
        tokensMapping = new Gson().fromJson(json, TokensMapping.class);
        contract = tokensMapping.getContracts().get(0);
        assertThat(contract.address, equalTo("abcdefg"));
        assertThat(contract.chainId, equalTo(456789L));
        assertThat(tokensMapping.getGroup(), equalTo(groupEnum));
    }

}