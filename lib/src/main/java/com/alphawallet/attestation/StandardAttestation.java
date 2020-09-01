package com.alphawallet.attestation;

import java.io.IOException;
import java.security.PublicKey;
import java.security.SecureRandom;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

public class StandardAttestation extends Attestation {
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
  public StandardAttestation(String identity, AttestationType type, PublicKey key)  {
    super();
    this.crypto = new AttestationCrypto(new SecureRandom());
    super.setVersion(18); // Our initial version
    super.setSubject("CN=" + crypto.addressFromKey(key));
    super.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
    super.setSubjectPublicKeyInfo(AttestationCrypto.OID_SIGNATURE_ALG, key.getEncoded());
    PoK = setRiddle(identity, type);
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
    this.PoK = pok;
  }

  /**
   * Picks a riddle and sets it as an Attribute on the Attestation/
   * @return A proof of knowledge of the riddle
   */
  private ProofOfExponent setRiddle(String identity, AttestationType type) {
    ASN1EncodableVector extensions = new ASN1EncodableVector();
    extensions.add(new ASN1ObjectIdentifier(Attestation.OID_OCTETSTRING));
    extensions.add(ASN1Boolean.TRUE);
    ProofOfExponent proof = crypto.constructRiddle(identity, type);
    extensions.add(new DEROctetString(proof.getRiddle().getEncoded(false)));
    // Double Sequence is needed to be compatible with X509V3
    this.setExtensions(new DERSequence(new DERSequence(extensions)));
    return proof;
  }

  public ProofOfExponent getPoK() {
    return PoK;
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
