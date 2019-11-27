package com.alphawallet.token.tools;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import org.bouncycastle.util.encoders.Base64;

public class TrustAddressGeneratorTest {
    String digest;

    public TrustAddressGeneratorTest() throws IOException, MarshalException, ParserConfigurationException, SAXException, XMLSignatureException {
        InputStream input = new FileInputStream("src/test/ts/EntryToken.tsml");
        XMLDSigVerifier sigVerifier = new XMLDSigVerifier();
        XMLSignature signature = sigVerifier.getValidXMLSignature(input);
        InputStream digest = signature.getSignedInfo().getCanonicalizedData();
        this.digest = convertHexToBase64String(Numeric.toHexString(getBytesFromInputStream(digest)));
    }

    @Test
    public void generateTrustAddress() throws Exception {
        System.out.println("digest:" + digest);
        String trustAddress = TrustAddressGenerator.getTrustAddress("0x63cCEF733a093E5Bd773b41C96D3eCE361464942", digest);
        assert(trustAddress.equals("0x2e02934b4ed1bee0defa7a58061dd8ee9440094c"));
    }

    @Test
    public void generateRevokeAddress() throws Exception {
        String revokeAddress = TrustAddressGenerator.getRevokeAddress("0x63cCEF733a093E5Bd773b41C96D3eCE361464942", digest);
        assert(revokeAddress.equals("0x6b4c50938caef365fa3e04bfe5a25da518dba447"));
    }

    /*
     * the following utility functions are moved from
     * TrustAddressGenerator because it doesn't belong
     * there. TrustAddressGenerator generates addresses from a
     * contract address and a digest. Itself shouldn't do the work of
     * parsing XML and calculating the digest.
     */

    byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    String convertHexToBase64String(String input) throws IOException {
        byte barr[] = new byte[16];
        int bcnt = 0;
        for (int i = 0; i < 32; i += 2) {
            char c1 = input.charAt(i);
            char c2 = input.charAt(i + 1);
            int i1 = convertCharToInt(c1);
            int i2 = convertCharToInt(c2);
            barr[bcnt] = 0;
            barr[bcnt] |= (byte) ((i1 & 0x0F) << 4);
            barr[bcnt] |= (byte) (i2 & 0x0F);
            bcnt++;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Base64.encode(barr, outputStream);
        return outputStream.toString();
    }

    int convertCharToInt(char c) {
        char[] carr = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char clower = Character.toLowerCase(c);
        for (int i = 0; i < carr.length; i++) {
            if (clower == carr[i]) {
                return i;
            }
        }
        return 0;
    }

}
