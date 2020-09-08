package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Assert;

public class TestCheque {
  private static AsymmetricCipherKeyPair senderKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    senderKeys = TestHelper.constructECKeys(rand);
  }

  @org.junit.Test
  public void testFullDecoding() throws Exception {
    Cheque cheque = new Cheque("test@test.ts", AttestationType.EMAIL, 1000, 3600000, senderKeys, BigInteger.TEN);
    byte[] encoded = cheque.getDerEncoding();
    Cheque newCheque = new Cheque(encoded);
    Assert.assertTrue(cheque.verify());
    Assert.assertTrue(cheque.checkValidity());
    Assert.assertArrayEquals(encoded, newCheque.getDerEncoding());

    Cheque otherConstructor = new Cheque(newCheque.getRiddle(), newCheque.getAmount(),
        newCheque.getNotValidBefore(), newCheque.getNotValidAfter(), newCheque.getSignature(),
        newCheque.getPublicKey());
    Assert.assertEquals(cheque.getAmount(), otherConstructor.getAmount());
    Assert.assertEquals(cheque.getNotValidBefore(), otherConstructor.getNotValidBefore());
    Assert.assertEquals(cheque.getNotValidAfter(), otherConstructor.getNotValidAfter());
    Assert.assertArrayEquals(cheque.getRiddle(), otherConstructor.getRiddle());
    Assert.assertArrayEquals(cheque.getSignature(), otherConstructor.getSignature());
    // Note that apparently a proper equality has not been implemented for AsymmetricKeyParameter
//    Assert.assertEquals(cheque.getPublicKey(), otherConstructor.getPublicKey());
    Assert.assertArrayEquals(encoded, otherConstructor.getDerEncoding());
  }

}
