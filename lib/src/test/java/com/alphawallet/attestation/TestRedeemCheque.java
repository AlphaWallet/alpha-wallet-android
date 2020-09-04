package com.alphawallet.attestation;

import com.alphawallet.attestation.StandardAttestation.AttestationType;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi.EC;
import org.junit.Assert;

public class TestRedeemCheque {
  private static AsymmetricCipherKeyPair subjectKeys;
  private static AsymmetricCipherKeyPair issuerKeys;
  private static AsymmetricCipherKeyPair senderKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    subjectKeys = TestHelper.constructBCKeys(rand);
    issuerKeys = TestHelper.constructBCKeys(rand);
    senderKeys = TestHelper.constructBCKeys(rand);
  }

  @org.junit.Test
  public void testSunshine() {
    BigInteger subjectSecret = new BigInteger("42");
    BigInteger senderSecret = new BigInteger("112");
    Attestation att = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic(), subjectSecret);
    SignedAttestation signed = new SignedAttestation(att, issuerKeys);
    Assert.assertTrue(signed.verify());
    Cheque cheque = new Cheque("test@test.ts", AttestationType.EMAIL, 1000, 3600000, senderKeys, senderSecret);
    Assert.assertTrue(cheque.verify());
    RedeemCheque redeem = new RedeemCheque(cheque, signed, subjectKeys, subjectSecret, senderSecret);
    Assert.assertTrue(redeem.verify());
    Assert.assertTrue(redeem.checkValidity());
    try {
      PublicKey pk;
      System.out.println("Signed attestation:");
      System.out.println(TestHelper.printDER(signed.getDerEncoding(), "SIGNABLE"));
      pk = new EC().generatePublic(
          SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(issuerKeys.getPublic()));
      System.out.println("Attestation verification key:");
      System.out.println(TestHelper.printDER(pk.getEncoded(),"PUBLIC KEY"));

      System.out.println("Cheque:");
      System.out.println(TestHelper.printDER(cheque.getDerEncoding(), "CHEQUE"));
      System.out.println("Signed cheque verification key:");
      pk = new EC().generatePublic(
          SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(senderKeys.getPublic()));
      System.out.println(TestHelper.printDER(pk.getEncoded(),"PUBLIC KEY"));

      System.out.println("Redeem Cheque:");
      System.out.println(TestHelper.printDER(redeem.getDerEncoding(), "REDEEM"));
      System.out.println("Signed user public key (for redeem verification):");
      pk = new EC().generatePublic(
          SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(subjectKeys.getPublic()));
      System.out.println(TestHelper.printDER(pk.getEncoded(),"PUBLIC KEY"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @org.junit.Test
  public void testDecoding() {
    BigInteger subjectSecret = new BigInteger("42424242");
    BigInteger senderSecret = new BigInteger("112112112");
    Attestation att = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic(), subjectSecret);
    SignedAttestation signed = new SignedAttestation(att, issuerKeys);
    Cheque cheque = new Cheque("test@test.ts", AttestationType.EMAIL, 1000, 3600000, senderKeys, senderSecret);
    RedeemCheque redeem = new RedeemCheque(cheque, signed, subjectKeys, subjectSecret, senderSecret);
    RedeemCheque newRedeem = new RedeemCheque(redeem.getDerEncoding(), issuerKeys.getPublic(),
        subjectKeys.getPublic());
    Assert.assertTrue(newRedeem.getCheque().verify());
    Assert.assertTrue(newRedeem.getAtt().verify());
    Assert.assertTrue(newRedeem.getPok().verify());

    Assert.assertArrayEquals(redeem.getCheque().getDerEncoding(), newRedeem.getCheque().getDerEncoding());
    Assert.assertArrayEquals(redeem.getAtt().getDerEncoding(), newRedeem.getAtt().getDerEncoding());
    Assert.assertArrayEquals(redeem.getPok().getDerEncoding(), newRedeem.getPok().getDerEncoding());
    Assert.assertArrayEquals(redeem.getSignature(), newRedeem.getSignature());
    Assert.assertEquals(redeem.getUserPublicKey(), subjectKeys.getPublic());
    Assert.assertArrayEquals(redeem.getDerEncoding(), redeem.getDerEncoding());

    RedeemCheque newConstructor = new RedeemCheque(redeem.getCheque(), redeem.getAtt(), redeem.getPok(),
        redeem.getSignature(), issuerKeys.getPublic(), subjectKeys.getPublic());

    Assert.assertArrayEquals(redeem.getDerEncoding(), newConstructor.getDerEncoding());
  }
}
