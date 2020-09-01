package com.alphawallet.attestation;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.Assert;

public class TestAttestation {

    private static KeyPair subjectKeys;
    private static SecureRandom rand;

    @org.junit.BeforeClass
    public static void setupKeys() throws Exception {
        rand = SecureRandom.getInstance("SHA1PRNG");
        rand.setSeed("seed".getBytes());
        subjectKeys = TestHelper.constructKeys(rand);
    }

    @org.junit.Test
    public void testGetterSetter() throws Exception {
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
        att.setSubjectPublicKeyInfo(AttestationCrypto.OID_SIGNATURE_ALG, subjectKeys.getPublic().getEncoded());
        SubjectPublicKeyInfo newSpki = new SubjectPublicKeyInfo(new AlgorithmIdentifier(
            new ASN1ObjectIdentifier(AttestationCrypto.OID_SIGNATURE_ALG)), subjectKeys.getPublic().getEncoded());
        Assert.assertArrayEquals(att.getSubjectPublicKeyInfo(), newSpki.getEncoded());
        att.setSmartcontracts(Arrays.asList(42L, 13L));
        Assert.assertEquals(att.getSmartcontracts(), Arrays.asList(42L, 13L));
        att.setExtensions(new DERSequence());
        Assert.assertEquals(att.getExtensions(), new DERSequence());

        Attestation att2 = new Attestation();
        att2.setDataObject(new DERSequence());
        Assert.assertEquals(att2.getDataObject(), new DERSequence());
    }

    @org.junit.Test
    public void testMakeUnsignedX509Attestation() {
        byte[] res = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic()).getPrehash();
        Assert.assertTrue(res != null);
    }

    @org.junit.Test
    public void testInvalid() {
        Attestation res = TestHelper.makeMinimalAtt();
        res.setDataObject(null);
        Assert.assertFalse(res.isValid());
    }

    @org.junit.Test
    public void testInvalidx509() {
        Attestation res = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic());
        res.setSmartcontracts(Arrays.asList(13L));
        Assert.assertFalse(res.isValidX509());
    }

    @org.junit.Test
    public void testFullDecoding() throws Exception {
        byte[] encoding = TestHelper.makeMaximalAtt(subjectKeys.getPublic()).getPrehash();
        Attestation newAtt = new Attestation(encoding);
        Assert.assertArrayEquals(encoding, newAtt.getPrehash());
    }

    @org.junit.Test
    public void testMinimalDecoding() throws Exception {
        byte[] encoding = TestHelper.makeMinimalAtt().getPrehash();
        Attestation newAtt = new Attestation(encoding);
        Assert.assertArrayEquals(encoding, newAtt.getPrehash());
    }
}

