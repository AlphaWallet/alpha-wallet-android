package com.alphawallet.attestation;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class StandardAttestation extends Attestation implements Validateable {
  enum AttestationType {
    PHONE,
    EMAIL
  }

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
    setRiddle(identity, type, secret);
  }

  public StandardAttestation(byte[] derEncoding) throws IOException, IllegalArgumentException {
    super(derEncoding);
    this.crypto = new AttestationCrypto(new SecureRandom());
    if (!checkValidity()) {
      throw new IllegalArgumentException("The content is not valid for an identity attestation");
    }
  }

  /**
   * Verifies that the the attestation is in fact a valid identity attestation, in relation to field values.
   * @return true if the field values reflect that this is a standard attestation
   */
  @Override
  public boolean checkValidity() {
    if (!super.checkValidity()) {
      return false;
    }
    if (getVersion() != 18) {
      System.err.println("The version number is " + getVersion() + ", it must be 18");
      return false;
    }
    if (getSubject() == null || getSubject().length() != 45 || !getSubject()
        .startsWith("CN=0x")) { // The address is 2*20+5 chars long because it starts with CN=0x
      System.err.println("The subject is supposed to only be an Ethereum address as the Common Name");
    }
    if (!getSignature().equals(AttestationCrypto.OID_SIGNATURE_ALG)) {
      System.err.println("The signature algorithm is supposed to be " + AttestationCrypto.OID_SIGNATURE_ALG);
    }
    // Verify that the subject public key matches the subject common name
    try {
      AsymmetricKeyParameter parsedSubjectKey = PublicKeyFactory
          .createKey(getSubjectPublicKeyInfo());
      String parsedSubject = "CN=" + crypto.addressFromKey(parsedSubjectKey);
      if (!parsedSubject.equals(getSubject())) {
        System.err.println("The subject public key does not match the Ethereum address attested to");
      }
    } catch (IOException e) {
      System.err.println("Could not parse subject public key");
    }
    return true;
  }

  /**
   * Picks a riddle and sets it as an Attribute on the Attestation/
   * @return A proof of knowledge of the riddle
   */
  private void setRiddle(String identity, AttestationType type, BigInteger secret) {
    ASN1EncodableVector extensions = new ASN1EncodableVector();
    extensions.add(new ASN1ObjectIdentifier(Attestation.OID_OCTETSTRING));
    extensions.add(ASN1Boolean.TRUE);
    extensions.add(new DEROctetString(AttestationCrypto.constructPointBytesFromIdentity(identity, type, secret)));
    // Double Sequence is needed to be compatible with X509V3
    this.setExtensions(new DERSequence(new DERSequence(extensions)));
  }

  @Override
  public byte[] getDerEncoding() {
    return this.getPrehash();
  }

  @Override
  public byte[] getPrehash() {
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
