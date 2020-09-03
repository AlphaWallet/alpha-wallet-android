package com.alphawallet.attestation;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public class SignedAttestation implements ASNEncodable, Verifiable {
  private final Attestation att;
  private final byte[] signature;
  private final AsymmetricKeyParameter publicKey;

  public SignedAttestation(Attestation att, AsymmetricCipherKeyPair key) {
    this.att = att;
    this.signature = SignatureUtility.sign(att.getPrehash(), key.getPrivate());
    this.publicKey = key.getPublic();
    if (!verify()) {
      throw new IllegalArgumentException("The signature is not valid");
    }
  }

  public SignedAttestation(byte[] derEncoding, AsymmetricKeyParameter signingPublicKey) throws IOException {
    ASN1InputStream input = new ASN1InputStream(derEncoding);
    ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
    ASN1Sequence attestationEnc = ASN1Sequence.getInstance(asn1.getObjectAt(0));
    this.att = new Attestation(attestationEnc.getEncoded());
    DERBitString signatureEnc = DERBitString.getInstance(asn1.getObjectAt(2));
    this.signature = signatureEnc.getBytes();
    this.publicKey = signingPublicKey;
    if (!verify()) {
      throw new IllegalArgumentException("The signature is not valid");
    }
  }

  public Attestation getUnsignedAttestation() {
    return att;
  }

  public byte[] getSignature() {
    return signature;
  }

  public AsymmetricKeyParameter getPublicKey() { return publicKey; }

  @Override
  public byte[] getDerEncoding() {
    return constructSignedAttestation(this.att, this.signature);
  }

  static byte[] constructSignedAttestation(Attestation unsignedAtt, byte[] signature) {
    try {
      byte[] rawAtt = unsignedAtt.getPrehash();
      ASN1EncodableVector res = new ASN1EncodableVector();
      res.add(ASN1Primitive.fromByteArray(rawAtt));
      res.add(new AlgorithmIdentifier(new ASN1ObjectIdentifier(unsignedAtt.getSignature())));
      res.add(new DERBitString(signature));
      return new DERSequence(res).getEncoded();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean verify() {
    return SignatureUtility.verify(att.getDerEncoding(), signature, publicKey);
  }
}
