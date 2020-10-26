package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.junit.Assert;

public class TestPoK {

  @org.junit.Test
  public void TestSunshine() {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, BigInteger.TEN);
    Assert.assertTrue(pok.verify());
    SmartContract sc = new SmartContract();
    sc.testEncoding(pok);
    ProofOfExponent newPok = new ProofOfExponent(pok.getDerEncoding());
    Assert.assertTrue(newPok.verify());
    Assert.assertEquals(pok.getBase(), newPok.getBase());
    Assert.assertEquals(pok.getRiddle(), newPok.getRiddle());
    Assert.assertEquals(pok.getPoint(), newPok.getPoint());
    Assert.assertEquals(pok.getChallenge(), newPok.getChallenge());
    Assert.assertArrayEquals(pok.getDerEncoding(), newPok.getDerEncoding());

    ProofOfExponent newConstructor = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge());
    Assert.assertArrayEquals(pok.getDerEncoding(), newConstructor.getDerEncoding());
  }

  @org.junit.Test
  public void TestNegative() {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, BigInteger.TEN);
    Assert.assertTrue(pok.verify());
    ProofOfExponent newPok;
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge().add(BigInteger.ONE));
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint().multiply(new BigInteger("2")), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase().multiply(new BigInteger("2")), pok.getRiddle(), pok.getPoint(), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle().multiply(new BigInteger("2")), pok.getPoint(), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
  }

  @org.junit.Test
  public void TestContract()
  {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());

    SecureRandom rand = new SecureRandom();
    SmartContract sc = new SmartContract();

    for (int i = 0; i < 30; i++)
    {
      byte[] bytes = new byte[32];
      rand.nextBytes(bytes);
      BigInteger rVal = new BigInteger(bytes);
      ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, rVal);
      Assert.assertTrue(pok.verify());
      Assert.assertTrue(sc.testEncoding(pok));
    }

    //now check fail
    for (int i = 0; i < 5; i++)
    {
      byte[] bytes = new byte[32];
      rand.nextBytes(bytes);
      BigInteger rVal = new BigInteger(bytes);
      ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, rVal);
      Assert.assertTrue(pok.verify());
      ProofOfExponent newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge().add(BigInteger.ONE));
      Assert.assertFalse(sc.testEncoding(newPok));
    }
  }

}
