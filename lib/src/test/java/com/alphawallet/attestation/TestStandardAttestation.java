package com.alphawallet.attestation;

import com.alphawallet.attestation.StandardAttestation.AttestationType;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import org.junit.Assert;

public class TestStandardAttestation {
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

  private Attestation makeUnsignedAtt(PublicKey key) {
    Attestation att = new StandardAttestation("test@test.ts", AttestationType.EMAIL, key);
    att.setIssuer("CN=ALX");
    att.setSerialNumber(1);
    Date now = new Date();
    att.setNotValidBefore(now);
    att.setNotValidAfter(new Date(System.currentTimeMillis() + 3600000)); // Valid for an hour
    att.setSmartcontracts(Arrays.asList(42L, 1337L));
    Assert.assertTrue(att.isValid());
    Assert.assertFalse(att.isValidX509()); // Since the version is wrong, and algorithm is non-standard
    return att;
  }

  @org.junit.Test
  public void testConstructCert() {
    Attestation att = makeUnsignedAtt(subjectKeys.getPublic());
    System.out.println(TestHelper.printDER(att.getPrehash()));
  }


  @org.junit.Test
  public void testSignAttestation() {
    AttestationManager manager = new AttestationManager(issuerKeys);
    Attestation att = makeUnsignedAtt(subjectKeys.getPublic());
    byte[] signature = manager.sign(att);
    byte[] signedAtt = manager.constructSignedAttestation(att, signature);
    Assert.assertTrue(manager.verifySigned(att, signature, issuerKeys.getPublic()));
    System.out.println(TestHelper.printDER(signedAtt));
  }

}
