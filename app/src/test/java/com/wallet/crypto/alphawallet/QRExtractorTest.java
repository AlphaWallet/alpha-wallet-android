package com.wallet.crypto.trustapp;

import com.wallet.crypto.trustapp.util.QRURLParser;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class QRExtractorTest {

    @Test
    public void extractingIsCorrect() throws Exception {

        QRURLParser parser = QRURLParser.getInstance();

        // Correct string
        String extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Protocol with spaces
        extractedString = parser.extractAddressFromQrString("NG node:0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Protocol with upper case
        extractedString = parser.extractAddressFromQrString("PROTOCOL:0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // String without value
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Lowed case
        extractedString = parser.extractAddressFromQrString("ethereum:0xabcdef0000000000000000000000000000000000");
        assertTrue("0xabcdef0000000000000000000000000000000000".equals(extractedString));

        // Upper case
        extractedString = parser.extractAddressFromQrString("ethereum:0xABC0000000000000000000000000000000000000");
        assertTrue("0xabc0000000000000000000000000000000000000".equals(extractedString));

        // Mixed case
        extractedString = parser.extractAddressFromQrString("ethereum:0xABCdef0000000000000000000000000000000000");
        assertTrue("0xabcdef0000000000000000000000000000000000".equals(extractedString));

        // Address without value
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Address with value
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Address with a different protocol
        extractedString = parser.extractAddressFromQrString("OMG:0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Address longer than expected, parse out an address anyway
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000123");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Two parameters
        extractedString = parser.extractAddressFromQrString("notethereum:0x0000000000000000000000000000000000000abc?value=0&symbol=USD");
        assertTrue("0x0000000000000000000000000000000000000abc".equals(extractedString));

        // Parse out address even with protocol missing
        extractedString = parser.extractAddressFromQrString(":0x0000000000000000000000000000000000000abc?value=0invalid");
        assertTrue("0x0000000000000000000000000000000000000abc".equals(extractedString));

        // Two query parameters
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000123?key=value&key=value");
        assertTrue("0x0000000000000000000000000000000000000123".equals(extractedString));

        // Ampersand on the end
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000123?key=value&key=value&");
        assertTrue("0x0000000000000000000000000000000000000123".equals(extractedString));

        // Parse out non-hex characters
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000000000000000000000000XyZ?value=0invalid");
        assertTrue("0x0000000000000000000000000000000000000xyz".equals(extractedString));

        // Negative: null when address too short
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000abc?value=0invalid");
        assertTrue(extractedString == null);
    }

    @Test
    public void parseQRURLTest() {
        QRURLParser parser = QRURLParser.getInstance();
        QRURLParser.QrUrlResult result;
        Map<String, String> params;

        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ?k1=v1");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000xyz".equals(result.getAddress()));

        params = new HashMap<>();
        params.put("k1", "v1");
        assertTrue(params.equals(result.getParameters()));

        // No parameters
        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000xyz".equals(result.getAddress()));

        params = new HashMap<>();
        assertTrue(params.equals(result.getParameters()));

        // No parameters
        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ?");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000xyz".equals(result.getAddress()));

        params = new HashMap<>();
        assertTrue(params.equals(result.getParameters()));

        // Multiple query params
        result = parser.parse("naga coin:0x0000000000000000000000000000000000000XyZ?k1=v1&k2=v2");
        assertTrue("naga coin".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000xyz".equals(result.getAddress()));

        params = new HashMap<>();
        params.put("k1", "v1");
        params.put("k2", "v2");
        assertTrue(params.equals(result.getParameters()));

        // Too many ':'
        result = parser.parse("something:coin:0x0000000000000000000000000000000000000XyZ?k1=v1&k2=v2");
        assertTrue(result == null);
    }
}