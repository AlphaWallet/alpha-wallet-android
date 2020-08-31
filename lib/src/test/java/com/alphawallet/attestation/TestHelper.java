package com.alphawallet.attestation;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class TestHelper {
  public static final int CHARS_IN_LINE = 65;

  public static KeyPair constructKeys(SecureRandom rand) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(AttestationCrypto.SIGNATURE_ALG, "BC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec(AttestationCrypto.ECDSA_CURVE);
    keyGen.initialize(ecSpec, rand);
    return keyGen.generateKeyPair();
  }

  public static String printDER(byte[] input) {
    byte[] encodedCert = Base64.getEncoder().encode(input);
    StringBuilder builder = new StringBuilder();
    builder.append("-----BEGIN SIGNABLE-----\n");
    addBytes(builder, encodedCert);
    builder.append("-----END SIGNABLE-----");
    return builder.toString();
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
