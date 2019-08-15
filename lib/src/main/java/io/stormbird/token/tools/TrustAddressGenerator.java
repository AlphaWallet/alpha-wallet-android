package io.stormbird.token.tools;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;

import static java.nio.charset.StandardCharsets.*;
import static javax.xml.bind.DatatypeConverter.parseHexBinary;

    public class TrustAddressGenerator {
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(),
            CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    public static final byte[] masterPubKey = parseHexBinary("04f0985bd9dbb6f461adc994a0c12595716a7f4fb2879bfc5155dffec3770096201c13f8314b46db8d8177887f8d95af1f2dd217291ce6ffe9183681186696bbe5");

    public static String getTrustAddress(String contractAddress, InputStream tokenscript) throws Exception {
        String digest = getXMLDSigSignedInfoDigest(tokenscript);
        return preimageToAddress(String.format("%s%s%s", contractAddress, "TRUST", digest).getBytes(UTF_8));
    }

    public static String getRevokeAddress(String contractAddress, InputStream tokenscript) throws Exception {
        String digest = getXMLDSigSignedInfoDigest(tokenscript);
        return preimageToAddress(String.format("%s%s%s", contractAddress, "REVOKE", digest).getBytes(UTF_8));
    }

    // this won't make sense at all if you didn't read security.md
    // https://github.com/AlphaWallet/TokenScript/blob/master/doc/security.md
    public static String preimageToAddress(byte[] preimage) throws Exception {
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

    private static String getXMLDSigSignedInfoDigest(InputStream tsmlFile) throws MarshalException, ParserConfigurationException, SAXException, XMLSignatureException, IOException {
        XMLDSigVerifier sigVerifier = new XMLDSigVerifier();
        XMLSignature signature = sigVerifier.getValidXMLSignature(tsmlFile);
        InputStream digest = signature.getSignedInfo().getCanonicalizedData();
        return convertHexToBase64String(Numeric.toHexString(getBytesFromInputStream(digest)));
    }

    private static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    private static String convertHexToBase64String(String input) throws IOException {
        byte barr[] = new byte[16];
        int bcnt = 0;
        for (int i = 0; i < 32; i += 2) {
            char c1 = input.charAt(i);
            char c2 = input.charAt(i + 1);
            int i1 = convertCharToInt(c1);
            int i2 = convertCharToInt(c2);
            barr[bcnt] = 0;
            barr[bcnt] |= (byte) ((i1 & 0x0F) << 4);
            barr[bcnt] |= (byte) (i2 & 0x0F);
            bcnt++;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Base64.encode(barr, outputStream);
        return outputStream.toString();
    }

    private static int convertCharToInt(char c) {
        char[] carr = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char clower = Character.toLowerCase(c);
        for (int i = 0; i < carr.length; i++) {
            if (clower == carr[i]) {
                return i;
            }
        }
        return 0;
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

    /***************** Below are for making this class usable with Amazon Lambda *******************/

    // a public zero-argument constructor is required by Amazon Lambda
    // object created in this fashion is un-initialised, only suited for handleRequest which has its own initialisation code
    //public TokenScriptTrustAddress() throws NoSuchProviderException, NoSuchAlgorithmException {
    //}

    public Response DeriveTrustAddress(Request req) throws Exception {
        InputStream targetStream = new ByteArrayInputStream(req.getTokenScript().getBytes());
        String trust = getTrustAddress(req.contractAddress, targetStream);
        targetStream.close();
        targetStream = new ByteArrayInputStream(req.getTokenScript().getBytes());
        String revoke = getRevokeAddress(req.contractAddress, targetStream);
        targetStream.close();
        return new Response(trust, revoke);
    }

    public static class Request {
        String contractAddress;
        String tokenScript;

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public String getTokenScript() {
            return tokenScript;
        }

        public void setTokenScript(String tokenScript) {
            this.tokenScript = tokenScript;
        }

        public Request(String contractAddress, String tokenScript) {
            this.contractAddress = contractAddress;
            this.tokenScript = tokenScript;
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