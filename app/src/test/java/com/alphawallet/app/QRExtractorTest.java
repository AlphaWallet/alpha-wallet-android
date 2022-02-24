package com.alphawallet.app;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.util.QRParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;

import static org.mockito.Mockito.when;

import static com.alphawallet.app.entity.EIP681Type.OTHER;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class QRExtractorTest {

    @Before
    public void setUp()
    {
        PowerMockito.mockStatic(TextUtils.class);
    }

    @Test
    public void extractingIsCorrect()
    {
        QRParser parser = QRParser.getInstance(null);

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
        assertTrue("0xABC0000000000000000000000000000000000000".equals(extractedString));

        // Mixed case
        extractedString = parser.extractAddressFromQrString("ethereum:0xABCdef0000000000000000000000000000000000");
        assertTrue("0xABCdef0000000000000000000000000000000000".equals(extractedString));

        // Address without value
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Address with value
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000?value=0");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        extractedString = parser.extractAddressFromQrString("jappodonks.ethereum.eth?value=0");
        assertTrue("jappodonks.ethereum.eth".equals(extractedString));

        extractedString = parser.extractAddressFromQrString("jappodonks.ethereum.eth");
        assertTrue("jappodonks.ethereum.eth".equals(extractedString));

        // Address with a different protocol
        extractedString = parser.extractAddressFromQrString("OMG:0x0000000000000000000000000000000000000000");
        assertTrue("0x0000000000000000000000000000000000000000".equals(extractedString));

        // Address longer than expected, don't accept it
        extractedString = parser.extractAddressFromQrString("0x0000000000000000000000000000000000000000123");
        assertTrue(extractedString == null);

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
        assertTrue("0x0000000000000000000000000000000000000XyZ".equals(extractedString));

        //ethereum:0xB4Eda076896D62419409e6D89f734A336608D18D?token=ENJ&contractAddress=0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c

        // Negative: null when address too short
        extractedString = parser.extractAddressFromQrString("ethereum:0x0000000000000000abc?value=0invalid");
        assertTrue(extractedString == null);

        extractedString = parser.extractAddressFromQrString("ethereum:jappodonks.ethereum.eth?value=0invalid");
        assertTrue(extractedString.equals("jappodonks.ethereum.eth"));
    }

    @Test
    public void parseQRURLTest() {
        QRParser parser = QRParser.getInstance(null);
        QRResult result;
        Map<String, String> params;

        when(TextUtils.isEmpty(anyString())).thenReturn(false);

        CryptoFunctions cryptoFunctions = new CryptoFunctions()
        {
            @Override
            public byte[] Base64Decode(String message)
            {
                return Base64.getUrlDecoder().decode(message);
            }

            @Override
            public byte[] Base64Encode(byte[] data)
            {
                return Base64.getUrlEncoder().encode(data);
            }
        };

        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ?k1=v1");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000XyZ".equals(result.getAddress()));

        assertTrue(result.getFunctionDetail().equals("(k1{v1})"));

        // No parameters
        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000XyZ".equals(result.getAddress()));

        assertTrue(result.getFunction().length() == 0);

        // No parameters
        result = parser.parse("protocol:0x0000000000000000000000000000000000000XyZ?");
        assertTrue("protocol".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000XyZ".equals(result.getAddress()));
        assertTrue(result.getFunction().length() == 0);

        // Multiple query params
        result = parser.parse("naga coin:0x0000000000000000000000000000000000000XyZ?k1=v1&k2=v2");
        assertTrue("naga coin".equals(result.getProtocol()));
        assertTrue("0x0000000000000000000000000000000000000XyZ".equals(result.getAddress()));

        assertTrue(result.getFunctionDetail().equals("(k1{v1},k2{v2})"));

        // Too many ':'
        result = parser.parse("something:coin:0x0000000000000000000000000000000000000XyZ?k1=v1&k2=v2");
        assertTrue(result.type == OTHER);

        //Test EIP681
        result = parser.parse("ethereum:0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359?value=2.014e18");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue("0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359".equals(result.getAddress()));
        assertTrue(new BigInteger("2014000000000000000",10).equals(result.getValue()));
        assertTrue(1 == result.chainId);

        result = parser.parse("ethereum:0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359@100?value=2.014e18");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue("0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359".equals(result.getAddress()));
        assertTrue(100 == result.chainId);

        result = parser.parse("ethereum:0x89205a3a3b2a69de6dbf7f01ed13b2108b2c43e7@100/transfer?address=0x8e23ee67d1332ad560396262c48ffbb01f93d052&uint256=1");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue("0x89205a3a3b2a69de6dbf7f01ed13b2108b2c43e7".equals(result.getAddress()));
        assertTrue(result.getFunction().equals("transfer(address,uint256)"));

        //ENJIN wallet style non standard EIP681
        result = parser.parse("ethereum:0xB4Eda076896D62419409e6D89f734A336608D18D?token=ENJ&contractAddress=0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c");
        //should be open ended token transfer
        assertTrue(result.type == EIP681Type.TRANSFER);
        assertTrue("0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c".equalsIgnoreCase(result.getAddress()));
        assertTrue("0xB4Eda076896D62419409e6D89f734A336608D18D".equalsIgnoreCase(result.functionToAddress));

        //Test function calls and function signature generation
        result = parser.parse("ethereum:0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B@4/pay?uint256=100000&value=1000&gasPrice=700000&gasLimit=27500");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue(result.type == EIP681Type.FUNCTION_CALL);
        assertTrue("0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B".equals(result.getAddress()));
        assertTrue(result.getFunction().equals("pay(uint256)"));
        assertTrue(result.getGasPrice().equals(BigInteger.valueOf(700000)));
        assertTrue(result.getGasLimit().equals(BigInteger.valueOf(27500)));
        assertTrue(new BigInteger("1000",10).equals(result.getValue()));

        result = parser.parse("ethereum:0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B@4/approve?address=0x8e23ee67d1332ad560396262c48ffbb01f93d052&uint256=1000");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue(result.type == EIP681Type.FUNCTION_CALL);
        assertTrue("0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B".equals(result.getAddress()));
        assertTrue(result.getFunction().equals("approve(address,uint256)"));

        //ethereum function with no params
        result = parser.parse("ethereum:0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B@4/activateCompoundFinance");
        assertTrue("ethereum".equals(result.getProtocol()));
        assertTrue(result.type == EIP681Type.FUNCTION_CALL);
        assertTrue("0xaaf3A96b8f5E663Fc47bCc19f14e10A3FD9c414B".equals(result.getAddress()));
        assertTrue(result.getFunction().equals("activateCompoundFinance()"));

        //Test magiclink
        result = parser.parse("https://rinkeby.aw.app/AQAHoSBfnM-scRdqGWp_UEFOHl90fBDj0M9-ItcPWPUGu2LCYyzBDW7mt9VVyoPkHIk1ElL9xCQM90jeMiYJMYA4l4-JtVQ-UGijRcbFEaCZLSvSsCuXGpApc4zCehw=");
        assertTrue(result.type == EIP681Type.MAGIC_LINK);

        result = parser.parse("https://www.alphawallet.com");
        System.out.println(result.getAddress());
        assertTrue(result.type == EIP681Type.URL);

        result = parser.parse("http://www.alphawallet.com");
        System.out.println(result.getAddress());
        assertTrue(result.type == EIP681Type.URL);
    }
}