package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.QRResult;

import org.junit.Test;

import java.math.BigInteger;

public class QRParserTest
{
    @Test
    public void should_return_null_when_url_is_null()
    {
        QRParser parser = QRParser.getInstance(null);
        QRResult result = parser.parse(null);
        assertNull(result);
    }

    @Test
    public void should_parse_magic_link()
    {
        QRParser parser = QRParser.getInstance(null);
        String url = "https://aw.app/";
        QRResult result = parser.parse(url);
        assertThat(result.type, equalTo(EIP681Type.MAGIC_LINK));
        assertThat(result.getAddress(), equalTo(url));
    }

    @Test
    public void should_parse_address()
    {
        QRParser parser = QRParser.getInstance(null);
        String address = "0xD0c424B3016E9451109ED97221304DeC639b3F84";
        QRResult result = parser.parse(address);
        assertThat(result.type, equalTo(EIP681Type.ADDRESS));
        assertThat(result.getAddress(), equalTo(address));
    }

    @Test
    public void should_parse_ethereum_payment_url()
    {
        QRParser parser = QRParser.getInstance(null);
        String url = "ethereum:0xD0c424B3016E9451109ED97221304DeC639b3F84@42?value=1.5e18";
        QRResult result = parser.parse(url);
        assertThat(result.type, equalTo(EIP681Type.PAYMENT));
        assertThat(result.getAddress(), equalTo("0xD0c424B3016E9451109ED97221304DeC639b3F84"));
        assertThat(result.getProtocol(), equalTo("ethereum"));
        assertThat(result.chainId, equalTo(42L));
        assertThat(result.getValue(), equalTo(BigInteger.valueOf(1500000000000000000L)));
    }

    @Test
    public void should_parse_http_url()
    {
        QRParser parser = QRParser.getInstance(null);
        String url = "https://ethereum.org";
        QRResult result = parser.parse(url);
        assertThat(result.type, equalTo(EIP681Type.URL));
        assertThat(result.getAddress(), equalTo(url));
        assertNotNull(result.getProtocol()); // null causes NPE when in switch case
    }

    @Test
    public void should_parse_other_url()
    {
        QRParser parser = QRParser.getInstance(null);
        String url = "safe-wc:13d2db50-8aa6-4444-1d-14af45c55588@1?bridge=https%3A%2F%2Fsafe-walletconnect.gnosis.io%2F&key=ae112";
        QRResult result = parser.parse(url);
        assertThat(result.type, equalTo(EIP681Type.OTHER));
    }

}