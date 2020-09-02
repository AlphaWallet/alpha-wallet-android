package com.alphawallet.attestation;

import com.alphawallet.attestation.StandardAttestation.AttestationType;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class Cheque implements ASNEncodable {
  private final byte[] riddle;
  private final long amount;
  private final long validity;
  private final AsymmetricKeyParameter publicKey;
  private final byte[] encoded;

  /**
   *
   * @param identifier The identifier of the receiver
   * @param type The type of identifier given
   * @param amount Amount of units the cheque should be valid for
   * @param validity time from now which the cheque should be valid, in milliseconds
   * @param keys the keys used to sign the cheque
   * @param secret the secret that must be known to cash the cheque
   */
  public Cheque(String identifier, AttestationType type, long amount, long validity, AsymmetricCipherKeyPair keys, BigInteger secret) {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    this.riddle = crypto.makeRiddle(identifier, type, secret);
    this.publicKey = keys.getPublic();
    this.amount = amount;
    this.validity = validity;
    this.encoded = signCheque(makeCheque(this.riddle, amount, validity), keys);
  }

  public Cheque(byte[] riddle, long amount, long validity, AsymmetricCipherKeyPair keys) {
    this.riddle = riddle;
    this.publicKey = keys.getPublic();
    this.amount = amount;
    this.validity = validity;
    this.encoded = signCheque(makeCheque(this.riddle, amount, validity), keys);
  }

  public Cheque(byte[] derEncoded) throws IOException {
    this.encoded = derEncoded;
    ASN1InputStream input = new ASN1InputStream(derEncoded);
    ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
    ASN1Sequence cheque = ASN1Sequence.getInstance(asn1.getObjectAt(0));
    this.amount = (ASN1Integer.getInstance(cheque.getObjectAt(0))).getValue().longValueExact();

    ASN1Sequence validity = ASN1Sequence.getInstance(asn1.getObjectAt(1));
    ASN1GeneralizedTime notValidBefore = ASN1GeneralizedTime.getInstance(validity.getObjectAt(0));
    ASN1GeneralizedTime notValidAfter = ASN1GeneralizedTime.getInstance(validity.getObjectAt(1));
    try {
      this.validity = notValidAfter.getDate().getTime() - notValidBefore.getDate().getTime();
    } catch (ParseException e) {
      throw new IOException("Validity is not encoded properly");
    }

    this.riddle = (ASN1OctetString.getInstance(cheque.getObjectAt(2))).getOctets();

     this.publicKey = SignatureUtility.restoreKey(asn1.getObjectAt(1).toASN1Primitive()
        .getEncoded());

    // Verify signature
    byte[] signature = DERBitString.getInstance(asn1.getObjectAt(2)).getBytes();
    if (!SignatureUtility.verify(cheque.getEncoded(), signature, this.publicKey)) {
      throw new IllegalArgumentException("The signature on the cheque is invalid");
    }
  }

  private ASN1Sequence makeCheque(byte[] riddle, long amount, long validity) {
    ASN1EncodableVector cheque = new ASN1EncodableVector();
    cheque.add(new ASN1Integer(amount));

    long currentTime = System.currentTimeMillis();
    Date current = new Date(currentTime);
    ASN1GeneralizedTime notValidBefore = new ASN1GeneralizedTime(current);
    Date later = new Date(currentTime+validity);
    ASN1GeneralizedTime notValidAfter = new ASN1GeneralizedTime(later);
    ASN1Sequence validityEnc = new DERSequence(new ASN1Encodable[] {notValidBefore, notValidAfter});
    cheque.add(validityEnc);

    cheque.add(new DEROctetString(riddle));

    return new DERSequence(cheque);
  }

  private byte[] signCheque(ASN1Sequence cheque, AsymmetricCipherKeyPair keys) {
    try {
      ASN1EncodableVector signedCheque = new ASN1EncodableVector();
      signedCheque.add(cheque);

      SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keys.getPublic());
      signedCheque.add(spki.getPublicKeyData());

      byte[] signature = SignatureUtility.sign(cheque.getEncoded(), keys.getPrivate());
      signedCheque.add(new DEROctetString(signature));
      return new DERSequence(signedCheque).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getDerEncoding() {
    return encoded;
  }


  public byte[] getRiddle() {
    return riddle;
  }

  public long getAmount() {
    return amount;
  }

  public long getValidity() {
    return validity;
  }

  public AsymmetricKeyParameter getPublicKey() {
    return this.publicKey;
  }

}
