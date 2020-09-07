package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;

public class TestHelper {
  public static final int CHARS_IN_LINE = 65;


  public static KeyPair constructKeys(SecureRandom rand) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(AttestationCrypto.SIGNATURE_ALG, "BC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec(AttestationCrypto.ECDSA_CURVE);
    keyGen.initialize(ecSpec, rand);
    return keyGen.generateKeyPair();
  }

  public static AsymmetricCipherKeyPair constructBCKeys(SecureRandom rand) {
    ECKeyPairGenerator generator = new ECKeyPairGenerator();
    ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(AttestationCrypto.domain, rand);
    generator.init(keygenParams);
    return generator.generateKeyPair();
  }

  public static IdentifierAttestation makeUnsignedStandardAtt(AsymmetricKeyParameter key, BigInteger secret) {
    IdentifierAttestation att = new IdentifierAttestation("test@test.ts", AttestationType.EMAIL, key, secret);
    att.setIssuer("CN=ALX");
    att.setSerialNumber(1);
    Date now = new Date();
    att.setNotValidBefore(now);
    att.setNotValidAfter(new Date(System.currentTimeMillis() + 3600000)); // Valid for an hour
    att.setSmartcontracts(Arrays.asList(42L, 1337L));
    Assert.assertTrue(att.checkValidity());
    Assert.assertFalse(att.isValidX509()); // Since the version is wrong, and algorithm is non-standard
    return att;
  }

  /* the unsigned x509 attestation will have a subject of "CN=0x2042424242424564648" */
  public static Attestation makeUnsignedx509Att(AsymmetricKeyParameter key) throws IOException  {
    Attestation att = new Attestation();
    att.setVersion(2); // =v3 since counting starts from 0
    att.setSerialNumber(42);
    att.setSignature("1.2.840.10045.4.3.2"); // ECDSA with SHA256 which is needed for a proper x509
    att.setIssuer("CN=ALX");
    Date now = new Date();
    att.setNotValidBefore(now);
    att.setNotValidAfter(new Date(System.currentTimeMillis()+3600000)); // Valid for an hour
    att.setSubject("CN=0x2042424242424564648");
    SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
    spki = new SubjectPublicKeyInfo(new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.10045.4.3.2")),  // ECDSA with SHA256 which is needed for a proper x509
        spki.getPublicKeyData());
    att.setSubjectPublicKeyInfo(spki);
    ASN1EncodableVector extensions = new ASN1EncodableVector();
    extensions.add(new ASN1ObjectIdentifier(Attestation.OID_OCTETSTRING));
    extensions.add(ASN1Boolean.TRUE);
    extensions.add(new DEROctetString("hello world".getBytes()));
    // Double Sequence is needed to be compatible with X509V3
    att.setExtensions(new DERSequence(new DERSequence(extensions)));
    Assert.assertTrue(att.isValidX509());
    return att;
  }

  public static Attestation makeMaximalAtt(AsymmetricKeyParameter key) throws IOException {
    Attestation att = new Attestation();
    att.setVersion(18); // Our initial version
    att.setSerialNumber(42);
    att.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
    att.setIssuer("CN=ALX");
    Date now = new Date();
    att.setNotValidBefore(now);
    att.setNotValidAfter(new Date(System.currentTimeMillis()+3600000)); // Valid for an hour
    att.setSubject("CN=0x2042424242424564648");
    SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
    att.setSubjectPublicKeyInfo(spki);
    att.setSmartcontracts(Arrays.asList(42L, 1337L));
    ASN1EncodableVector dataObject = new ASN1EncodableVector();
    dataObject.add(new DEROctetString("hello world".getBytes()));
    dataObject.add(new ASN1Integer(42));
    att.setDataObject(new DERSequence(dataObject));
    Assert.assertTrue(att.checkValidity());
    return att;
  }

  public static Attestation makeMinimalAtt() {
    Attestation att = new Attestation();
    att.setVersion(18); // Our initial version
    att.setSerialNumber(42);
    att.setSignature(AttestationCrypto.OID_SIGNATURE_ALG);
    ASN1EncodableVector dataObject = new ASN1EncodableVector();
    dataObject.add(new DEROctetString("hello world".getBytes()));
    att.setDataObject(new DERSequence(dataObject));
    Assert.assertTrue(att.checkValidity());
    return att;
  }

  public static String printDER(byte[] input, String type) {
    byte[] encodedCert = Base64.getEncoder().encode(input);
    StringBuilder builder = new StringBuilder();
    builder.append("-----BEGIN "+ type +"-----\n");
    addBytes(builder, encodedCert);
    builder.append("-----END "+ type +"-----");
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
