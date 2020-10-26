package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.junit.Assert;

public class TestAttestationRequest {
  private static AsymmetricCipherKeyPair subjectKeys;
  private static AttestationCrypto crypto;

  @org.junit.BeforeClass
  public static void setupKeys() throws Exception {
    SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
    rand.setSeed("seed".getBytes());

    crypto = new AttestationCrypto(rand);
    subjectKeys = crypto.constructECKeys();
  }

  @org.junit.Test
  public void testSunshine() {
    String id = "+4588888888";
    AttestationType type = AttestationType.PHONE;
    BigInteger secret = new BigInteger("42");
    ProofOfExponent pok = crypto.constructProof(id, type, secret);
    AttestationRequest request = new AttestationRequest(id, type, pok, subjectKeys);
    Assert.assertTrue(request.getPok().verify());
    Assert.assertTrue(request.verify());
    Assert.assertTrue(request.checkValidity());
  }

  @org.junit.Test
  public void testDecoding() {
    String id = "foo@bar.baz";
    AttestationType type = AttestationType.EMAIL;
    BigInteger secret = new BigInteger("42424242");
    ProofOfExponent pok = crypto.constructProof(id, type, secret);
    AttestationRequest request = new AttestationRequest(id, type, pok, subjectKeys);
    AttestationRequest newRequest = new AttestationRequest(request.getDerEncoding());
    Assert.assertTrue(newRequest.getPok().verify());
    Assert.assertTrue(newRequest.verify());
    Assert.assertTrue(newRequest.checkValidity());
    Assert.assertArrayEquals(request.getPok().getDerEncoding(), newRequest.getPok().getDerEncoding());
    Assert.assertArrayEquals(request.getDerEncoding(), newRequest.getDerEncoding());
    Assert.assertArrayEquals(request.getSignature(), newRequest.getSignature());
    Assert.assertEquals(request.getIdentity(), newRequest.getIdentity());
    Assert.assertEquals(request.getIdentity(), id);
    Assert.assertEquals(request.getType(), newRequest.getType());
    Assert.assertEquals(request.getType(), type);
    Assert.assertEquals( ((ECKeyParameters) request.getPublicKey()).getParameters(),
        ((ECKeyParameters) newRequest.getPublicKey()).getParameters());
    Assert.assertEquals(((ECKeyParameters) request.getPublicKey()).getParameters(),
        ((ECKeyParameters) subjectKeys.getPublic()).getParameters());
  }

  @org.junit.Test
  public void testNormalizingID() {
    AttestationType type = AttestationType.EMAIL;
    BigInteger secret = new BigInteger("154160516004573454304564685743521");
    ProofOfExponent pok = crypto.constructProof("foo@bar.baz", type, secret);
    AttestationRequest request = new AttestationRequest(" foO@BAr.baz     ", type, pok, subjectKeys);
    // The IDs should be equivalent to avoid impersonation
    Assert.assertTrue(request.verify());
    Assert.assertTrue(request.checkValidity());
  }

  @org.junit.Test
  public void testBadID() {
    AttestationType type = AttestationType.EMAIL;
    BigInteger secret = new BigInteger("42424242");
    ProofOfExponent pok = crypto.constructProof("foo@bar.baz", type, secret);
    AttestationRequest request = new AttestationRequest("foo@bar.bazt", type, pok, subjectKeys);
    Assert.assertTrue(request.verify()); // Signature and proof are ok by themselves
    Assert.assertFalse(request.checkValidity()); // However, the proof is not done over the right id
  }

  @org.junit.Test
  public void testBadType() {
    String id = "foo@bar.baz";
    BigInteger secret = new BigInteger("42424242");
    ProofOfExponent pok = crypto.constructProof(id, AttestationType.EMAIL, secret);
    AttestationRequest request = new AttestationRequest(id, AttestationType.PHONE, pok, subjectKeys);
    Assert.assertTrue(request.verify()); // Signature and proof are ok by themselves
    Assert.assertFalse(request.checkValidity()); // However, the proof is not done over the right type
  }
}
