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
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.math.ec.ECPoint;

public class RedeemCheque implements ASNEncodable, Verifiable {
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
      vec.add(new DERBitString(this.signature));
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
      vec.add(new DERBitString(this.signature));
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
      this.unsignedEncoding = new DERSequence(Arrays.copyOfRange(asn1.toArray(), 0, 3)).getEncoded();
      this.signature = DERBitString.getInstance(asn1.getObjectAt(3)).getBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("The redeem request is not valid");
    }
  }

  public Cheque getCheque() {
    return cheque;
  }

  public SignedAttestation getAtt() {
    return att;
  }

  public ProofOfExponent getPok() {
    return pok;
  }

  public byte[] getSignature() {
    return signature;
  }

  public AsymmetricKeyParameter getUserPublicKey() {
    return userPublicKey;
  }

  /**
   * Verifies that the redeem request will be accepted by the smart contract
   * @return true if the redeem request should be accepted by the smart contract
   */
  public boolean checkValidity() {
    // CHECK: that it is an identity attestation otherwise not all the checks of validity needed gets carried out
    try {
      StandardAttestation std = new StandardAttestation(att.getUnsignedAttestation().getDerEncoding());
      // CHECK: perform the needed checks of an identity attestation
      if (!std.checkValidity()) {
        System.err.println("The attestation is not a valid standard attestation");
        return false;
      }
    } catch (IOException e) {
      System.err.println("The attestation could not be parsed as a standard attestation");
      return false;
    }

    // CHECK: that the cheque is still valid
    if (!getCheque().checkValidity()) {
      System.err.println("Cheque is not valid");
      return false;
    }

    // CHECK: verify signature on RedeemCheque is from the same party that holds the attestation
    SubjectPublicKeyInfo spki = getAtt().getUnsignedAttestation().getSubjectPublicKeyInfo();
    try {
      AsymmetricKeyParameter parsedSubjectKey = PublicKeyFactory.createKey(spki);
      if (!SignatureUtility.verify(this.unsignedEncoding, getSignature(), parsedSubjectKey)) {
        System.err.println("The signature on RedeemCheque is not valid");
        return false;
      }
    } catch (IOException e) {
      System.err.println("The attestation SubjectPublicKey cannot be parsed");
      return false;
    }

    // CHECK: verify the identity of the proof and the attestation matcher
    ASN1Sequence extensions = att.getUnsignedAttestation().getExtensions();
    // Need to decode twice since the standard ASN1 encodes the octet string in an octet string
    ASN1OctetString identityEnc = ASN1OctetString
        .getInstance(ASN1Sequence.getInstance(extensions.getObjectAt(0)).getObjectAt(2));
    if (!Arrays.equals(identityEnc.getOctets(), pok.getBase().getEncoded(false))) {
      System.err.println("Identity used in proof and attestation does not match");
      return false;
    }

    // CHECK: verify that the riddle of the proof and cheque matches
    byte[] decodedRiddle = AttestationCrypto.decodePoint(getCheque().getRiddle()).getEncoded(false);
    if (!Arrays.equals(decodedRiddle, getPok().getRiddle().getEncoded(false))) {
      System.err.println("The riddle of the proof and cheque does not match");
      return false;
    }

    // CHECK: the Ethereum address on the attestation matches receivers signing key
    // TODO
    return true;
  }

  @Override
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

  // TODO override equals and hashcode
}
