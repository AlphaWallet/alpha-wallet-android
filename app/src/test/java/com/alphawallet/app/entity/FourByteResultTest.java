package com.alphawallet.app.entity;

import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FourByteResultTest
{

    Result result;

    @Before
    public void setUp()
    {
        String sampleResult = "{\n" +
            "    \"count\": 4,\n" +
            "    \"next\": null,\n" +
            "    \"previous\": null,\n" +
            "    \"results\": [\n" +
            "        {\n" +
            "            \"id\": 844293,\n" +
            "            \"created_at\": \"2022-08-26T12:22:13.363345Z\",\n" +
            "            \"text_signature\": \"watch_tg_invmru_119a5a98(address,uint256,uint256)\",\n" +
            "            \"hex_signature\": \"0x70a08231\",\n" +
            "            \"bytes_signature\": \"p \u00821\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": 166551,\n" +
            "            \"created_at\": \"2019-09-24T11:36:57.296021Z\",\n" +
            "            \"text_signature\": \"passphrase_calculate_transfer(uint64,address)\",\n" +
            "            \"hex_signature\": \"0x70a08231\",\n" +
            "            \"bytes_signature\": \"p \u00821\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": 166550,\n" +
            "            \"created_at\": \"2019-09-24T11:36:37.525020Z\",\n" +
            "            \"text_signature\": \"branch_passphrase_public(uint256,bytes8)\",\n" +
            "            \"hex_signature\": \"0x70a08231\",\n" +
            "            \"bytes_signature\": \"p \u00821\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": 143,\n" +
            "            \"created_at\": \"2016-07-09T03:58:27.545013Z\",\n" +
            "            \"text_signature\": \"balanceOf(address)\",\n" +
            "            \"hex_signature\": \"0x70a08231\",\n" +
            "            \"bytes_signature\": \"p \u00821\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

        result = new Gson().fromJson(sampleResult, Result.class);
    }

    @Test
    public void should_return_lowest_id()
    {
        String actual = result.getFirst().text_signature;
        String expected = "balanceOf(address)";

        Assert.assertTrue(actual.contains(expected));
    }
}