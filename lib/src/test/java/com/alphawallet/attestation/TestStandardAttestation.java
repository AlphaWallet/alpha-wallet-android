package com.alphawallet.attestation;

import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.SecureRandom;
import org.junit.Assert;

public class TestStandardAttestation {
  private static KeyPair subjectKeys;
  private static SecureRandom rand;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    subjectKeys = TestHelper.constructKeys(rand);
  }

  @org.junit.Test
  public void testFullDecoding() throws Exception {
    StandardAttestation initial = TestHelper.makeUnsignedStandardAtt(subjectKeys.getPublic());
    ProofOfExponent pok = initial.getPoK();
    byte[] encoding = initial.getPrehash();
    Attestation newAtt = new StandardAttestation(encoding, pok);
    Assert.assertArrayEquals(encoding, newAtt.getPrehash());
  }

  @org.junit.Test
  public void testNotStandard() throws Exception {
    Attestation initial = TestHelper.makeUnsignedx509Att(subjectKeys.getPublic());
    byte[] encoding = initial.getPrehash();
    ProofOfExponent pok = new ProofOfExponent(null, null, null, null);
    try {
      new StandardAttestation(encoding, pok);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

}
