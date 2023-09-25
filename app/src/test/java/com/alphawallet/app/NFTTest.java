package com.alphawallet.app;

import static org.junit.Assert.assertEquals;

import com.alphawallet.app.util.Utils;

import org.junit.Test;

import java.math.BigInteger;

public class NFTTest
{
    //there seems to be issues with resolving IPFS files on the test machine,
    //so it's best to have unit tests which don't require to resolve the connection

    @Test
    public void testTokenURI()
    {
        // https://token-cdn-domain/{id}.json would be replaced with https://token-cdn-domain/000000000000000000000000000000000000000000000000000000000004cce0.json
        String metadataPath = "https://token-cdn-domain/{id}.json";
        String resolvedPath = Utils.parseResponseValue(metadataPath, BigInteger.valueOf(260));
        assertEquals(resolvedPath, "https://token-cdn-domain/0000000000000000000000000000000000000000000000000000000000000104.json");
    }
}
