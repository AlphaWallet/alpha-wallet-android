package com.alphawallet.attestation;

import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Assert;

public class TestIdentifierAttestation {
  private static AsymmetricCipherKeyPair subjectKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    subjectKeys = TestHelper.constructECKeys(rand);
  }

  @org.junit.Test
  public void testFullDecoding() throws Exception {
    IdentifierAttestation initial = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic(), BigInteger.ONE);
    byte[] encoding = initial.getDerEncoding();
    Attestation newAtt = new IdentifierAttestation(encoding);
    Assert.assertArrayEquals(encoding, newAtt.getPrehash());
  }

  @org.junit.Test
  public void testNotStandard() throws Exception {
    Attestation initial = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic());
    byte[] encoding = initial.getPrehash();
    try {
      new IdentifierAttestation(encoding);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

}
