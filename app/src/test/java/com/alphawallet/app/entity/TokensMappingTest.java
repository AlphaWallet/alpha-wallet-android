package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class TokensMappingTest {
    @Test
    public void serializeAndDeserialize() {
        String rawJson = "{\"contracts\":[{\"address\":\"abcdefg\",\"chainId\":456789}],\"group\":\"Assets\"}";
        TokensMapping tokensMapping = new Gson().fromJson(rawJson, TokensMapping.class);

        ContractAddress contract = tokensMapping.getContracts().get(0);
        assertThat(contract.address, equalTo("abcdefg"));
        assertThat(contract.chainId, equalTo(456789L));

        assertThat(tokensMapping.getGroup(), equalTo(TokenGroup.ASSET));

        //Can we go back to JSON, then back again and get the same result?
        String json = new Gson().toJson(tokensMapping);
        tokensMapping = new Gson().fromJson(json, TokensMapping.class);
        contract = tokensMapping.getContracts().get(0);
        assertThat(contract.address, equalTo("abcdefg"));
        assertThat(contract.chainId, equalTo(456789L));
        assertThat(tokensMapping.getGroup(), equalTo(TokenGroup.ASSET));
    }
}