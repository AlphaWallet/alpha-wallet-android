package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.io.IOException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERVisibleString;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.math.ec.ECPoint;

public class AttestationRequest implements ASNEncodable, Validateable, Verifiable {
  private final String identity;
  private final AttestationType type;
  private final ProofOfExponent pok;

  private final AsymmetricKeyParameter publicKey;
  private final byte[] signature;

  public AttestationRequest(String identity, AttestationType type, ProofOfExponent pok, AsymmetricCipherKeyPair keys) {
    try {
      this.identity = identity;
      this.type = type;
      this.pok = pok;
      this.publicKey = PublicKeyFactory
          .createKey( SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keys.getPublic()));
      this.signature = SignatureUtility.sign(getUnsignedEncoding(), keys.getPrivate());

      if (!verify()) {
        throw new IllegalArgumentException("The signature is not valid");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not decode public key from attestation");
    }
  }

  public AttestationRequest(byte[] derEncoding) throws IOException {
    ASN1InputStream input = new ASN1InputStream(derEncoding);
    ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
    ASN1Sequence unsigned = ASN1Sequence.getInstance(asn1.getObjectAt(0));
    this.identity = DERVisibleString.getInstance(unsigned.getObjectAt(0)).getString();
    this.type = AttestationType.values()[
        ASN1Integer.getInstance(unsigned.getObjectAt(1)).getValue().intValueExact()];
    this.pok = new ProofOfExponent(ASN1Sequence.getInstance(unsigned.getObjectAt(2)).getEncoded());
    this.publicKey =  PublicKeyFactory
        .createKey(SubjectPublicKeyInfo.getInstance(asn1.getObjectAt(1)));
    DERBitString signatureEnc = DERBitString.getInstance(asn1.getObjectAt(2));
    this.signature = signatureEnc.getBytes();
    if (!verify()) {
      throw new IllegalArgumentException("The signature is not valid");
    }
  }

  public String getIdentity() { return identity; }

  public AttestationType getType() { return type; }

  public ProofOfExponent getPok() { return pok; }

  public byte[] getSignature() {
    return signature;
  }

  public AsymmetricKeyParameter getPublicKey() { return publicKey; }

  private byte[] getUnsignedEncoding() {
    try {
      ASN1EncodableVector res = new ASN1EncodableVector();
      res.add(new DERVisibleString(identity));
      res.add(new ASN1Integer(type.ordinal()));
      res.add(ASN1Primitive.fromByteArray(pok.getDerEncoding()));
      return new DERSequence(res).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getDerEncoding() {
    try {
      byte[] rawData = getUnsignedEncoding();
      ASN1EncodableVector res = new ASN1EncodableVector();
      res.add(ASN1Primitive.fromByteArray(rawData));
      res.add(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey));
      res.add(new DERBitString(signature));
      return new DERSequence(res).getEncoded();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean checkValidity() {
    ECPoint rehashed = AttestationCrypto.hashIdentifier(type.ordinal(), identity);
    if (!rehashed.normalize().equals(pok.getBase().normalize())) {
      return false;
    }

    return true;
  }

  @Override
  public boolean verify() {
    if (!SignatureUtility.verify(getUnsignedEncoding(), signature, publicKey)) {
      return false;
    }
    if (!AttestationCrypto.verifyProof(pok)) {
      return false;
    }

    return true;
  }
}
