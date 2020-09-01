package com.alphawallet.attestation;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import sun.security.x509.X509CertImpl;

public class TestAttestation {

    private static KeyPair subjectKeys;
    private static KeyPair issuerKeys;
    private static SecureRandom rand;

    @org.junit.BeforeClass
    public static void setupKeys() throws Exception {
        rand = SecureRandom.getInstance("SHA1PRNG");
        rand.setSeed("seed".getBytes());

        subjectKeys = TestHelper.constructKeys(rand);
        issuerKeys = TestHelper.constructKeys(rand);
    }

    private Attestation makeUnsignedx509Att() {
        Attestation att = new Attestation();
        att.setVersion(2); // =v3 since counting starts from 0
        att.setSerialNumber(42);
        att.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
        att.setIssuer("CN=ALX");
        Date now = new Date();
        att.setNotValidBefore(now);
        att.setNotValidAfter(new Date(System.currentTimeMillis()+3600000)); // Valid for an hour
        att.setSubject("CN=0x2042424242424564648");
        att.setSubjectPublicKeyInfo(AttestationCrypto.OID_SIGNATURE_ALG, subjectKeys.getPublic().getEncoded());

        ASN1EncodableVector extensions = new ASN1EncodableVector();
        extensions.add(new ASN1ObjectIdentifier(Attestation.OID_OCTETSTRING));
        extensions.add(ASN1Boolean.TRUE);
        extensions.add(new DEROctetString("hello world".getBytes()));
        // Double Sequence is needed to be compatible with X509V3
        att.setExtensions(new DERSequence(new DERSequence(extensions)));
        Assert.assertTrue(att.isValidX509());
        return att;
    }

    private Attestation makeUnsignedAtt() {
        Attestation att = new Attestation();
        att.setVersion(18); // Our initial version
        att.setSerialNumber(42);
        att.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
        att.setSubject("CN=0x2042424242424564648");
        att.setSmartcontracts(Arrays.asList(42L, 1337L));
        ASN1EncodableVector dataObject = new ASN1EncodableVector();
        dataObject.add(new DEROctetString("hello world".getBytes()));
        dataObject.add(new ASN1Integer(42));
        att.setDataObject(new DERSequence(dataObject));
        Assert.assertTrue(att.isValid());
        return att;
    }

    @org.junit.Test
    public void testGetterSetter() {
        Attestation att = new Attestation();
        att.setVersion(19);
        Assert.assertEquals(att.getVersion(), 19);
        att.setSerialNumber(42);
        Assert.assertEquals(att.getSerialNumber(), 42);
        att.setIssuer("CN=ALX");
        Assert.assertEquals(att.getIssuer(), "CN=ALX");
        Date now = new Date();
        att.setNotValidBefore(now);
        Assert.assertEquals(att.getNotValidBefore().toString(), now.toString());
        Date later = new Date(System.currentTimeMillis()+1000);
        att.setNotValidAfter(later);
        Assert.assertEquals(att.getNotValidAfter().toString(), later.toString());
        att.setSubject("CN=me");
        Assert.assertEquals(att.getSubject(), "CN=me");
        att.setExtensions(new DERSequence());
        Assert.assertEquals(att.getExtensions(), new DERSequence());

        Attestation att2 = new Attestation();
        att2.setDataObject(new DERSequence());
        Assert.assertEquals(att2.getDataObject(), new DERSequence());
    }

    @org.junit.Test
    public void testMakeUnsignedX509Attestation() {
        byte[] res = makeUnsignedx509Att().getPrehash();
        Assert.assertTrue(res != null);
    }

    @org.junit.Test
    public void testMakeUnsignedAttestation() {
        byte[] res = makeUnsignedAtt().getPrehash();
        Assert.assertTrue(res != null);
    }

    @org.junit.Test
    public void testSignAttestration() {
        AttestationManager manager = new AttestationManager(issuerKeys);
        Attestation att = makeUnsignedAtt();
        byte[] signature = manager.sign(att);
        Assert.assertTrue(manager.verifySigned(att, signature, issuerKeys.getPublic()));
    }

    @org.junit.Test
    public void testX509Comp() throws Exception {
        AttestationManager manager = new AttestationManager(issuerKeys);
        Attestation att = makeUnsignedx509Att();
        byte[] signature = manager.sign(att);
        byte[] signedAtt = manager.constructSignedAttestation(att, signature);
        X509Certificate cert = new X509CertImpl(signedAtt);
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException|CertificateNotYetValidException e) {
            Assert.fail();
        }
        cert.verify(issuerKeys.getPublic(), new BouncyCastleProvider());
    }

    @org.junit.Test
    public void testDecoding() throws Exception {
        byte[] encoding = makeUnsignedAtt().getPrehash();
        Attestation newAtt = new Attestation(encoding);
        Assert.assertArrayEquals(encoding, newAtt.getPrehash());
    }
}

