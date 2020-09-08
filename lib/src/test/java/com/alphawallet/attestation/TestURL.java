package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.junit.Assert;

public class TestURL {
  private static AsymmetricCipherKeyPair senderKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    senderKeys = TestHelper.constructECKeys(rand);
  }

  @org.junit.Test
  public void testSunshine() throws IOException  {
    BigInteger senderSecret = new BigInteger("112");
    Cheque cheque = new Cheque("test@test.ts", AttestationType.EMAIL, 1000, 3600000, senderKeys, senderSecret);

    byte[] senderPublicKey = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(senderKeys.getPublic()).getPublicKeyData().getEncoded();
    String url = URLUtility.encodeList(Arrays.asList(cheque.getDerEncoding(), senderPublicKey));

    List<byte[]> decoded = URLUtility.decodeList(url);
    Cheque newCheque = new Cheque(decoded.get(0));
    Assert.assertTrue(newCheque.verify());
    Assert.assertTrue(newCheque.checkValidity());
    Assert.assertArrayEquals(cheque.getDerEncoding(), newCheque.getDerEncoding());

    AsymmetricKeyParameter newIssuerPublicKey = SignatureUtility.restoreKey(decoded.get(1));
    Cheque otherConstructorCheque = new Cheque(newCheque.getRiddle(), newCheque.getAmount(),
        newCheque.getNotValidBefore(), newCheque.getNotValidAfter(), newCheque.getSignature(), newIssuerPublicKey);
    Assert.assertArrayEquals(cheque.getDerEncoding(), otherConstructorCheque.getDerEncoding());
  }

  @org.junit.Test
  public void testConsistentEncoding() throws IOException {
    BigInteger senderSecret = new BigInteger("112");
    Cheque cheque = new Cheque("test@test.ts", AttestationType.EMAIL, 1000, 3600000, senderKeys, senderSecret);
    String url = URLUtility.encodeData(cheque.getDerEncoding());
    Cheque newCheque = new Cheque(URLUtility.decodeData(url));
    String newUrl = URLUtility.encodeData(newCheque.getDerEncoding());
    Assert.assertEquals(url, newUrl);
  }
}
