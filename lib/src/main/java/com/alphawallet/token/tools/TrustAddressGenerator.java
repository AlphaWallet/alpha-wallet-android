package com.alphawallet.token.tools;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/***** WARNING *****
 *
 * TrustAddress can be generated without the TokenScript being
 * signed. It's digest is produced in the way "as if tokenscript is
 * signed", therefore please do not add logic like extracting
 * <SignedInfo> from the TokenScript assuming it's signed.
 * - Weiwu
 */
public class TrustAddressGenerator {
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(),
            CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    public static final byte[] masterPubKey = Hex.decode("04f0985bd9dbb6f461adc994a0c12595716a7f4fb2879bfc5155dffec3770096201c13f8314b46db8d8177887f8d95af1f2dd217291ce6ffe9183681186696bbe5");

    public static String getTrustAddress(String contractAddress, String digest) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return preimageToAddress((contractAddress + "TRUST" + digest).getBytes());
    }

    public static String getRevokeAddress(String contractAddress, String digest) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return preimageToAddress((contractAddress + "REVOKE" + digest).getBytes());
    }

    // this won't make sense at all if you didn't read security.md
    // https://github.com/AlphaWallet/TokenScript/blob/master/doc/security.md
    public static String preimageToAddress(byte[] preimage) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());

        // get the hash of the preimage text
        Keccak.Digest256 digest = new Keccak.Digest256();
        digest.update(preimage);
        byte[] hash = digest.digest();

        // use the hash to derive a new address
        BigInteger keyDerivationFactor = new BigInteger(Numeric.toHexStringNoPrefix(hash), 16);
        ECPoint donatePKPoint = extractPublicKey(decodeKey(masterPubKey));
        ECPoint digestPKPoint = donatePKPoint.multiply(keyDerivationFactor);
        return getAddress(digestPKPoint);
    }

    private static ECPoint extractPublicKey(ECPublicKey ecPublicKey) {
        java.security.spec.ECPoint publicPointW = ecPublicKey.getW();
        BigInteger xCoord = publicPointW.getAffineX();
        BigInteger yCoord = publicPointW.getAffineY();
        return CURVE.getCurve().createPoint(xCoord, yCoord);
    }

    private static ECPublicKey decodeKey(byte[] encoded)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
        ECCurve curve = params.getCurve();
        java.security.spec.EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, params.getSeed());
        java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
        java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
        java.security.spec.ECPublicKeySpec keySpec = new java.security.spec.ECPublicKeySpec(point, params2);
        return (ECPublicKey) fact.generatePublic(keySpec);
    }

    private static String getAddress(ECPoint pub) {
        byte[] pubKeyHash = computeAddress(pub);
        return Numeric.toHexString(pubKeyHash);
    }

    private static byte[] computeAddress(byte[] pubBytes) {
        Keccak.Digest256 digest = new Keccak.Digest256();
        digest.update(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
        byte[] addressBytes = digest.digest();
        return Arrays.copyOfRange(addressBytes, 0, 20);
    }

    private static byte[] computeAddress(ECPoint pubPoint) {
        return computeAddress(pubPoint.getEncoded(false ));
    }

    /**********************************************************************************
     For use in Command Console
     **********************************************************************************/

    public static void main(String args[]) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        if (args.length == 2) {
            System.out.println("Express of Trust Address derived using the following:");
            System.out.println("");
            System.out.println("\tContract Address: " + args[0]);
            System.out.println("\tXML Digest for Signature: " + args[1]);
            System.out.println("");
            System.out.println("Are:");
            System.out.println("");
            System.out.println("\tTrust Address:\t" + getTrustAddress(args[0], args[1]));
            System.out.println("\tRevoke Address:\t" + getRevokeAddress(args[0], args[1]));
        } else {
            System.out.println("This utility generates express-of-trust address and its revocation address\n for a given pair of token contract and TokenScript");
            System.out.println("");
            System.out.println("Expecting two arguments: contract address and XML digest.");
            System.out.println("");
            System.out.println("\tExample:");
            System.out.println("\tAssuming classpath is set properly,:");
            System.out.println("\te.g. if you built the lib project with `gradle shadowJar` and you've set");
            System.out.println("\tCLASSPATH=build/libs/lib-all.jar");
            System.out.println("\tRun the following:");
            System.out.println("");
            System.out.println("$ java " + TrustAddressGenerator.class.getCanonicalName() +
                    "0x63cCEF733a093E5Bd773b41C96D3eCE361464942 z+I6NxdALVtlc3TuUo2QEeV9rwyAmKB4UtQWkTLQhpE=");
        }
    }

    /**********************************************************************************
     For use in Amazon Lambda
     **********************************************************************************/

    public Response DeriveTrustAddress(Request req) throws Exception {
        String trust = getTrustAddress(req.contract, req.getDigest());
        String revoke = getRevokeAddress(req.contract, req.getDigest());
        return new Response(trust, revoke);
    }

    public static class Request {
        String contract;
        String digest;

        public String getContractAddress() {
            return contract;
        }

        public void setContractAddress(String contractAddress) {
            this.contract = contractAddress;
        }

        public String getDigest() {
            return digest;
        }

        public void setDigest(String digest) {
            this.digest = digest;
        }

        public Request(String contractAddress, String digest) {
            this.contract = contractAddress;
            this.digest = digest;
        }

        public Request() {
        }
    }

    public static class Response {
        String trustAddress;
        String revokeAddress;

        public String getTrustAddress() { return trustAddress; }

        public void setTrustAddress(String trustAddress) { this.trustAddress = trustAddress; }

        public String getRevokeAddress() { return revokeAddress; }

        public void setRevokeAddress(String revokeAddress) { this.revokeAddress = revokeAddress; }

        public Response(String trustAddress, String revokeAddress) {
            this.trustAddress = trustAddress;
            this.revokeAddress = revokeAddress;
        }

        public Response() {
        }
    }
}
