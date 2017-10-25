package com.wallet.crypto.trustapp;

import com.wallet.crypto.trustapp.controller.Utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class QRExtractorTest {
    @Test
    public void extractingIsCorrect() throws Exception {

        // Correct string
        String extractedString = Utils.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // String without value
        extractedString = Utils.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Negative: address too long
        extractedString = Utils.extractAddressFromQrString("notethereum:0x0000000000000000000000000000000000000000123");
        assertTrue(extractedString == null);

        // Negative: invalid prefix
        extractedString = Utils.extractAddressFromQrString("notethereum:0x0000000000000000000000000000000000000000");
        assertTrue(extractedString == null);

        // Negative: invalid postfix
        extractedString = Utils.extractAddressFromQrString("notethereum:0x0000000000000000000000000000000000000000?value=0invalid");
        assertTrue(extractedString == null);

        // Negative: missing 'ethereum'
        extractedString = Utils.extractAddressFromQrString(":0x0000000000000000000000000000000000000000?value=0invalid");
        assertTrue(extractedString == null);
    }
}