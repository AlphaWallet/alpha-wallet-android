package com.alphawallet.attestation;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.Keccak.Digest256;

public class SignatureUtility {
    /**
     * Extract the public key from its DER encoded BITString
     * @param input
     * @return
     */
    public static AsymmetricKeyParameter restoreKey(byte[] input) throws IOException {
        AlgorithmIdentifier identifierEnc = new AlgorithmIdentifier(new ASN1ObjectIdentifier(AttestationCrypto.OID_SIGNATURE_ALG), AttestationCrypto.curve.toASN1Primitive());
        ASN1BitString keyEnc = DERBitString.getInstance(input);
        ASN1Sequence spkiEnc = new DERSequence(new ASN1Encodable[] {identifierEnc, keyEnc});
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(spkiEnc);
        return PublicKeyFactory.createKey(spki);
    }

    public static byte[] sign(byte[] toSign, AsymmetricKeyParameter key) {
        Digest256 digest = new Keccak.Digest256();
        byte[] digestBytes = digest.digest(toSign);
        return signHashed(digestBytes, key);
    }

    static byte[] signHashed(byte[] digest, AsymmetricKeyParameter key) {
        try {
            ECDSASigner signer = new ECDSASigner();
            signer.init(true, key);
            BigInteger[] signature = signer.generateSignature(digest);
            // Normalize number s
            BigInteger half_curve = ((ECKeyParameters) key).getParameters().getCurve().getOrder().shiftRight(1);
            if (signature[1].compareTo(half_curve) > 0) {
                signature[1] = ((ECKeyParameters) key).getParameters().getN().subtract(signature[1]);
            }
            ASN1EncodableVector asn1 = new ASN1EncodableVector();
            asn1.add(new ASN1Integer(signature[0]));
            asn1.add(new ASN1Integer(signature[1]));
            return new DERSequence(asn1).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(byte[] unsigned, byte[] signature, AsymmetricKeyParameter key) {
        try {
            ASN1InputStream input = new ASN1InputStream(signature);
            ASN1Sequence seq = ASN1Sequence.getInstance(input.readObject());
            BigInteger r = ASN1Integer.getInstance(seq.getObjectAt(0)).getValue();
            BigInteger s = ASN1Integer.getInstance(seq.getObjectAt(1)).getValue();
            // Normalize number s
            BigInteger half_curve = ((ECKeyParameters) key).getParameters().getCurve().getOrder().shiftRight(1);
            if (s.compareTo(half_curve) > 0) {
                s = ((ECKeyParameters) key).getParameters().getN().subtract(s);
            }
            ECDSASigner signer = new ECDSASigner();
            signer.init(false, key);
            Digest256 digest = new Keccak.Digest256();
            byte[] digestBytes = digest.digest(unsigned);
            return signer.verifySignature(digestBytes, r, s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
