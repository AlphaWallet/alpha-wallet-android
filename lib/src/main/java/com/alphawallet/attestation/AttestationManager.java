package com.alphawallet.attestation;

import com.alphawallet.attestation.Attestation;
import com.alphawallet.token.entity.Signable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class AttestationManager {
//  private static final AlgorithmIdentifier identifier = new AlgorithmIdentifier(new ASN1ObjectIdentifier(Attestation.OID_SHA256ECDSA));

    private final KeyPair keys;
    private final Signature sig;

    public AttestationManager(String signatureOid, KeyPair keys) {
        this.keys = keys;
        try {
            Security.addProvider(new BouncyCastleProvider());
            sig = Signature.getInstance(signatureOid, "BC");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] sign(Signable signable) {
        try {
            byte[] toSign = signable.getPrehash();
            sig.initSign(keys.getPrivate());
            sig.update(toSign);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] constructSignedAttestation(Attestation unsignedAtt, byte[] signature) {
        try {
            byte[] toSign = unsignedAtt.getPrehash();
            ASN1EncodableVector res = new ASN1EncodableVector();
            res.add(ASN1Primitive.fromByteArray(toSign));
            res.add(new AlgorithmIdentifier(new ASN1ObjectIdentifier(unsignedAtt.getSignature())));
            res.add(new DERBitString(signature));
            return new DERSequence(res).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifySigned(Signable unsignedAtt, byte[] signature, PublicKey verificationKey) {
        try {
            sig.initVerify(verificationKey);
            sig.update(unsignedAtt.getPrehash());
            return sig.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
