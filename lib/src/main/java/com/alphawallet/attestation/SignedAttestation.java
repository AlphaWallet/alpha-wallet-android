package com.alphawallet.attestation;

import java.io.IOException;
import java.security.PrivateKey;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class SignedAttestation implements ASNEncodable {
  private final Attestation att;
  private final byte[] signature;

  public SignedAttestation(Attestation att, PrivateKey key) {
    this.att = att;
    this.signature = SignatureUtility.sign(att.getPrehash(), key);
  }

  public SignedAttestation(byte[] derEncoding) throws IOException {
    ASN1InputStream input = new ASN1InputStream(derEncoding);
    ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
    ASN1Sequence attestationEnc = ASN1Sequence.getInstance(asn1.getObjectAt(0));
    this.att = new Attestation(attestationEnc.getEncoded());
    DERBitString signatureEnc = DERBitString.getInstance(asn1.getObjectAt(2));
    this.signature = signatureEnc.getBytes();
  }

  public Attestation getUnsignedAttestation() {
    return att;
  }

  public byte[] getSignature() {
    return signature;
  }

  @Override
  public byte[] getDerEncoding() {
    return constructSignedAttestation(this.att, this.signature);
  }

  private byte[] constructSignedAttestation(Attestation unsignedAtt, byte[] signature) {
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
}
