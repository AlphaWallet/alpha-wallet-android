package com.alphawallet.token.web.Service;

import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class SpawnableLinkGenerator {

    //Set here
    private final List<BigInteger> tokens = Arrays.asList(BigInteger.ONE, BigInteger.TEN);
    private final String contractAddress = "0x86a2390e15c287d4cb22768d4d1069ef82b7c27b";
    private final BigInteger price = BigInteger.ZERO;
    private long expiry = (System.currentTimeMillis() + 1000000000) / 1000L;;
    private ParseMagicLink parseMagicLink = new ParseMagicLink(new CryptoFunctions(), null);
    private final BigInteger privateKey = BigInteger.TEN;
    private final int chainId = 3;

    public static void main(String[] args) throws SalesOrderMalformed {
        new SpawnableLinkGenerator().createSpawnableLink();
    }

    private void createSpawnableLink() throws SalesOrderMalformed {
        byte[] message = parseMagicLink.getSpawnableBytes(tokens, contractAddress, BigInteger.ZERO, expiry);
        byte[] signature = signMagicLink(message, privateKey);
        byte[] linkData = ParseMagicLink.generateSpawnableLeadingLinkBytes(tokens, contractAddress, BigInteger.ZERO, expiry);
        String link = parseMagicLink.completeUniversalLink(chainId, linkData, signature);
        System.out.println(link);
    }

    private void createTokenIds() {
        //TODO allow the user to specify the attributes to go into the tokenid then form them together and print
    }

    private byte[] signMagicLink(byte[] signData, BigInteger privateKeyOfOrganiser)
    {
        ECKeyPair ecKeyPair  = ECKeyPair.create(privateKeyOfOrganiser);
        Sign.SignatureData signatureData = Sign.signMessage(signData, ecKeyPair);
        return bytesFromSignature(signatureData);
    }

    //TODO this function should be in the libs module not here or in the app
    private static byte[] bytesFromSignature(Sign.SignatureData signature)
    {
        byte[] sigBytes = new byte[65];
        Arrays.fill(sigBytes, (byte) 0);
        try
        {
            System.arraycopy(signature.getR(), 0, sigBytes, 0, 32);
            System.arraycopy(signature.getS(), 0, sigBytes, 32, 32);
            sigBytes[64] = signature.getV();
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
        }

        return sigBytes;
    }

}

