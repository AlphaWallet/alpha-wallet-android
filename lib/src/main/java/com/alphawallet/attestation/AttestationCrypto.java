package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.ECFieldFp;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
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
  public static final String OID_SIGNATURE_ALG = "1.2.840.10045.2.1"; // OID for elliptic curve crypto
  public static final X9ECParameters curve = SECNamedCurves.getByName(AttestationCrypto.ECDSA_CURVE);
  public static final ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
  public static final ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(ECDSA_CURVE);
  public static final ECNamedCurveSpec params = new ECNamedCurveSpec(ECDSA_CURVE, spec.getCurve(), spec.getG(),
          spec.getN());
  public static final BigInteger fieldSize = ((ECFieldFp) params.getCurve().getField()).getP();
  public static final BigInteger curveOrder = params.getOrder();
  private final SecureRandom rand;

  public AttestationCrypto(SecureRandom rand) {
    this.rand = rand;
  }

  /**
   * Code shamelessly stolen from https://medium.com/@fixone/ecc-for-ethereum-on-android-7e35dc6624c9
   * @param key
   * @return
   */
  public static String addressFromKey(AsymmetricKeyParameter key) {
    // Todo should be verified that is works as intended, are there any reference values?
    byte[] pubKey;
    try {
      SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
      pubKey = spki.getPublicKeyData().getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    //discard the first byte which only tells what kind of key it is //i.e. encoded/un-encoded
    pubKey = Arrays.copyOfRange(pubKey,1,pubKey.length);
    MessageDigest KECCAK = new Keccak.Digest256();
    KECCAK.reset();
    KECCAK.update(pubKey);
    byte[] hash = KECCAK.digest();
    //finally get only the last 20 bytes
    return "0x" + Hex.toHexString(Arrays.copyOfRange(hash,hash.length-20,hash.length)).toUpperCase();
  }

  public AsymmetricCipherKeyPair constructECKeys() {
    ECKeyPairGenerator generator = new ECKeyPairGenerator();
    ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(domain, rand);
    generator.init(keygenParams);
    return generator.generateKeyPair();
  }

  /**
   * Constructs a riddle based on a secret and returns a proof of knowledge of this
   */
  public ProofOfExponent constructProof(String identity, AttestationType type, BigInteger secret) {
    ECPoint hashedIdentity = hashIdentifier(type.ordinal(), identity);
    ECPoint identifier = hashedIdentity.multiply(secret);
    return computeProof(hashedIdentity, identifier, secret);
  }

  static byte[] constructPointBytesFromIdentity(String identity, AttestationType type, BigInteger secret) {
    ECPoint hashedIdentity = hashIdentifier(type.ordinal(), identity);
    ECPoint identifierPoint = hashedIdentity.multiply(secret);
    return identifierPoint.getEncoded(false);
  }

  public byte[] makeRiddle(String identity, AttestationType type, BigInteger secret) {
    ECPoint hashedIdentity = hashIdentifier(type.ordinal(), identity);
    return hashedIdentity.multiply(secret).getEncoded(false);
  }

  ProofOfExponent computeProof(ECPoint base, ECPoint riddle, BigInteger exponent) {
    BigInteger r = makeSecret();
    ECPoint t = base.multiply(r);
    // TODO ideally Bob's ethreum address should also be part of the challenge
    BigInteger c = mapToInteger(makeArray(Arrays.asList(base, riddle, t))).mod(curveOrder);
    BigInteger d = r.add(c.multiply(exponent)).mod(curveOrder);
    return new ProofOfExponent(base, riddle, t, d);
  }

  public static boolean verifyProof(ProofOfExponent pok)  {
    BigInteger c = mapToInteger(makeArray(Arrays.asList(pok.getBase(), pok.getRiddle(), pok.getPoint()))).mod(curveOrder);
    ECPoint lhs = pok.getBase().multiply(pok.getChallenge());
    ECPoint rhs = pok.getRiddle().multiply(c).add(pok.getPoint());
    return lhs.equals(rhs);
  }

  public BigInteger makeSecret() {
    return new BigInteger(256, rand).mod(curveOrder);
  }

  private static byte[] makeArray(List<ECPoint> points ) {
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

   protected static ECPoint hashIdentifier(int type, String identifier) {
    // TODO check that identifier is legal in other ways
    BigInteger idenNum = mapToInteger(type, identifier.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    return computePoint(spec.getCurve(), fieldSize, idenNum);
  }

  private static BigInteger mapToInteger(byte[] value) {
    try {
      Keccak.DigestKeccak kecc = new Keccak.Digest256();
      kecc.update(value, 0, value.length);
      BigInteger idenNum = new BigInteger(kecc.digest());
      return idenNum.mod(fieldSize);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static BigInteger mapToInteger(int type, byte[] identity) {
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
  private static ECPoint computePoint(ECCurve params, BigInteger p, BigInteger x) {
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

  public static ECPoint decodePoint(byte[] point) {
    return spec.getCurve().decodePoint(point);
  }
}
