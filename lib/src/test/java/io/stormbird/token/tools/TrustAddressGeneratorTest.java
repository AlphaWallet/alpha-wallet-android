package io.stormbird.token.tools;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;

public class TrustAddressGeneratorTest {

    @Test
    public void generateTrustAddress() throws Exception {
        InputStream input = new FileInputStream("src/test/ts/EntryToken.tsml");
        String trustAddress = TrustAddressGenerator.getTrustAddress("0x63cCEF733a093E5Bd773b41C96D3eCE361464942", input);
        assert(trustAddress.equals("0x2e02934b4ed1bee0defa7a58061dd8ee9440094c"));
    }

    @Test
    public void generateRevokeAddress() throws Exception {
        InputStream input = new FileInputStream("src/test/ts/EntryToken.tsml");
        String revokeAddress = TrustAddressGenerator.getRevokeAddress("0x63cCEF733a093E5Bd773b41C96D3eCE361464942", input);

        assert(revokeAddress.equals("0x6b4c50938caef365fa3e04bfe5a25da518dba447"));
    }

}
