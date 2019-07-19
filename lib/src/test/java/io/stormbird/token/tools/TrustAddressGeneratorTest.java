package io.stormbird.token.tools;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;

public class TrustAddressGeneratorTest {

    @Test
    public void generateTrustAddress() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken.tsml");
        TrustAddressGenerator trustAddressGenerator = new TrustAddressGenerator();
        String trustAddress = trustAddressGenerator.generateTrustAddress(
                EntryToken,
                "0x63cCEF733a093E5Bd773b41C96D3eCE361464942",
                "TRUST"
        );
        assert(trustAddress.equals("0x061dd8ee9440094c57202278207b63860e027747"));
    }

    @Test
    public void generateRevokeAddress() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken.tsml");
        TrustAddressGenerator trustAddressGenerator = new TrustAddressGenerator();
        String revokeAddress = trustAddressGenerator.generateTrustAddress(
                EntryToken,
                "0x63cCEF733a093E5Bd773b41C96D3eCE361464942",
                "REVOKE"
        );
        assert(revokeAddress.equals("0xe5a25da518dba447faaeb8ef20588da303056f2d"));
    }

}