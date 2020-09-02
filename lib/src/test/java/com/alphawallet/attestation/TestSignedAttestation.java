package com.alphawallet.attestation;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import sun.security.x509.X509CertImpl;

public class TestSignedAttestation {
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

  @org.junit.Test
  public void testSignAttestation() {
    Attestation att = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic(), BigInteger.ONE);
    SignedAttestation signed = new SignedAttestation(att, issuerKeys.getPrivate());
    Assert.assertTrue(SignatureUtility.verify(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    Assert.assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
    System.out.println(TestHelper.printDER(signed.getDerEncoding()));
  }

  @org.junit.Test
  public void testDecoding() throws Exception {
    Attestation att = TestHelper.makeMaximalAtt(subjectKeys.getPublic());
    SignedAttestation signed = new SignedAttestation(att, issuerKeys.getPrivate());
    Assert.assertTrue(SignatureUtility.verify(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    Assert.assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
    byte[] signedEncoded = signed.getDerEncoding();
    SignedAttestation newSigned = new SignedAttestation(signedEncoded);
    Assert.assertArrayEquals(signed.getDerEncoding(), newSigned.getDerEncoding());
  }

  @org.junit.Test
  public void testX509Comp() throws Exception {
    Attestation att = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic());
    SignedAttestation signed = new SignedAttestation(att, issuerKeys.getPrivate());
    Assert.assertTrue(SignatureUtility.verify(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    // Test X509 compliance
    X509Certificate cert = new X509CertImpl(signed.getDerEncoding());
    try {
      cert.checkValidity();
    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
      Assert.fail();
    }
    cert.verify(issuerKeys.getPublic(), new BouncyCastleProvider());
  }
}
