package com.alphawallet.attestation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64Encoder;

public class DERUtility {
  public static final int CHARS_IN_LINE = 65;

  /**
   * Extact an EC keypair from the DER encoded private key
   * @param input The DER encoded input
   * @return
   */
  public static AsymmetricCipherKeyPair restoreBase64Keys(String input) {
    try {
      ECPrivateKeyParameters priv = (ECPrivateKeyParameters)
          PrivateKeyFactory.createKey(restoreBytes(input));
      ECPoint Q = priv.getParameters().getG().multiply(priv.getD());
      ECKeyParameters pub = new ECPublicKeyParameters(Q, priv.getParameters());
      return new AsymmetricCipherKeyPair(pub, priv);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] encodeSecret(BigInteger secret) {
    try {
      ASN1EncodableVector asn1 = new ASN1EncodableVector();
      asn1.add(new DEROctetString(secret.toByteArray()));
      return new DERSequence(asn1).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static BigInteger decodeSecret(byte[] secretBytes) {
    try {
      ASN1InputStream input = new ASN1InputStream(secretBytes);
      ASN1Sequence asn1 = ASN1Sequence.getInstance(input.readObject());
      ASN1OctetString secret = ASN1OctetString.getInstance(asn1.getObjectAt(0));
      return new BigInteger(secret.getOctets());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Restores bytes from a base64 PEM-style DER encoding
   * @param input The string containing the base64 encoding
   * @return the raw DER bytes that are encoded
   */
  public static byte[] restoreBytes(String input) throws IOException {
    List<String> lines = Arrays.asList(input.split("\\n").clone());
    // skip first and last line
    List<String> arr = lines.subList(1, lines.size()-1);
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < arr.size(); i++) {
      buf.append(arr.get(i));
    }
    Base64Encoder coder = new Base64Encoder();
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    coder.decode(buf.toString(), outstream);
    return outstream.toByteArray();
  }

  public static String printDER(byte[] input, String type) {
    try {
      Base64Encoder coder = new Base64Encoder();
      ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      coder.encode(input, 0, input.length, outstream);
      byte[] encodedCert = outstream.toByteArray();
      StringBuilder builder = new StringBuilder();
      builder.append("-----BEGIN " + type + "-----\n");
      addBytes(builder, encodedCert);
      builder.append("-----END " + type + "-----");
      return builder.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void addBytes(StringBuilder builder, byte[] encoding) {
    int start = 0;
    while (start < encoding.length) {
      int end = encoding.length - (start + CHARS_IN_LINE) > 0 ?
          start + CHARS_IN_LINE : encoding.length;
      builder.append(new String(Arrays.copyOfRange(encoding, start, end)));
      builder.append('\n');
      start += CHARS_IN_LINE;
    }
  }
}
