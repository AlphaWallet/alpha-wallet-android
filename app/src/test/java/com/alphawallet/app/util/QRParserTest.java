package com.alphawallet.app.util;

import static org.junit.Assert.assertNotNull;

import com.alphawallet.app.entity.QRResult;

import org.junit.Test;

public class QRParserTest
{

    @Test
    public void should_protocol_not_be_null_given_normal_url()
    {
        QRParser parser = QRParser.getInstance(null);
        String url = "http://alphawallet.com";
        QRResult result = parser.parse(url);
        assertNotNull(result);
        assertNotNull(result.getProtocol()); // it causes NPE when in switch case
    }
}