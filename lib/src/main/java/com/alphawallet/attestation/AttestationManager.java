package com.alphawallet.attestation;

import com.alphawallet.token.entity.Signable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class AttestationManager {

    public static byte[] sign(Signable signable, PrivateKey key) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Signature sig = Signature.getInstance(AttestationCrypto.OID_SIGNATURE_ALG, "BC");
            byte[] toSign = signable.getPrehash();
            sig.initSign(key);
            sig.update(toSign);
            return sig.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifySigned(Signable unsignedAtt, byte[] signature, PublicKey verificationKey) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Signature sig = Signature.getInstance(AttestationCrypto.OID_SIGNATURE_ALG, "BC");
            sig.initVerify(verificationKey);
            sig.update(unsignedAtt.getPrehash());
            return sig.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
