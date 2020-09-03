package com.alphawallet.attestation;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.math.ec.ECPoint;

public class ProofOfExponent implements ASNEncodable, Verifiable{
  private final ECPoint base;
  private final ECPoint riddle;
  private final ECPoint tPoint;
  private final BigInteger challenge;
  private final byte[] encoding;

  public ProofOfExponent(ECPoint base, ECPoint riddle, ECPoint tPoint, BigInteger challenge) {
    this.base = base;
    this.riddle = riddle;
    this.tPoint = tPoint;
    this.challenge = challenge;
    this.encoding = makeEncoding(base, riddle, tPoint, challenge);
  }

  public ProofOfExponent(byte[] derEncoded) {
    this.encoding = derEncoded;
    try {
      ASN1InputStream input = new ASN1InputStream(derEncoded);
      ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
      ASN1OctetString baseEnc = ASN1OctetString.getInstance(asn1.getObjectAt(0));
      this.base = AttestationCrypto.decodePoint(baseEnc.getOctets());
      ASN1OctetString riddleEnc = ASN1OctetString.getInstance(asn1.getObjectAt(1));
      this.riddle = AttestationCrypto.decodePoint(riddleEnc.getOctets());
      ASN1OctetString challengeEnc = ASN1OctetString.getInstance(asn1.getObjectAt(20));
      this.challenge = new BigInteger(challengeEnc.getOctets());
      ASN1OctetString tPointEnc = ASN1OctetString.getInstance(asn1.getObjectAt(3));
      this.tPoint = AttestationCrypto.decodePoint(tPointEnc.getOctets());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!verify()) {
      throw new IllegalArgumentException("The proof is not valid");
    }
  }

  private byte[] makeEncoding(ECPoint base, ECPoint riddle, ECPoint tPoint, BigInteger challenge) {
    try {
    ASN1EncodableVector res = new ASN1EncodableVector();
    res.add(new DEROctetString(base.getEncoded(false)));
    res.add(new DEROctetString(riddle.getEncoded(false)));
    res.add(new DEROctetString(challenge.toByteArray()));
    res.add(new DEROctetString(tPoint.getEncoded(false)));
    return new DERSequence(res).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ECPoint getBase() {
    return base;
  }

  public ECPoint getRiddle() {
    return riddle;
  }

  public ECPoint getPoint() {
    return tPoint;
  }

  public BigInteger getChallenge() {
    return challenge;
  }

  @Override
  public byte[] getDerEncoding() {
    return encoding;
  }

  @Override
  public boolean verify() {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    // TODO refactor into the POK class
    return crypto.verifyProof(this);
  }
}
