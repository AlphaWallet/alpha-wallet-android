package com.alphawallet.attestation;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.math.ec.ECPoint;

public class RedeemCheque implements ASNEncodable {
  private final Cheque cheque;
  private final SignedAttestation att;
  private final ProofOfExponent pok;
  private final byte[] signature;

  private final AsymmetricKeyParameter userPublicKey;

  private final byte[] unsignedEncoding;
  private final byte[] encoding;

  public RedeemCheque(Cheque cheque, SignedAttestation att, AsymmetricCipherKeyPair userKeys, BigInteger attestationSecret, BigInteger chequeSecret) {
    this.cheque = cheque;
    this.att = att;
    this.userPublicKey = userKeys.getPublic();

    try {
      this.pok = makeProof(att, attestationSecret, chequeSecret);
      ASN1EncodableVector vec = new ASN1EncodableVector();
      vec.add(ASN1Sequence.getInstance(cheque.getDerEncoding()));
      vec.add(ASN1Sequence.getInstance(att.getDerEncoding()));
      vec.add(ASN1Sequence.getInstance(pok.getDerEncoding()));
      this.unsignedEncoding = new DERSequence(vec).getEncoded();
      this.signature = SignatureUtility.sign(this.unsignedEncoding, userKeys.getPrivate());
      vec.add(DERBitString.getInstance(this.signature));
      this.encoding = new DERSequence(vec).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("The redeem request is not valid");
    }
  }

  public RedeemCheque(Cheque cheque, SignedAttestation att, ProofOfExponent pok, byte[] signature, AsymmetricKeyParameter publicAttestationSigningKey, AsymmetricKeyParameter userPublicKey) {
    this.cheque = cheque;
    this.att = att;
    this.userPublicKey = userPublicKey;
    this.pok = pok;
    this.signature = signature;

    try {
      ASN1EncodableVector vec = new ASN1EncodableVector();
      vec.add(ASN1Sequence.getInstance(cheque.getDerEncoding()));
      vec.add(ASN1Sequence.getInstance(att.getDerEncoding()));
      vec.add(ASN1Sequence.getInstance(pok.getDerEncoding()));
      this.unsignedEncoding = new DERSequence(vec).getEncoded();
      vec.add(DERBitString.getInstance(this.signature));
      this.encoding = new DERSequence(vec).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("The redeem request is not valid");
    }
  }

  public RedeemCheque(byte[] derEncoding, AsymmetricKeyParameter publicAttestationSigningKey, AsymmetricKeyParameter userPublicKey) {
    this.encoding = derEncoding;
    this.userPublicKey = userPublicKey;
    try {
      ASN1InputStream input = new ASN1InputStream(derEncoding);
      ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
      this.cheque = new Cheque(asn1.getObjectAt(0).toASN1Primitive().getEncoded());
      this.att = new SignedAttestation(asn1.getObjectAt(1).toASN1Primitive().getEncoded(), publicAttestationSigningKey);
      this.pok = new ProofOfExponent(asn1.getObjectAt(2).toASN1Primitive().getEncoded());
      this.signature = DERBitString.getInstance(asn1.getObjectAt(3)).getEncoded();

      this.unsignedEncoding = new DERSequence(Arrays.copyOfRange(asn1.toArray(), 0, 4)).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("The redeem request is not valid");
    }
  }

  public boolean verify() {
    return cheque.verify() && att.verify() && pok.verify() && SignatureUtility.verify(unsignedEncoding, signature, userPublicKey);
  }

  private ProofOfExponent makeProof(SignedAttestation att, BigInteger attestationSecret, BigInteger chequeSecret) {
    // TODO Bob should actually verify the cheque is valid before trying to cash it to avoid wasting gas
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    ECPoint decodedRiddle = crypto.decodePoint(cheque.getRiddle());
    BigInteger x = attestationSecret.modInverse(crypto.curveOrder).multiply(chequeSecret).mod(crypto.curveOrder);
    // Need to decode twice since the standard ASN1 encodes the octet string in an octet string
    ASN1Sequence extensions = DERSequence.getInstance(att.getUnsignedAttestation().getExtensions().getObjectAt(0));
    // Index in the second DER sequence is 2 since the third object in an extension is the actual value
    ASN1OctetString identifierEnc = ASN1OctetString.getInstance(extensions.getObjectAt(2));
    // It now holds that identifier.multiply(x) = riddle
    ECPoint identifier = crypto.decodePoint(identifierEnc.getOctets());
    return crypto.computeProof(identifier, decodedRiddle, x);
  }

  @Override
  public byte[] getDerEncoding() {
    return encoding;
  }
}
