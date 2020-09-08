package com.alphawallet.attestation;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.UrlBase64Encoder;

public class URLUtility {
  public static String encodeList(List<byte[]> inputs) {
    return encodeData(encodeListHelper(inputs));
  }

  private static byte[] encodeListHelper(List<byte[]> inputs) {
    try {
      ASN1EncodableVector vec = new ASN1EncodableVector();
      for (byte[] current : inputs) {
        vec.add(new DEROctetString(current));
      }
      return new DERSequence(vec).getEncoded();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String encodeData(byte[] input) {
    try {
      UrlBase64Encoder encoder = new UrlBase64Encoder();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.encode(input, 0, input.length, baos);
      baos.close();
      byte[] encodedBytes = baos.toByteArray();
      return new String(encodedBytes, US_ASCII);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param url The part of the URL that contains encoding. I.e. it must be pruned for domainame and such
   */
  public static List<byte[]> decodeList(String url) throws IOException {
    List<byte[]> res = new ArrayList<>();
    byte[] decodedData = decodeData(url);
    ASN1InputStream input = new ASN1InputStream(decodedData);
    ASN1Encodable[] asn1 = ASN1Sequence.getInstance(input.readObject()).toArray();
    for (ASN1Encodable current : asn1) {
      res.add(ASN1OctetString.getInstance(current).getOctets());
    }
    return res;
  }

  public static byte[] decodeData(String url) {
    try {
      UrlBase64Encoder encoder = new UrlBase64Encoder();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.decode(url, baos);
      baos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
