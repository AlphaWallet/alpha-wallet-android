package io.stormbird.token.tools;

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
import org.web3j.crypto.Hash;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.xml.crypto.dsig.XMLSignature;
import sun.misc.BASE64Encoder;
import static java.nio.charset.StandardCharsets.*;


public class TrustAddressGenerator {
    private X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    private ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(),
            CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    public TrustAddressGenerator() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String generateTrustAddress(InputStream fileStream, String contractAddress, String status) throws Exception {
        byte[] tsmlRoot = getTSMLRootBytes(fileStream);
        String data = Numeric.toHexString(tsmlRoot);
        BigInteger digest = generateDigestFromData(contractAddress, data, status);
        return generateAddressFromDigest(digest);
    }

    private String generateAddressFromDigest(BigInteger digest) throws Exception {
        String PUBKEYHEX = "04f0985bd9dbb6f461adc994a0c12595716a7f4fb2879bfc5155dffec3770096201c13f8314b46db8d8177887f8d95af1f2dd217291ce6ffe9183681186696bbe5";
        ECPublicKey pubkey = decodeKey(Numeric.hexStringToByteArray(PUBKEYHEX));
        ECPoint donatePKPoint = extractPublicKey(pubkey);
        ECPoint digestPKPoint = donatePKPoint.multiply(digest);
        return getAddress(digestPKPoint);
    }

    private byte[] getTSMLRootBytes(InputStream tsmlFile) throws Exception {
        XMLDSigVerifier sigVerifier = new XMLDSigVerifier();
        XMLSignature signature = sigVerifier.getValidXMLSignature(tsmlFile);
        InputStream digest = signature.getSignedInfo().getCanonicalizedData();
        return getBytesFromInputStream(digest);
    }

    private byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    private BigInteger generateDigestFromData(String contractAddress, String data, String status) {
        String digest = convertHexToBase64String(data);
        byte[] target = String.format("%s%s%s", contractAddress, status, digest).getBytes(UTF_8);
        byte[] h_digest = Hash.sha3(target);
        return new BigInteger(Numeric.toHexStringNoPrefix(h_digest), 16);
    }

    private String convertHexToBase64String(String input) {
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

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(barr);
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

    private ECPoint extractPublicKey(ECPublicKey ecPublicKey) {
        java.security.spec.ECPoint publicPointW = ecPublicKey.getW();
        BigInteger xCoord = publicPointW.getAffineX();
        BigInteger yCoord = publicPointW.getAffineY();
        return CURVE.getCurve().createPoint(xCoord, yCoord);
    }

    private ECPublicKey decodeKey(byte[] encoded)
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

    private String getAddress(ECPoint pub) {
        byte[] pubKeyHash = computeAddress(pub);
        return Numeric.toHexString(pubKeyHash);
    }

    private byte[] computeAddress(byte[] pubBytes) {
        byte[] addressBytes = Hash.sha3(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
        return Arrays.copyOfRange(addressBytes, 0, 20);
    }

    private byte[] computeAddress(ECPoint pubPoint) {
        return computeAddress(pubPoint.getEncoded(false ));
    }

}

