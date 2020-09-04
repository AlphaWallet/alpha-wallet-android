package com.alphawallet.attestation;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class StandardAttestation extends Attestation implements Verifiable {
  enum AttestationType {
    PHONE,
    EMAIL
  }

  private final ProofOfExponent PoK;
  private final AttestationCrypto crypto;

  /**
   * Sets up the attestation.
   * You still need to set the optional fields, that is
   * issuer, notValidBefore, notValidAfter, smartcontracts
   */
  public StandardAttestation(String identity, AttestationType type, AsymmetricKeyParameter key, BigInteger secret)  {
    super();
    this.crypto = new AttestationCrypto(new SecureRandom());
    super.setVersion(18); // Our initial version
    super.setSubject("CN=" + crypto.addressFromKey(key));
    super.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
    try {
      SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
      super.setSubjectPublicKeyInfo(spki);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // TODO I am not sure this actually belongs here
    PoK = setRiddle(identity, type, secret);
  }

  public StandardAttestation(byte[] derEncoding, ProofOfExponent pok) throws IOException, IllegalArgumentException {
    super(derEncoding);
    this.crypto = new AttestationCrypto(new SecureRandom());
    if (getVersion() != 18) {
      throw new IllegalArgumentException(
          "The version number is " + getVersion() + ", it must be 18");
    }
    if (getSubject() == null || getSubject().length() != 45 || !getSubject()
        .startsWith("CN=0x")) { // The address is 2*20+5 chars long because it starts with CN=0x
        throw new IllegalArgumentException("The subject is supposed to only be an Ethereum address as the Common Name");
    }
    if (!getSignature().equals(AttestationCrypto.OID_SIGNATURE_ALG)) {
      throw new IllegalArgumentException("The signature algorithm is supposed to be " + AttestationCrypto.OID_SIGNATURE_ALG);
    }
    if (!crypto.verifyProof(pok)) {
      throw new IllegalArgumentException("The Proof Of Knowledge is not correct!");
    }
    // Verify consistency between the PoK and the riddle stored
    ASN1Sequence seq = DERSequence.getInstance(getExtensions().getObjectAt(0));
    ASN1OctetString string = DEROctetString.getInstance(seq.getObjectAt(2));
    if (!Arrays.equals(pok.getRiddle().getEncoded(false), string.getOctets())) {
      throw new IllegalArgumentException("The Proof Of Knowledge does not match the attestation");
    }
    this.PoK = pok;
  }

  /**
   * Picks a riddle and sets it as an Attribute on the Attestation/
   * @return A proof of knowledge of the riddle
   */
  private ProofOfExponent setRiddle(String identity, AttestationType type, BigInteger secret) {
    ASN1EncodableVector extensions = new ASN1EncodableVector();
    extensions.add(new ASN1ObjectIdentifier(Attestation.OID_OCTETSTRING));
    extensions.add(ASN1Boolean.TRUE);
    ProofOfExponent proof = crypto.constructProof(identity, type, secret);
    extensions.add(new DEROctetString(proof.getRiddle().getEncoded(false)));
    // Double Sequence is needed to be compatible with X509V3
    this.setExtensions(new DERSequence(new DERSequence(extensions)));
    return proof;
  }

  public ProofOfExponent getPoK() {
    return PoK;
  }

  @Override
  public boolean verify() {
    return getPoK().verify();
  }

  @Override
  public byte[] getDerEncoding() {
    return this.getPrehash();
  }

  @Override
  public byte[] getPrehash() {
    // Verify the riddle has been constructed correctly
    if (!crypto.verifyProof(PoK)) {
      throw new RuntimeException("Proof of Knowledge of identifier did not succeed.");
    }
    return super.getPrehash();
  }

  @Override
  public void setVersion(int version) {
    throw new RuntimeException("Not allowed to be manually set in concrete Attestation");
  }

  @Override
  public void setSignature(String oid) {
    throw new RuntimeException("Not allowed to be manually set in concrete Attestation");
  }

  @Override
  public void setSubject(String subject) {
    throw new RuntimeException("Not allowed to be manually set in concrete Attestation");
  }
}
