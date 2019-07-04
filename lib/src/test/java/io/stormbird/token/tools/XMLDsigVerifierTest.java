package io.stormbird.token.tools;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;

import io.stormbird.token.entity.XMLDsigVerificationResult;

public class XMLDsigVerifierTest {

    @Test
    public void verifyRSAxmldsig() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        //Should not pass because shong.wang is signed by an authority which is not recognised (cacert.org)
        assert(!result.isValid);
        assert(result.failureReason.equals("Path does not chain with any of the trust anchors"));
    }

    @Test
    public void verifyECDSAxmldsig() throws Exception {
        InputStream DAIToken = new FileInputStream("src/test/ts/DAI.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(DAIToken);
        assert(result.isValid);
        assert(result.subjectPrincipal.equals("CN=*.aw.app"));
    }

    @Test
    public void testFifaTSML() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/fifa.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        //Should not pass because shong.wang is signed by an authority which is not recognised (cacert.org)
        assert(!result.isValid);
        assert(result.failureReason.equals("cannot find validation key"));
    }
}