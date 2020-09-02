package com.alphawallet.attestation;

import com.alphawallet.attestation.StandardAttestation.AttestationType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECFieldFp;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

public class AttestationCrypto {
  //  private static final AlgorithmIdentifier identifier = new AlgorithmIdentifier(new ASN1ObjectIdentifier(Attestation.OID_SHA256ECDSA));
  public static final String ECDSA_CURVE = "secp256k1";
  public static final String MAC_ALGO = "HmacSHA256";
  public static final String SIGNATURE_ALG = "ECDSA";
  public static final String OID_SIGNATURE_ALG = "1.2.840.10045.4.3.2"; // TODO ECDSA WIT HSHA256 maybe 1.2.840.10045.2.1 will work (just ECC)

  public final BigInteger fieldSize;
  public final BigInteger curveOrder;
  public final ECNamedCurveParameterSpec spec;
  private final SecureRandom rand;

  public AttestationCrypto(SecureRandom rand) {
    this.rand = rand;
    spec = ECNamedCurveTable.getParameterSpec(ECDSA_CURVE);
    ECNamedCurveSpec params = new ECNamedCurveSpec(ECDSA_CURVE, spec.getCurve(), spec.getG(),
        spec.getN());
    fieldSize = ((ECFieldFp) params.getCurve().getField()).getP();
    curveOrder = params.getOrder();
  }

  /**
   * Code shamelessly stolen from https://medium.com/@fixone/ecc-for-ethereum-on-android-7e35dc6624c9
   * @param key
   * @return
   */
  public static String addressFromKey(PublicKey key) {
    // Todo should be verified that is works as intended, are there any reference values?
    byte[] pubKey = key.getEncoded();
    //discard the first byte which only tells what kind of key it is //i.e. encoded/un-encoded
    pubKey = Arrays.copyOfRange(pubKey,1,pubKey.length);
    MessageDigest KECCAK = new Keccak.Digest256();
    KECCAK.reset();
    KECCAK.update(pubKey);
    byte[] hash = KECCAK.digest();
    //finally get only the last 20 bytes
    return "0x" + Hex.toHexString(Arrays.copyOfRange(hash,hash.length-20,hash.length));
  }

  /**
   * Constructs a riddle based on a secret and returns a proof of knowledge of this
   */
  public ProofOfExponent constructProof(String identity, AttestationType type, BigInteger secret) {
    ECPoint hashedIdentity = hashIdentifier(type.ordinal(), identity);
    ECPoint identifier = hashedIdentity.multiply(secret);
    return computeProof(hashedIdentity, identifier, secret);
  }

  public byte[] makeRiddle(String identity, AttestationType type, BigInteger secret) {
    ECPoint hashedIdentity = hashIdentifier(type.ordinal(), identity);
    return hashedIdentity.multiply(secret).getEncoded(false);
  }

  private ProofOfExponent computeProof(ECPoint base, ECPoint riddle, BigInteger exponent) {
    BigInteger r = makeSecret();
    ECPoint t = base.multiply(r);
    // TODO ideally Bob's ethreum address should also be part of the challenge
    BigInteger c = mapToInteger(makeArray(Arrays.asList(base, riddle, t))).mod(curveOrder);
    BigInteger d = r.add(c.multiply(exponent)).mod(curveOrder);
    return new ProofOfExponent(base, riddle, t, d);
  }

  public boolean verifyProof(ProofOfExponent pok)  {
    BigInteger c = mapToInteger(makeArray(Arrays.asList(pok.getBase(), pok.getRiddle(), pok.gettPoint()))).mod(curveOrder);
    ECPoint lhs = pok.getBase().multiply(pok.getChallenge());
    ECPoint rhs = pok.getRiddle().multiply(c).add(pok.gettPoint());
    return lhs.equals(rhs);
  }

  public BigInteger makeSecret() {
    return new BigInteger(256, rand).mod(curveOrder);
  }

  private byte[] makeArray(List<ECPoint> points ) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      for (ECPoint current : points) {
        outputStream.write(current.getEncoded(false));
      }
      byte[] res = outputStream.toByteArray();
      outputStream.close();
      return res;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ECPoint hashIdentifier(int type, String identifier) {
    BigInteger idenNum = mapToInteger(type, identifier.getBytes(StandardCharsets.UTF_8));
    return computePoint(spec.getCurve(), fieldSize, idenNum);
  }

  private BigInteger mapToInteger(byte[] value) {
    try {
      // We use HMAC to avoid issues with extension attacks, although SHA3 or double hashing should be sufficient on its own
      Mac mac = Mac.getInstance(MAC_ALGO);
      SecretKeySpec keySpec = new SecretKeySpec("static_key".getBytes((StandardCharsets.UTF_8)), MAC_ALGO);
      mac.init(keySpec);
      mac.update(value);
      byte[] macData = mac.doFinal();

      BigInteger idenNum = new BigInteger(macData);
      idenNum.abs();
      return idenNum.mod(fieldSize);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private BigInteger mapToInteger(int type, byte[] identity) {
    ByteBuffer buf = ByteBuffer.allocate(4 + identity.length);
    buf.putInt(type);
    buf.put(identity);
    return mapToInteger(buf.array());
  }

  /**
   * Compute a specific point on the curve (generator) based on x
   * @param params
   * @param p The size of the underlying field
   * @param x The x-coordiante for which we will compute y
   * @return A corresponding y coordinate for x
   */
  private ECPoint computePoint(ECCurve params, BigInteger p, BigInteger x) {
    x = x.mod(p);
    BigInteger y, expected, ySquare;
    do {
      x = x.add(BigInteger.ONE).mod(p);
      BigInteger a = params.getA().toBigInteger();
      BigInteger b = params.getB().toBigInteger();
      ySquare = x.modPow(new BigInteger("3"), p).add(a.multiply(x)).add(b).mod(p);
      // Since we use secp256k1 we use the Lagrange trick to compute the squareroot (since p mod 4=3)
      BigInteger magicExp = p.add(BigInteger.ONE).divide(new BigInteger("4"));
      y = ySquare.modPow(magicExp, p);
      // Check that the squareroot actually exists and hence that we have a point on the curve
      expected = y.multiply(y).mod(p);
    } while (!expected.equals(ySquare));
    return params.createPoint(x, y);
  }
}
