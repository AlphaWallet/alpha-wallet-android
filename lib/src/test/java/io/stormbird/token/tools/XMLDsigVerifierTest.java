package io.stormbird.token.tools;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;

import io.stormbird.token.entity.XMLDsigVerificationResult;

public class XMLDsigVerifierTest {

    @Test
    public void testRSACertSignedByInvalidAuthority() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
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
    public void verifyRSAxmldsig() throws Exception {
        InputStream DAIToken = new FileInputStream("src/test/ts/EntryToken-valid-RSA.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(DAIToken);
        assert(result.isValid);
        assert(result.subjectPrincipal.equals("CN=aw.app"));
    }

    @Test
    public void testFifaTSMLECDSA() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/fifa.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        //Fails because cert is expired
        assert(!result.isValid);
        assert(result.failureReason.equals("cannot find validation key")); // that's the error for expired certs
    }

    @Test
    public void testDuplicateKeyInfo() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken-duplicate-Values.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        assert(!result.isValid);
        assert(result.failureReason.equals("Invalid element name: KeyInfo, expected KeyInfo or Object"));
    }

    @Test
    public void testWrongOrderCertChain() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/DAI-wrong-chain-order.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        assert(result.isValid);
    }

    @Test
    public void testNotYetValidCertificate() throws Exception {
        InputStream EntryToken = new FileInputStream("src/test/ts/EntryToken-future-cert-self-signed.tsml");
        XMLDsigVerificationResult result = new XMLDSigVerifier().VerifyXMLDSig(EntryToken);
        assert(!result.isValid);
        assert(result.failureReason.contains("NotBefore")); // save travis from misreporting thanks to timezone
    }
}