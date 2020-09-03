package com.alphawallet.attestation;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi.EC;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import sun.security.x509.X509CertImpl;

public class TestSignedAttestation {
  private static AsymmetricCipherKeyPair subjectKeys;
  private static AsymmetricCipherKeyPair issuerKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    subjectKeys = TestHelper.constructBCKeys(rand);
    issuerKeys = TestHelper.constructBCKeys(rand);
  }

  @org.junit.Test
  public void testSignAttestation() {
    Attestation att = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic(), BigInteger.ONE);
    SignedAttestation signed = new SignedAttestation(att, issuerKeys);
    Assert.assertTrue(SignatureUtility.verify(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    Assert.assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
    System.out.println(TestHelper.printDER(signed.getDerEncoding()));
  }

  @org.junit.Test
  public void testDecoding() throws Exception {
    Attestation att = TestHelper.makeMaximalAtt(subjectKeys.getPublic());
    SignedAttestation signed = new SignedAttestation(att, issuerKeys);
    Assert.assertTrue(SignatureUtility.verify(att.getPrehash(), signed.getSignature(), issuerKeys.getPublic()));
    Assert.assertArrayEquals(att.getPrehash(), signed.getUnsignedAttestation().getPrehash());
    byte[] signedEncoded = signed.getDerEncoding();
    SignedAttestation newSigned = new SignedAttestation(signedEncoded);
    Assert.assertArrayEquals(signed.getDerEncoding(), newSigned.getDerEncoding());
  }

  @org.junit.Test
  public void testX509Comp() throws Exception {
    Attestation att = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic());
    byte[] toSign = att.getPrehash();
    byte[] digestBytes = new byte[32];
    Digest digest = new SHA256Digest();
    digest.update(toSign, 0, toSign.length);
    digest.doFinal(digestBytes, 0);
    byte[] signature = SignatureUtility.signHashed(digestBytes, issuerKeys.getPrivate());
    byte[] signed = SignedAttestation.constructSignedAttestation(att, signature);
    // Test X509 compliance
    X509Certificate cert = new X509CertImpl(signed);
    try {
      cert.checkValidity();
    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
      Assert.fail();
    }
    PublicKey pk = new EC().generatePublic(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(issuerKeys.getPublic()));
    cert.verify(pk, new BouncyCastleProvider());
  }
}
